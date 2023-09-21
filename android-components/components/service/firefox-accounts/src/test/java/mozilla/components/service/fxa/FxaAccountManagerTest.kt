/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mozilla.appservices.fxaclient.FxaAction
import mozilla.appservices.fxaclient.FxaAuthEventKind
import mozilla.appservices.fxaclient.FxaAuthState
import mozilla.appservices.fxaclient.FxaEvent
import mozilla.appservices.fxaclient.FxaServer
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.sync.AccountEventsObserver
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthFlowError
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.DeviceCapability
import mozilla.components.concept.sync.DeviceConfig
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.FxAEntryPoint
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.StatePersistenceCallback
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.manager.SyncEnginesStorage
import mozilla.components.service.fxa.sync.SyncDispatcher
import mozilla.components.service.fxa.sync.SyncManager
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.service.fxa.sync.SyncStatusObserver
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

internal class TestableStorageWrapper(
    manager: FxaAccountManager,
    accountEventObserverRegistry: ObserverRegistry<AccountEventsObserver>,
    serverConfig: ServerConfig,
    val mockAccount: FirefoxAccount,
) : StorageWrapper(manager, accountEventObserverRegistry, serverConfig) {
    override fun obtainAccount(): FirefoxAccount = mockAccount
}

// Same as the actual account manager, except we get to control how FirefoxAccountShaped instances
// are created. This is necessary because due to some build issues (native dependencies not available
// within the test environment) we can't use fxaclient supplied implementation of FirefoxAccountShaped.
// Instead, we express all of our account-related operations over an interface.
internal open class TestableFxaAccountManager(
    context: Context,
    config: ServerConfig,
    coroutineContext: CoroutineContext,
    private val storage: AccountStorage = mock<AccountStorage>(),
    capabilities: Set<DeviceCapability> = emptySet(),
    syncConfig: SyncConfig? = null,
    crashReporter: CrashReporting? = null,
) : FxaAccountManager(context, config, DeviceConfig("test", DeviceType.UNKNOWN, capabilities), syncConfig, emptySet(), crashReporter) {
    val accountObserver = mock<AccountObserver>()
    val deviceConstellation = mock<DeviceConstellation>()
    val mockAccount: FirefoxAccount = mock<FirefoxAccount>().also {
        // Note: getAuthState should only be called once to get the initial auth state,
        // FxaAccountManager uses FxaEvent.AuthEvent to track changes
        whenever(it.getAuthState()).thenReturn(FxaAuthState.DISCONNECTED)
        whenever(it.clientCoroutineContext).thenReturn(coroutineContext)
        whenever(it.deviceConstellation()).thenReturn(deviceConstellation)
        whenever(it.getCurrentDeviceId()).thenReturn("testDeviceId")
        val captor = argumentCaptor<FxaAction>()
        whenever(it.queueAction(captor.capture())).thenAnswer {
            val action = captor.value
            when (action) {
                is FxaAction.BeginOAuthFlow -> action.result?.complete(EXPECTED_OAUTH_URL)
                is FxaAction.BeginPairingFlow -> action.result?.complete(EXPECTED_OAUTH_URL)
                is FxaAction.SetDeviceName -> action.result?.complete(true)
                is FxaAction.SetDevicePushSubscription -> action.result?.complete(true)
                is FxaAction.SendSingleTab -> action.result?.complete(true)
                else -> Unit
            }
        }
    }

    init {
        register(accountObserver)
    }

    private val testableStorageWrapper = TestableStorageWrapper(this, accountEventObserverRegistry, serverConfig, mockAccount)

    override var syncStatusObserverRegistry = ObserverRegistry<SyncStatusObserver>()

    override fun getStorageWrapper(): StorageWrapper {
        return testableStorageWrapper
    }

    override fun getAccountStorage(): AccountStorage {
        return storage
    }

    override fun createSyncManager(config: SyncConfig): SyncManager = mock()
}

