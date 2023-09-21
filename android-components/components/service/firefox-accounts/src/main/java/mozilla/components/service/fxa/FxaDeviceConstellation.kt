/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import mozilla.appservices.fxaclient.FxaAction
import mozilla.appservices.fxaclient.FxaClient
import mozilla.appservices.syncmanager.SyncTelemetry
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.sync.AccountEvent
import mozilla.components.concept.sync.AccountEventsObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.ConstellationState
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.DeviceCommandOutgoing
import mozilla.components.concept.sync.DeviceConfig
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.DeviceConstellationObserver
import mozilla.components.concept.sync.DevicePushSubscription
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import kotlin.coroutines.CoroutineContext

internal sealed class FxaDeviceConstellationException : Exception() {
    /**
     * Failure while ensuring device capabilities.
     */
    class EnsureCapabilitiesFailed : FxaDeviceConstellationException()
}

/**
 * Provides an implementation of [DeviceConstellation] backed by an [FxaClient].
 */
class FxaDeviceConstellation(
    private val account: FxaClient,
    private val coroutineContext: CoroutineContext,
    @get:VisibleForTesting
    internal val crashReporter: CrashReporting? = null,
) : DeviceConstellation, Observable<AccountEventsObserver> by ObserverRegistry() {
    private val logger = Logger("FxaDeviceConstellation")
    private val deviceObserverRegistry = ObserverRegistry<DeviceConstellationObserver>()

    @Volatile
    private var constellationState: ConstellationState? = null

    override fun state(): ConstellationState? = constellationState

    override fun finalizeDevice(authType: AuthType, config: DeviceConfig) {
        when (authType) {
            AuthType.Signin,
            AuthType.Signup,
            AuthType.Pairing,
            is AuthType.OtherExternal,
            AuthType.MigratedCopy,
            -> {
                account.queueAction(
                    FxaAction.InitializeDevice(
                        config.name,
                        config.type.into(),
                        config.capabilities.map({ it.into() }).toList(),
                    ),
                )
            }
            AuthType.Existing,
            AuthType.MigratedReuse,
            -> {
                account.queueAction(
                    FxaAction.EnsureCapabilities(
                        config.capabilities.map({ it.into() }).toList(),
                    ),
                )
            }
            else -> Unit
        }
    }

    override suspend fun processRawEvent(payload: String) = withContext(coroutineContext) {
        val result = handleFxaExceptions(logger, "processing raw commands") {
            val events = when (val accountEvent: AccountEvent = account.handlePushMessage(payload).into()) {
                is AccountEvent.DeviceCommandIncoming -> account.pollDeviceCommands().map {
                    AccountEvent.DeviceCommandIncoming(command = it.into())
                }
                else -> listOf(accountEvent)
            }
            processEvents(events)
        }
        result != null
    }

    @MainThread
    override fun registerDeviceObserver(
        observer: DeviceConstellationObserver,
        owner: LifecycleOwner,
        autoPause: Boolean,
    ) {
        logger.debug("registering device observer")
        deviceObserverRegistry.register(observer, owner, autoPause)
    }

    override suspend fun setDeviceName(name: String, context: Context): Boolean {
        val result = CompletableDeferred<Boolean>()
        account.queueAction(FxaAction.SetDeviceName(name, result))
        val success = result.await()
        if (success) {
            FxaDeviceSettingsCache(context).updateCachedName(name)
        }
        return success
    }

    override suspend fun setDevicePushSubscription(subscription: DevicePushSubscription): Boolean {
        val result = CompletableDeferred<Boolean>()
        account.queueAction(
            FxaAction.SetDevicePushSubscription(
                subscription.endpoint,
                subscription.publicKey,
                subscription.authKey,
                result,
            ),
        )
        return result.await()
    }

    override suspend fun sendCommandToDevice(
        targetDeviceId: String,
        outgoingCommand: DeviceCommandOutgoing,
    ) = withContext(coroutineContext) {
        when (outgoingCommand) {
            is DeviceCommandOutgoing.SendTab -> {
                val result = CompletableDeferred<Boolean>()
                account.queueAction(
                    FxaAction.SendSingleTab(
                        targetDeviceId,
                        outgoingCommand.title,
                        outgoingCommand.url,
                        result,
                    ),
                )
                result.await()
            }
            else -> {
                logger.debug("Skipped sending unsupported command type: $outgoingCommand")
                false
            }
        }
    }

    // Poll for missed commands. Commands are the only event-type that can be
    // polled for, although missed commands will be delivered as AccountEvents.
    override suspend fun pollForCommands() = withContext(coroutineContext) {
        val events = handleFxaExceptions(logger, "polling for device commands") {
            account.pollDeviceCommands().map { AccountEvent.DeviceCommandIncoming(command = it.into()) }
        }

        if (events == null) {
            false
        } else {
            processEvents(events)
            val errors: List<Throwable> = SyncTelemetry.processFxaTelemetry(account.gatherTelemetry())
            for (error in errors) {
                crashReporter?.submitCaughtException(error)
            }
            true
        }
    }

    private fun processEvents(events: List<AccountEvent>) {
        notifyObservers { onEvents(events) }
    }

    override suspend fun refreshDevices(): Boolean {
        return withContext(coroutineContext) {
            logger.info("Refreshing device list...")

            // Attempt to fetch devices, or bail out on failure.
            val allDevices = fetchAllDevices() ?: return@withContext false

            // Find the current device.
            val currentDevice = allDevices.find { it.isCurrentDevice }?.also {
                // If our current device's push subscription needs to be renewed, then we
                // possibly missed some push notifications, so check for that here.
                // (This doesn't actually perform the renewal, FxaPushSupportFeature does that.)
                if (it.subscription == null || it.subscriptionExpired) {
                    logger.info("Current device needs push endpoint registration, so checking for missed commands")
                    pollForCommands()
                }
            }

            // Filter out the current devices.
            val otherDevices = allDevices.filter { !it.isCurrentDevice }

            val newState = ConstellationState(currentDevice, otherDevices)
            constellationState = newState

            logger.info("Refreshed device list; saw ${allDevices.size} device(s).")

            // NB: at this point, 'constellationState' might have changed.
            // Notify with an immutable, local 'newState' instead.
            deviceObserverRegistry.notifyObservers {
                logger.info("Notifying observer about constellation updates.")
                onDevicesUpdate(newState)
            }
            true
        }
    }

    /**
     * Get all devices in the constellation.
     * @return A list of all devices in the constellation, or `null` on failure.
     */
    private suspend fun fetchAllDevices(): List<Device>? {
        return handleFxaExceptions(logger, "fetching all devices") {
            account.getDevices().map { it.into() }
        }
    }
}
