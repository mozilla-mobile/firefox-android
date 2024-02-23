/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.nimbus.messaging

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import kotlinx.coroutines.runBlocking
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import org.mozilla.experiments.nimbus.NimbusMessagingHelperInterface
import org.mozilla.experiments.nimbus.NimbusMessagingInterface
import org.mozilla.experiments.nimbus.internal.FeatureHolder
import org.mozilla.experiments.nimbus.internal.NimbusException
import mozilla.components.service.nimbus.GleanMetrics.Messaging as GleanMessaging

/**
 * This ID must match the name given in the `messaging.fml.yaml` file, which
 * itself generates the classname for [mozilla.components.service.nimbus.messaging.FxNimbusMessaging].
 *
 * If that ever changes, it should also change here.
 *
 * This constant is the id for the messaging feature (the Nimbus feature). We declare it here
 * so as to afford the best chance of it being changed if a rename operation is needed.
 *
 * It is used in the Studies view, to filter out any experiments which only use a messaging surface.
 */
const val MESSAGING_FEATURE_ID = "messaging"

/**
 * Provides messages from [messagingFeature] and combine with the metadata store on [metadataStorage].
 */
class NimbusMessagingStorage(
    private val context: Context,
    private val metadataStorage: MessageMetadataStorage,
    private val onMalformedMessage: (String) -> Unit = {
        GleanMessaging.malformed.record(GleanMessaging.MalformedExtra(it))
    },
    private val nimbus: NimbusMessagingInterface,
    private val messagingFeature: FeatureHolder<Messaging>,
    private val attributeProvider: JexlAttributeProvider? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    /**
     * Contains all malformed messages where they key can be the value or a trigger of the message
     * and the value is the message id.
     */
    @VisibleForTesting
    val malFormedMap = mutableMapOf<String, String>()
    private val logger = Logger("MessagingStorage")
    private val customAttributes: JSONObject
        get() = attributeProvider?.getCustomAttributes(context) ?: JSONObject()

    /**
     * Returns a Nimbus message helper, for evaluating JEXL.
     *
     * The JEXL context is time-sensitive, so this should be created new for each set of evaluations.
     *
     * Since it has a native peer, it should be [destroy]ed after finishing the set of evaluations.
     */
    fun createMessagingHelper(): NimbusMessagingHelperInterface =
        nimbus.createMessageHelper(customAttributes)

    /**
     * Returns the [Message] for the given [key] or returns null if none found.
     */
    suspend fun getMessage(key: String): Message? =
        createMessage(messagingFeature.value(), key)

    private suspend fun createMessage(featureValue: Messaging, key: String): Message? {
        val message = createMessageOrNull(featureValue, key)
        if (message == null) {
            reportMalformedMessage(key)
        }
        return message
    }

    @Suppress("ReturnCount")
    private suspend fun createMessageOrNull(featureValue: Messaging, key: String): Message? {
        val message = featureValue.messages[key] ?: return null

        val action = if (!message.isControl) {
            if (message.text.isBlank()) {
                return null
            }
            sanitizeAction(message.action, featureValue.actions)
                ?: return null
        } else {
            "CONTROL_ACTION"
        }

        val trigger = sanitizeTriggers(message.trigger, featureValue.triggers) ?: return null
        val style = sanitizeStyle(message.style, featureValue.styles) ?: return null
        val storageMetadata = metadataStorage.getMetadata()

        return Message(
            id = key,
            data = message,
            action = action,
            style = style,
            metadata = storageMetadata[key] ?: addMetadata(key),
            triggers = trigger,
        )
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun reportMalformedMessage(key: String) {
        messagingFeature.recordMalformedConfiguration(key)
        onMalformedMessage(key)
    }

    /**
     * Returns a list of currently available messages descending sorted by their [StyleData.priority].
     *
     * "Currently available" means all messages contained in the Nimbus SDK, validated and denormalized.
     *
     * The messages have the JEXL triggers and actions that came from the Nimbus SDK, but these themselves
     * are not validated at this time.
     *
     * The messages also have state attached, which manage how many times the messages has been shown,
     * and if the user has interacted with it or not.
     *
     * The list of messages may also contain control messages which should not be shown to the user.
     *
     * All additional filtering of these messages will happen in [getNextMessage].
     */
    suspend fun getMessages(): List<Message> {
        val featureValue = messagingFeature.value()
        val nimbusMessages = featureValue.messages
        return nimbusMessages.keys
            .mapNotNull { key ->
                createMessage(featureValue, key)
            }.sortedByDescending {
                it.style.priority
            }
    }

    /**
     * Returns the next message for this surface.
     *
     * Message selection takes into account,
     * - the message's surface,
     * - how many times the message has been displayed already
     * - whether or not the user has interacted with the message already.
     * - the message eligibility, via JEXL triggers.
     *
     * If more than one message for this surface is eligible to be shown, then the
     * first one to be encountered in [messages] list is returned.
     */
    fun getNextMessage(surface: MessageSurfaceId, messages: List<Message>): Message? {
        val availableMessages = messages
            .filter {
                it.surface == surface
            }
            .filter {
                !it.isExpired &&
                    !it.metadata.dismissed &&
                    !it.metadata.pressed
            }
        return createMessagingHelper().use {
            getNextMessage(
                surface,
                availableMessages,
                setOf(),
                it,
            )
        }
    }

    @Suppress("ReturnCount")
    private fun getNextMessage(
        surface: MessageSurfaceId,
        availableMessages: List<Message>,
        excluded: Set<String>,
        helper: NimbusMessagingHelperInterface,
    ): Message? {
        val message = availableMessages
            .filter { !excluded.contains(it.id) }
            .firstOrNull { isMessageEligible(it, helper) } ?: return null

        // If this is an experimental message, but not a placebo, then just return the message.
        if (!message.data.isControl) {
            return message
        }

        // This is a control message which we're definitely not going to show to anyone,
        // however, we need to do the bookkeeping and as if we were.
        //
        // Since no one is going to see it, then we need to do it ourselves, here.
        runBlocking {
            onMessageDisplayed(message)
        }

        // This is a control, so we need to either return the next message (there may not be one)
        // or not display anything.
        return when (getOnControlBehavior()) {
            ControlMessageBehavior.SHOW_NEXT_MESSAGE ->
                getNextMessage(
                    surface,
                    availableMessages,
                    excluded + message.id,
                    helper,
                )

            ControlMessageBehavior.SHOW_NONE -> null
        }
    }

    /**
     * Returns a pair of uuid and valid action for the provided [action].
     *
     * Uses Nimbus' targeting attributes to do basic string interpolation.
     *
     * e.g.
     * `https://example.com/{locale}/whatsnew.html?version={app_version}`
     *
     * If the string `{uuid}` is detected in the [action] string, then it is
     * replaced with a random UUID. This is returned as the first value of the returned
     * [Pair].
     *
     * The fully resolved (with all substitutions) action is returned as the second value
     * of the [Pair].
     */
    fun generateUuidAndFormatAction(action: String): Pair<String?, String> =
        createMessagingHelper().use { helper ->
            val uuid = helper.getUuid(action)
            val url = helper.stringFormat(action, uuid)
            Pair(uuid, url)
        }

    /**
     * Record the time and optional [bootIdentifier] of the display of the given message.
     *
     * If the message is part of an experiment, then record an exposure event for that
     * experiment.
     *
     * This is determined by the value in the [message.data.experiment] property.
     */
    suspend fun onMessageDisplayed(message: Message, bootIdentifier: String? = null): Message {
        // Record an exposure event if this is an experimental message.
        val slug = message.data.experiment
        if (slug != null) {
            // We know that it's experimental, and we know which experiment it came from.
            messagingFeature.recordExperimentExposure(slug)
        } else if (message.data.isControl) {
            // It's not experimental, but it is a control. This is obviously malformed.
            reportMalformedMessage(message.id)
        }

        // Now update the display counts.
        val updatedMetadata = message.metadata.copy(
            displayCount = message.metadata.displayCount + 1,
            lastTimeShown = now(),
            latestBootIdentifier = bootIdentifier,
        )
        val nextMessage = message.copy(
            metadata = updatedMetadata,
        )
        updateMetadata(nextMessage.metadata)
        return nextMessage
    }

    /**
     * Updated the provided [metadata] in the storage.
     */
    suspend fun updateMetadata(metadata: Message.Metadata) {
        metadataStorage.updateMetadata(metadata)
    }

    @VisibleForTesting
    internal fun sanitizeAction(
        unsafeAction: String,
        nimbusActions: Map<String, String>,
    ): String? =
        when {
            unsafeAction.startsWith("http") -> {
                unsafeAction
            }
            else -> {
                val safeAction = nimbusActions[unsafeAction]
                if (safeAction.isNullOrBlank() || safeAction.isEmpty()) {
                    null
                } else {
                    safeAction
                }
            }
        }

    @VisibleForTesting
    internal fun sanitizeTriggers(
        unsafeTriggers: List<String>,
        nimbusTriggers: Map<String, String>,
    ): List<String>? =
        unsafeTriggers.map {
            val safeTrigger = nimbusTriggers[it]
            if (safeTrigger.isNullOrBlank() || safeTrigger.isEmpty()) {
                return null
            }
            safeTrigger
        }

    @VisibleForTesting
    internal fun sanitizeStyle(
        unsafeStyle: String,
        nimbusStyles: Map<String, StyleData>,
    ): StyleData? = nimbusStyles[unsafeStyle]

    /**
     * Return true if the message passed as a parameter is eligible
     *
     * Aimed to be used from tests only, but currently public because some tests inside Fenix need
     * it. This should be set as internal when this bug is fixed:
     * https://bugzilla.mozilla.org/show_bug.cgi?id=1823472
     */
    @VisibleForTesting
    fun isMessageEligible(
        message: Message,
        helper: NimbusMessagingHelperInterface,
    ): Boolean {
        return message.triggers.all { condition ->
            try {
                if (malFormedMap.containsKey(condition)) {
                    return false
                }
                helper.evalJexl(condition)
            } catch (e: NimbusException.EvaluationException) {
                reportMalformedMessage(message.id)
                malFormedMap[condition] = message.id
                logger.info("Unable to evaluate $condition")
                false
            }
        }
    }

    @VisibleForTesting
    internal fun getOnControlBehavior(): ControlMessageBehavior = messagingFeature.value().onControl

    private suspend fun addMetadata(id: String): Message.Metadata {
        return metadataStorage.addMetadata(
            Message.Metadata(
                id = id,
            ),
        )
    }
}

/**
 * A helper method to safely destroy the message helper after use.
 */
fun <R> NimbusMessagingHelperInterface.use(block: (NimbusMessagingHelperInterface) -> R) =
    block(this).also {
        this.destroy()
    }