const val EXPECTED_AUTH_STATE = "goodAuthState"
const val UNEXPECTED_AUTH_STATE = "badAuthState"
const val EXPECTED_OAUTH_URL = "http://example.com/oauth-flow-start?state=$EXPECTED_AUTH_STATE"

@ExperimentalCoroutinesApi // for runTest
@RunWith(AndroidJUnit4::class)
class FxaAccountManagerTest {

    val entryPoint: FxAEntryPoint = mock<FxAEntryPoint>().apply {
        whenever(entryName).thenReturn("home-menu")
    }

    @After
    fun cleanup() {
        SyncAuthInfoCache(testContext).clear()
        SyncEnginesStorage(testContext).clear()
    }

    internal class TestSyncDispatcher(registry: ObserverRegistry<SyncStatusObserver>) : SyncDispatcher, Observable<SyncStatusObserver> by registry {
        val inner: SyncDispatcher = mock()
        override fun isSyncActive(): Boolean {
            return inner.isSyncActive()
        }

        override fun syncNow(
            reason: SyncReason,
            debounce: Boolean,
            customEngineSubset: List<SyncEngine>,
        ) {
            inner.syncNow(reason, debounce, customEngineSubset)
        }

        override fun startPeriodicSync(unit: TimeUnit, period: Long, initialDelay: Long) {
            inner.startPeriodicSync(unit, period, initialDelay)
        }

        override fun stopPeriodicSync() {
            inner.stopPeriodicSync()
        }

        override fun workersStateChanged(isRunning: Boolean) {
            inner.workersStateChanged(isRunning)
        }

        override fun close() {
            inner.close()
        }
    }

    internal class TestSyncManager(config: SyncConfig) : SyncManager(config) {
        val dispatcherRegistry = ObserverRegistry<SyncStatusObserver>()
        val dispatcher: TestSyncDispatcher = TestSyncDispatcher(dispatcherRegistry)

        private var dispatcherUpdatedCount = 0
        override fun createDispatcher(supportedEngines: Set<SyncEngine>): SyncDispatcher {
            return dispatcher
        }

        override fun dispatcherUpdated(dispatcher: SyncDispatcher) {
            dispatcherUpdatedCount++
        }
    }

    class TestSyncStatusObserver : SyncStatusObserver {
        var onStartedCount = 0
        var onIdleCount = 0
        var onErrorCount = 0

        override fun onStarted() {
            onStartedCount++
        }

        override fun onIdle() {
            onIdleCount++
        }

        override fun onError(error: Exception?) {
            onErrorCount++
        }
    }

