/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import mozilla.appservices.fxaclient.FxaAction
import mozilla.appservices.fxaclient.IncomingDeviceCommand
import mozilla.appservices.fxaclient.SendTabPayload
import mozilla.appservices.fxaclient.TabHistoryEntry
import mozilla.components.concept.sync.AccountEvent
import mozilla.components.concept.sync.AccountEventsObserver
import mozilla.components.concept.sync.ConstellationState
import mozilla.components.concept.sync.DeviceCommandIncoming
import mozilla.components.concept.sync.DeviceCommandOutgoing
import mozilla.components.concept.sync.DeviceConstellationObserver
import mozilla.components.concept.sync.TabData
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import mozilla.appservices.fxaclient.AccountEvent as ASAccountEvent
import mozilla.appservices.fxaclient.Device as NativeDevice
import mozilla.appservices.fxaclient.DevicePushSubscription as NativeDevicePushSubscription
import mozilla.appservices.fxaclient.FxaClient as NativeFirefoxAccount
import mozilla.appservices.sync15.DeviceType as RustDeviceType

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FxaDeviceConstellationTest {
    lateinit var account: NativeFirefoxAccount
    lateinit var constellation: FxaDeviceConstellation

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @Before
    fun setup() {
        account = mock()
        val captor = argumentCaptor<FxaAction>()
        `when`(account.queueAction(captor.capture())).thenAnswer {
            val action = captor.value
            when (action) {
                is FxaAction.SetDeviceName -> action.result?.complete(true)
                is FxaAction.SetDevicePushSubscription -> action.result?.complete(true)
                is FxaAction.SendSingleTab -> action.result?.complete(true)
                else -> Unit
            }
        }
        val scope = CoroutineScope(coroutinesTestRule.testDispatcher) + SupervisorJob()
        constellation = FxaDeviceConstellation(account, scope.coroutineContext, mock())
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `process raw device command`() = runTestOnMain {
        // No commands, no observer.
        `when`(account.handlePushMessage("raw events payload")).thenReturn(mozilla.appservices.fxaclient.AccountEvent.Unknown)
        assertTrue(constellation.processRawEvent("raw events payload"))

        // No commands, with observer.
        val eventsObserver = object : AccountEventsObserver {
            var latestEvents: List<AccountEvent>? = null

            override fun onEvents(events: List<AccountEvent>) {
                latestEvents = events
            }
        }

        // No commands, with an observer.
        constellation.register(eventsObserver)
        assertTrue(constellation.processRawEvent("raw events payload"))
        assertEquals(listOf(AccountEvent.Unknown), eventsObserver.latestEvents)

        // Some commands, with an observer. More detailed command handling tests below.
        val testDevice1 = testDevice("test1", false)
        val testTab1 = TabHistoryEntry("Hello", "http://world.com/1")
        `when`(account.handlePushMessage("raw events payload")).thenReturn(
            ASAccountEvent.CommandReceived(
                command = IncomingDeviceCommand.TabReceived(testDevice1, SendTabPayload(listOf(testTab1), "flowid", "streamid")),
            ),
        )

        `when`(account.pollDeviceCommands()).thenReturn(
            arrayOf(
                IncomingDeviceCommand.TabReceived(testDevice1, SendTabPayload(listOf(testTab1), "flowid", "streamid")),
            ),
        )
        assertTrue(constellation.processRawEvent("raw events payload"))
        verify(account).pollDeviceCommands()

        val events = eventsObserver.latestEvents!!
        val command = (events[0] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(testDevice1.into(), (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf(testTab1.into()), command.entries)
    }

    @Test
    fun `send command to device`() = runTestOnMain {
        `when`(account.gatherTelemetry()).thenReturn("{}")
        assertTrue(
            constellation.sendCommandToDevice(
                "targetID",
                DeviceCommandOutgoing.SendTab("Mozilla", "https://www.mozilla.org"),
            ),
        )

        verify(account).queueAction(FxaAction.SendSingleTab("targetID", "Mozilla", "https://www.mozilla.org", any()))
    }

    @ExperimentalCoroutinesApi
    fun `refreshing constellation`() = runTestOnMain {
        // No devices, no observers.
        `when`(account.getDevices()).thenReturn(emptyArray())

        assertTrue(constellation.refreshDevices())

        val observer = object : DeviceConstellationObserver {
            var state: ConstellationState? = null

            override fun onDevicesUpdate(constellation: ConstellationState) {
                state = constellation
            }
        }
        constellation.registerDeviceObserver(observer, startedLifecycleOwner(), false)

        // No devices, with an observer.
        assertTrue(constellation.refreshDevices())
        assertEquals(ConstellationState(null, listOf()), observer.state)

        val testDevice1 = testDevice("test1", false)
        val testDevice2 = testDevice("test2", false)
        val currentDevice = testDevice("currentTestDevice", true)

        // Single device, no current device.
        `when`(account.getDevices()).thenReturn(arrayOf(testDevice1))
        assertTrue(constellation.refreshDevices())

        assertEquals(ConstellationState(null, listOf(testDevice1.into())), observer.state)
        assertEquals(ConstellationState(null, listOf(testDevice1.into())), constellation.state())

        // Current device, no other devices.
        `when`(account.getDevices()).thenReturn(arrayOf(currentDevice))
        assertTrue(constellation.refreshDevices())
        assertEquals(ConstellationState(currentDevice.into(), listOf()), observer.state)
        assertEquals(ConstellationState(currentDevice.into(), listOf()), constellation.state())

        // Current device with other devices.
        `when`(account.getDevices()).thenReturn(
            arrayOf(
                currentDevice,
                testDevice1,
                testDevice2,
            ),
        )
        assertTrue(constellation.refreshDevices())

        assertEquals(ConstellationState(currentDevice.into(), listOf(testDevice1.into(), testDevice2.into())), observer.state)
        assertEquals(ConstellationState(currentDevice.into(), listOf(testDevice1.into(), testDevice2.into())), constellation.state())

        // Current device with expired subscription.
        val currentDeviceExpired = testDevice("currentExpired", true, expired = true)
        `when`(account.getDevices()).thenReturn(
            arrayOf(
                currentDeviceExpired,
                testDevice2,
            ),
        )

        `when`(account.pollDeviceCommands()).thenReturn(emptyArray())
        `when`(account.gatherTelemetry()).thenReturn("{}")

        assertTrue(constellation.refreshDevices())

        verify(account, times(1)).pollDeviceCommands()

        assertEquals(ConstellationState(currentDeviceExpired.into(), listOf(testDevice2.into())), observer.state)
        assertEquals(ConstellationState(currentDeviceExpired.into(), listOf(testDevice2.into())), constellation.state())

        // Current device with no subscription.
        val currentDeviceNoSub = testDevice("currentNoSub", true, expired = false, subscribed = false)

        `when`(account.getDevices()).thenReturn(
            arrayOf(
                currentDeviceNoSub,
                testDevice2,
            ),
        )

        `when`(account.pollDeviceCommands()).thenReturn(emptyArray())
        `when`(account.gatherTelemetry()).thenReturn("{}")

        assertTrue(constellation.refreshDevices())

        verify(account, times(2)).pollDeviceCommands()
        assertEquals(ConstellationState(currentDeviceNoSub.into(), listOf(testDevice2.into())), constellation.state())
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `polling for commands triggers observers`() = runTestOnMain {
        // No commands, no observers.
        `when`(account.gatherTelemetry()).thenReturn("{}")
        `when`(account.pollDeviceCommands()).thenReturn(emptyArray())
        assertTrue(constellation.pollForCommands())

        val eventsObserver = object : AccountEventsObserver {
            var latestEvents: List<AccountEvent>? = null

            override fun onEvents(events: List<AccountEvent>) {
                latestEvents = events
            }
        }

        // No commands, with an observer.
        constellation.register(eventsObserver)
        assertTrue(constellation.pollForCommands())
        assertEquals(listOf<AccountEvent>(), eventsObserver.latestEvents)

        // Some commands.
        `when`(account.pollDeviceCommands()).thenReturn(
            arrayOf(
                IncomingDeviceCommand.TabReceived(null, SendTabPayload(emptyList(), "", "")),
            ),
        )
        assertTrue(constellation.pollForCommands())

        var command = (eventsObserver.latestEvents!![0] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(null, (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf<TabData>(), command.entries)

        val testDevice1 = testDevice("test1", false)
        val testDevice2 = testDevice("test2", false)
        val testTab1 = TabHistoryEntry("Hello", "http://world.com/1")
        val testTab2 = TabHistoryEntry("Hello", "http://world.com/2")
        val testTab3 = TabHistoryEntry("Hello", "http://world.com/3")

        // Zero tabs from a single device.
        `when`(account.pollDeviceCommands()).thenReturn(
            arrayOf(
                IncomingDeviceCommand.TabReceived(testDevice1, SendTabPayload(emptyList(), "", "")),
            ),
        )
        assertTrue(constellation.pollForCommands())

        Assert.assertNotNull(eventsObserver.latestEvents)
        assertEquals(1, eventsObserver.latestEvents!!.size)
        command = (eventsObserver.latestEvents!![0] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(testDevice1.into(), (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf<TabData>(), command.entries)

        // Single tab from a single device.
        `when`(account.pollDeviceCommands()).thenReturn(
            arrayOf(
                IncomingDeviceCommand.TabReceived(testDevice2, SendTabPayload(listOf(testTab1), "", "")),
            ),
        )
        assertTrue(constellation.pollForCommands())

        command = (eventsObserver.latestEvents!![0] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(testDevice2.into(), (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf(testTab1.into()), command.entries)

        // Multiple tabs from a single device.
        `when`(account.pollDeviceCommands()).thenReturn(
            arrayOf(
                IncomingDeviceCommand.TabReceived(testDevice2, SendTabPayload(listOf(testTab1, testTab3), "", "")),
            ),
        )
        assertTrue(constellation.pollForCommands())

        command = (eventsObserver.latestEvents!![0] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(testDevice2.into(), (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf(testTab1.into(), testTab3.into()), command.entries)

        // Multiple tabs received from multiple devices.
        `when`(account.pollDeviceCommands()).thenReturn(
            arrayOf(
                IncomingDeviceCommand.TabReceived(testDevice2, SendTabPayload(listOf(testTab1, testTab2), "", "")),
                IncomingDeviceCommand.TabReceived(testDevice1, SendTabPayload(listOf(testTab3), "", "")),
            ),
        )
        assertTrue(constellation.pollForCommands())

        command = (eventsObserver.latestEvents!![0] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(testDevice2.into(), (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf(testTab1.into(), testTab2.into()), command.entries)
        command = (eventsObserver.latestEvents!![1] as AccountEvent.DeviceCommandIncoming).command
        assertEquals(testDevice1.into(), (command as DeviceCommandIncoming.TabReceived).from)
        assertEquals(listOf(testTab3.into()), command.entries)

        // TODO FirefoxAccount needs @Throws annotations for these tests to actually work.
        // Failure to poll for commands. Panics are re-thrown.
//        `when`(account.pollDeviceCommands()).thenThrow(FxaPanicException("Don't panic!"))
//        try {
//            runBlocking(coroutinesTestRule.testDispatcher) {
//                constellation.refreshAsync()
//            }
//            fail()
//        } catch (e: FxaPanicException) {}
//
//        // Network exception are handled.
//        `when`(account.pollDeviceCommands()).thenThrow(FxaNetworkException("four oh four"))
//        runBlocking(coroutinesTestRule.testDispatcher) {
//            Assert.assertFalse(constellation.refreshAsync())
//        }
//        // Unspecified exception are handled.
//        `when`(account.pollDeviceCommands()).thenThrow(FxaUnspecifiedException("hmmm..."))
//        runBlocking(coroutinesTestRule.testDispatcher) {
//            Assert.assertFalse(constellation.refreshAsync())
//        }
//        // Unauthorized exception are handled.
//        val authErrorObserver = object : AuthErrorObserver {
//            var latestException: AuthException? = null
//
//            override fun onAuthErrorAsync(e: AuthException): Deferred<Unit> {
//                latestException = e
//                val r = CompletableDeferred<Unit>()
//                r.complete(Unit)
//                return r
//            }
//        }
//        authErrorRegistry.register(authErrorObserver)
//
//        val authException = FxaUnauthorizedException("oh no you didn't!")
//        `when`(account.pollDeviceCommands()).thenThrow(authException)
//        runBlocking(coroutinesTestRule.testDispatcher) {
//            Assert.assertFalse(constellation.refreshAsync())
//        }
//        assertEquals(authErrorObserver.latestException!!.cause, authException)
    }

    private fun testDevice(id: String, current: Boolean, expired: Boolean = false, subscribed: Boolean = true): NativeDevice {
        return NativeDevice(
            id = id,
            displayName = "testName",
            deviceType = RustDeviceType.MOBILE,
            isCurrentDevice = current,
            lastAccessTime = 123L,
            capabilities = listOf(),
            pushEndpointExpired = expired,
            pushSubscription = if (subscribed) NativeDevicePushSubscription("http://endpoint.com", "pk", "auth key") else null,
        )
    }

    private fun startedLifecycleOwner(): LifecycleOwner {
        val lifecycleOwner = mock<LifecycleOwner>()
        val lifecycle = mock<Lifecycle>()
        `when`(lifecycle.currentState).thenReturn(Lifecycle.State.STARTED)
        `when`(lifecycleOwner.lifecycle).thenReturn(lifecycle)
        return lifecycleOwner
    }
}
