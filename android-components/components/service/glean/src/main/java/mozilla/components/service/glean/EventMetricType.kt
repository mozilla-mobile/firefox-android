/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.service.glean.storages.EventsStorageEngine
import mozilla.components.service.glean.storages.RecordedEventData
import mozilla.components.support.base.log.logger.Logger

/**
 * This implements the developer facing API for recording events.
 *
 * Instances of this class type are automatically generated by the parsers at built time,
 * allowing developers to record events that were previously registered in the metrics.yaml file.
 *
 * The Events API only exposes the [record] method, which takes care of validating the input
 * data and making sure that limits are enforced.
 */
data class EventMetricType(
    override val disabled: Boolean,
    override val category: String,
    override val lifetime: Lifetime,
    override val name: String,
    override val sendInPings: List<String>,
    val objects: List<String>,
    val allowedExtraKeys: List<String>? = null
) : CommonMetricData {

    override val defaultStorageDestinations: List<String> = listOf("events")

    private val logger = Logger("glean/EventMetricType")

    // Holds the Job returned from launch{} for awaiting purposes
    private var ioTask: Job? = null

    companion object {
        // Maximum length of any string value in the extra dictionary, in characters
        internal const val MAX_LENGTH_EXTRA_KEY_VALUE = 80
        // Maximum length of any passed value string, in characters
        internal const val MAX_LENGTH_VALUE = 80
    }

    /**
     * Record an event by using the information provided by the instance of this class.
     *
     * @param objectId the object the event occurred on, e.g. 'reload_button'. The maximum
     *                 length of this string is defined by [MAX_LENGTH_OBJECT_ID]
     * @param value optional. This is a user defined value, providing context for the event. The
     *              maximum length of this string is defined by [MAX_LENGTH_VALUE]
     * @param extra optional. This is map, both keys and values need to be strings, keys are
     *              identifiers. This is used for events where additional richer context is needed.
     *              The maximum length for values is defined by [MAX_LENGTH_EXTRA_KEY_VALUE]
     */
    @Suppress("ReturnCount", "ComplexMethod")
    fun record(objectId: String, value: String? = null, extra: Map<String, String>? = null) {
        // TODO report errors through other special metrics handled by the SDK. See bug 1499761.
        if (!shouldRecord(logger)) {
            return
        }

        if (lifetime != Lifetime.Ping) {
            logger.warn("$category.$name can only have a Ping lifetime")
            return
        }

        // We don't need to check that the objectId is short, since that
        // has already been determined at build time for each of the valid objectId values.
        if (!objects.contains(objectId)) {
            logger.warn("objectId '$objectId' is not valid on the $category.$name metric")
            return
        }

        val truncatedValue = value?.let {
            if (it.length > MAX_LENGTH_VALUE) {
                logger.warn("Value parameter exceeds maximum string length, truncating.")
                return@let it.substring(0, MAX_LENGTH_VALUE)
            }
            it
        }

        // Check if the provided extra keys are allowed and have sane values.
        val truncatedExtraKeys = extra?.toMutableMap()?.let { eventKeys ->
            if (allowedExtraKeys == null) {
                logger.error("Cannot use extra keys are no extra keys are defined.")
                return
            }

            for ((key, extraValue) in eventKeys) {
                if (!allowedExtraKeys.contains(key)) {
                    logger.error("$key extra key is not allowed for $category.$name.")
                    return
                }

                if (extraValue.length > MAX_LENGTH_EXTRA_KEY_VALUE) {
                    logger.warn("$extraValue for $key is too long for $category.$name, truncating.")
                    eventKeys[key] = extraValue.substring(0, MAX_LENGTH_EXTRA_KEY_VALUE)
                }
            }
            eventKeys
        }

        ioTask = Dispatchers.API.launch {
            // Delegate storing the event to the storage engine.
            EventsStorageEngine.record(
                stores = getStorageNames(),
                category = category,
                name = name,
                objectId = objectId,
                value = truncatedValue,
                extra = truncatedExtraKeys
            )
        }
    }

    /**
     * Tests whether a value is stored for the metric for testing purposes only. This function will
     * attempt to await the last task (if any) writing to the the metric's storage engine before
     * returning a value.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.  Defaults
     *                 to the either the first value in [defaultStorageDestinations] or the first
     *                 value in [sendInPings]
     * @return true if metric value exists, otherwise false
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun testHasValue(pingName: String = getStorageNames().first()): Boolean {
        ioTask?.let { awaitJob(it) }

        val snapshot = EventsStorageEngine.getSnapshot(pingName, false) ?: return false
        return snapshot.any { event ->
            event.identifier == identifier
        }
    }

    /**
     * Returns the stored value for testing purposes only. This function will attempt to await the
     * last task (if any) writing to the the metric's storage engine before returning a value.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.  Defaults
     *                 to the either the first value in [defaultStorageDestinations] or the first
     *                 value in [sendInPings]
     * @return value of the stored metric
     * @throws [NullPointerException] if no value is stored
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun testGetValue(pingName: String = getStorageNames().first()): List<RecordedEventData> {
        ioTask?.let { awaitJob(it) }

        return EventsStorageEngine.getSnapshot(pingName, false)!!.filter { event ->
            event.identifier == identifier
        }
    }
}