    @Test
    fun `restored account state persistence`() = runTest {
        val accountStorage: AccountStorage = mock()

        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "http://auth-url/redirect", null),
            coroutineContext,
            accountStorage,
            setOf(DeviceCapability.SEND_TAB),
            null,
        )

        // We have an account at the start.
        verify(manager.mockAccount, never()).registerPersistenceCallback(any())
        manager.start()
        val captor = argumentCaptor<StatePersistenceCallback>()
        verify(manager.mockAccount, times(1)).registerPersistenceCallback(captor.capture())

        // Assert that persistence callback is interacting with the storage layer.
        captor.value.persist("test")
        verify(accountStorage).write("test")
    }

    @Test
    fun `error reading persisted account`() = runTest {
        val accountStorage = mock<AccountStorage>()
        val readException = FxaNetworkException("pretend we failed to fetch the account")
        `when`(accountStorage.read()).thenThrow(readException)

        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
            accountStorage,
        )

        val accountObserver = object : AccountObserver {
            override fun onLoggedOut() {
                fail()
            }

            override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
                fail()
            }

            override fun onAuthenticationProblems() {
                fail()
            }

            override fun onProfileUpdated(profile: Profile) {
                fail()
            }
        }

        manager.register(accountObserver)
        manager.start()
    }

    @Test
    fun `no persisted account`() = runTest {
        val accountStorage = mock<AccountStorage>()
        // There's no account at the start.
        `when`(accountStorage.read()).thenReturn(null)

        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
            accountStorage,
        )

        val accountObserver: AccountObserver = mock()

        manager.register(accountObserver)
        manager.start()

        verify(accountObserver, never()).onAuthenticated(any(), any())
        verify(accountObserver, never()).onProfileUpdated(any())
        verify(accountObserver, never()).onLoggedOut()

        verify(accountStorage, times(1)).read()
        verify(accountStorage, never()).write(any())
        verify(accountStorage, never()).clear()

        assertNull(manager.authenticatedAccount())
        assertNull(manager.accountProfile())
    }

    @Test
    fun `with persisted account and profile`() = runTest {
        val accountStorage = mock<AccountStorage>()
        val constellation: DeviceConstellation = mock()
        val profile = Profile(
            "testUid",
            "test@example.com",
            null,
            "Test Profile",
        )
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
            accountStorage,
            emptySet(),
            null,
        )
        val mockAccount = manager.mockAccount
        `when`(mockAccount.getProfile(ignoreCache = false)).thenReturn(profile)
        // We have a connected account at the start.
        `when`(accountStorage.read()).thenReturn(mockAccount)
        `when`(manager.mockAccount.getAuthState()).thenReturn(FxaAuthState.CONNECTED)
        `when`(mockAccount.getCurrentDeviceId()).thenReturn("testDeviceId")
        `when`(mockAccount.deviceConstellation()).thenReturn(constellation)

        val accountObserver: AccountObserver = mock()

        manager.register(accountObserver)

        manager.start()

        // Make sure that account and profile observers are fired exactly once.
        verify(accountObserver, times(1)).onAuthenticated(mockAccount, AuthType.Existing)
        verify(accountObserver, times(1)).onProfileUpdated(profile)
        verify(accountObserver, never()).onLoggedOut()

        verify(accountStorage, times(1)).read()
        verify(accountStorage, never()).write(any())
        verify(accountStorage, never()).clear()

        assertEquals(mockAccount, manager.authenticatedAccount())
        assertEquals(profile, manager.accountProfile())

        // Make sure 'logoutAsync' clears out state and fires correct observers.
        reset(accountObserver)
        reset(accountStorage)
        `when`(mockAccount.disconnect()).thenReturn(true)

        // Simulate SyncManager populating SyncEnginesStorage with some state.
        SyncEnginesStorage(testContext).setStatus(SyncEngine.History, true)
        SyncEnginesStorage(testContext).setStatus(SyncEngine.Passwords, false)
        assertTrue(SyncEnginesStorage(testContext).getStatus().isNotEmpty())

        verify(mockAccount, never()).disconnect()
        manager.logout()
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.DISCONNECTED,
                FxaAuthState.DISCONNECTED,
            ),
        )

        assertTrue(SyncEnginesStorage(testContext).getStatus().isEmpty())
        verify(accountObserver, never()).onAuthenticated(any(), any())
        verify(accountObserver, never()).onProfileUpdated(any())
        verify(accountObserver, times(1)).onLoggedOut()
        verify(accountStorage, never()).read()
        verify(accountStorage, never()).write(any())
        verify(accountStorage, times(1)).clear()

        assertNull(manager.authenticatedAccount())
        assertNull(manager.accountProfile())
    }

    @Test
    fun `onReady is sent to the AccountObserver`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        manager.start()
        verify(manager.accountObserver, times(1)).onReady(null)
    }

    @Test
    fun `onReady has the account set if the account is already connected`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        whenever(manager.mockAccount.getAuthState()).thenReturn(FxaAuthState.CONNECTED)
        manager.start()
        verify(manager.accountObserver, times(1)).onReady(manager.mockAccount)
    }

    @Test
    fun `auth flow events are sent to the AccountObserver on failure`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        manager.start()

        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.OAUTH_FAILED_TO_BEGIN,
                FxaAuthState.DISCONNECTED,
            ),
        )
        verify(manager.accountObserver, times(1)).onFlowError(AuthFlowError.FailedToBeginAuth)

        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.OAUTH_STARTED,
                FxaAuthState.AUTHENTICATING,
            ),
        )
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.OAUTH_FAILED_TO_COMPLETE,
                FxaAuthState.DISCONNECTED,
            ),
        )
        verify(manager.accountObserver, times(1)).onFlowError(AuthFlowError.FailedToCompleteAuth)
    }

    @Test
    fun `auth flow events are sent to the AccountObserver on success`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        manager.start()
        assertEquals(
            manager.beginAuthentication(entrypoint = entryPoint),
            EXPECTED_OAUTH_URL,
        )
        verify(manager.mockAccount).queueAction(FxaAction.BeginOAuthFlow(arrayOf("scope1", "scope2"), "test-entrypoint", any()))
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.OAUTH_STARTED,
                FxaAuthState.AUTHENTICATING,
            ),
        )
        manager.finishAuthentication(FxaAuthData(AuthType.Signin, "test-code", EXPECTED_AUTH_STATE))
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.OAUTH_COMPLETE,
                FxaAuthState.CONNECTED,
            ),
        )

        verify(manager.accountObserver, times(1)).onAuthenticated(manager.mockAccount, AuthType.Signin)
    }

    @Test
    fun `onAuthenticated is sent on auth recovery`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )

        manager.start()
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.AUTH_CHECK_SUCCESS,
                FxaAuthState.CONNECTED,
            ),
        )

        verify(manager.accountObserver, times(1)).onAuthenticated(manager.mockAccount, AuthType.Recovered)
    }

    @Test
    fun `onAuthenticated is sent on startup when already authenticated`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )

        whenever(manager.mockAccount.getAuthState()).thenReturn(FxaAuthState.CONNECTED)
        manager.start()
        verify(manager.accountObserver, times(1)).onAuthenticated(manager.mockAccount, AuthType.Existing)
    }

    @Test
    fun `the profile is fetched on authentication complete`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        val profile = Profile(
            "testUid",
            "test@example.com",
            null,
            "Test Profile",
        )
        `when`(manager.mockAccount.getProfile(ignoreCache = false)).thenReturn(profile)

        manager.start()
        manager.beginAuthentication(entrypoint = entryPoint)
        manager.finishAuthentication(FxaAuthData(AuthType.Signin, "test-code", EXPECTED_AUTH_STATE))
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.OAUTH_COMPLETE,
                FxaAuthState.CONNECTED,
            ),
        )

        verify(manager.mockAccount, times(1)).getProfile(ignoreCache = false)
        verify(manager.accountObserver, times(1)).onProfileUpdated(profile)
    }

    @Test
    fun `the profile is refetched on auth recovery`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        val profile = Profile(
            "testUid",
            "test@example.com",
            null,
            "Test Profile",
        )
        `when`(manager.mockAccount.getProfile(ignoreCache = true)).thenReturn(profile)

        manager.start()
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.AUTH_CHECK_SUCCESS,
                FxaAuthState.CONNECTED,
            ),
        )

        verify(manager.mockAccount, times(1)).getProfile(ignoreCache = true)
        verify(manager.accountObserver, times(1)).onProfileUpdated(profile)
    }

    @Test
    fun `auth flow events are sent to the AccountObserver on authenication problems`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        manager.start()
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.AUTH_CHECK_FAILED,
                FxaAuthState.AUTH_ISSUES,
            ),
        )

        verify(manager.accountObserver, times(1)).onAuthenticationProblems()
    }

    @Test
    fun `auth flow events are sent to the AccountObserver on logout`() = runTest {
        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig(FxaServer.Release, "dummyId", "bad://url", null),
            coroutineContext,
        )
        manager.onFxaEvent(
            FxaEvent.AuthEvent(
                FxaAuthEventKind.DISCONNECTED,
                FxaAuthState.DISCONNECTED,
            ),
        )

        verify(manager.accountObserver, times(1)).onLoggedOut()
    }

    @Test
    fun `accounts to sync integration`() {
        val syncManager: SyncManager = mock()
        val integration = FxaAccountManager.AccountsToSyncIntegration(syncManager)

        // onAuthenticated - mapping of AuthType to SyncReason
        integration.onAuthenticated(mock(), AuthType.Signin)
        verify(syncManager, times(1)).start()
        verify(syncManager, times(1)).now(eq(SyncReason.FirstSync), anyBoolean(), eq(listOf()))
        integration.onAuthenticated(mock(), AuthType.Signup)
        verify(syncManager, times(2)).start()
        verify(syncManager, times(2)).now(eq(SyncReason.FirstSync), anyBoolean(), eq(listOf()))
        integration.onAuthenticated(mock(), AuthType.Pairing)
        verify(syncManager, times(3)).start()
        verify(syncManager, times(3)).now(eq(SyncReason.FirstSync), anyBoolean(), eq(listOf()))
        integration.onAuthenticated(mock(), AuthType.MigratedReuse)
        verify(syncManager, times(4)).start()
        verify(syncManager, times(4)).now(eq(SyncReason.FirstSync), anyBoolean(), eq(listOf()))
        integration.onAuthenticated(mock(), AuthType.OtherExternal("test"))
        verify(syncManager, times(5)).start()
        verify(syncManager, times(5)).now(eq(SyncReason.FirstSync), anyBoolean(), eq(listOf()))
        integration.onAuthenticated(mock(), AuthType.Existing)
        verify(syncManager, times(6)).start()
        verify(syncManager, times(1)).now(eq(SyncReason.Startup), anyBoolean(), eq(listOf()))
        integration.onAuthenticated(mock(), AuthType.Recovered)
        verify(syncManager, times(7)).start()
        verify(syncManager, times(2)).now(eq(SyncReason.Startup), anyBoolean(), eq(listOf()))
        verifyNoMoreInteractions(syncManager)

        // onProfileUpdated - no-op
        integration.onProfileUpdated(mock())
        verifyNoMoreInteractions(syncManager)

        // onAuthenticationProblems
        integration.onAuthenticationProblems()
        verify(syncManager).stop()
        verifyNoMoreInteractions(syncManager)

        // onLoggedOut
        integration.onLoggedOut()
        verify(syncManager, times(2)).stop()
        verifyNoMoreInteractions(syncManager)
    }

    @Test
    fun `GIVEN a sync observer WHEN registering it THEN add it to the sync observer registry`() = runTest {
        val fxaManager = TestableFxaAccountManager(
            context = testContext,
            config = mock(),
            coroutineContext,
            storage = mock(),
            capabilities = setOf(DeviceCapability.SEND_TAB),
            syncConfig = null,
        )
        fxaManager.syncStatusObserverRegistry = mock()
        val observer: SyncStatusObserver = mock()
        val lifecycleOwner: LifecycleOwner = mock()

        fxaManager.registerForSyncEvents(observer, lifecycleOwner, false)

        verify(fxaManager.syncStatusObserverRegistry).register(observer, lifecycleOwner, false)
        verifyNoMoreInteractions(fxaManager.syncStatusObserverRegistry)
    }

    @Test
    fun `GIVEN a sync observer WHEN unregistering it THEN remove it from the sync observer registry`() = runTest {
        val fxaManager = TestableFxaAccountManager(
            context = testContext,
            config = mock(),
            coroutineContext,
            storage = mock(),
            capabilities = setOf(DeviceCapability.SEND_TAB),
            syncConfig = null,
        )
        fxaManager.syncStatusObserverRegistry = mock()
        val observer: SyncStatusObserver = mock()

        fxaManager.unregisterForSyncEvents(observer)

        verify(fxaManager.syncStatusObserverRegistry).unregister(observer)
        verifyNoMoreInteractions(fxaManager.syncStatusObserverRegistry)
    }
}
