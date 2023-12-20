/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.manager

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import mozilla.appservices.fxaclient.FxaEvent
import mozilla.appservices.fxaclient.FxaException
import mozilla.appservices.fxaclient.FxaState
import mozilla.appservices.syncmanager.DeviceSettings
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.sync.AccountEventsObserver
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthFlowError
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.DeviceConfig
import mozilla.components.concept.sync.FxAEntryPoint
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.AccessTokenUnexpectedlyWithoutKey
import mozilla.components.service.fxa.AccountManagerException
import mozilla.components.service.fxa.AccountStorage
import mozilla.components.service.fxa.FxaAuthData
import mozilla.components.service.fxa.FxaDeviceSettingsCache
import mozilla.components.service.fxa.FxaSyncScopedKeyMissingException
import mozilla.components.service.fxa.SecureAbove22AccountStorage
import mozilla.components.service.fxa.ServerConfig
import mozilla.components.service.fxa.SharedPrefAccountStorage
import mozilla.components.service.fxa.StorageWrapper
import mozilla.components.service.fxa.SyncAuthInfoCache
import mozilla.components.service.fxa.SyncConfig
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.asSyncAuthInfo
import mozilla.components.service.fxa.emitSyncFailedFact
import mozilla.components.service.fxa.into
import mozilla.components.service.fxa.sync.SyncManager
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.service.fxa.sync.SyncStatusObserver
import mozilla.components.service.fxa.sync.WorkManagerSyncManager
import mozilla.components.service.fxa.sync.clearSyncState
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.support.base.utils.NamedThreadFactory
import java.io.Closeable
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import mozilla.appservices.fxaclient.DeviceConfig as ASDeviceConfig

// Necessary to fetch a profile.
const val SCOPE_PROFILE = "profile"

// Necessary to obtain sync keys.
const val SCOPE_SYNC = "https://identity.mozilla.com/apps/oldsync"

// Necessary to obtain a sessionToken, which gives full access to the account.
const val SCOPE_SESSION = "https://identity.mozilla.com/tokens/session"

// If we see more than AUTH_CHECK_CIRCUIT_BREAKER_COUNT checks, and each is less than
// AUTH_CHECK_CIRCUIT_BREAKER_RESET_MS since the last check, then we'll trigger a "circuit breaker".
const val AUTH_CHECK_CIRCUIT_BREAKER_RESET_MS = 1000L * 10
const val AUTH_CHECK_CIRCUIT_BREAKER_COUNT = 10
// This logic is in place to protect ourselves from endless auth recovery loops, while at the same
// time allowing for a few 401s to hit the state machine in a quick succession.
// For example, initializing the account state machine & syncing after letting our access tokens expire
// due to long period of inactivity will trigger a few 401s, and that shouldn't be a cause for concern.

const val MAX_NETWORK_RETRIES = 3

/**
 * An account manager which encapsulates various internal details of an account lifecycle and provides
 * an observer interface along with a public API for interacting with an account.
 * The internal state machine abstracts over state space as exposed by the fxaclient library, not
 * the internal states experienced by lower-level representation of a Firefox Account; those are opaque to us.
 *
 * Class is 'open' to facilitate testing.
 *
 * @param context A [Context] instance that's used for internal messaging and interacting with local storage.
 * @param serverConfig A [ServerConfig] used for account initialization.
 * @param deviceConfig A description of the current device (name, type, capabilities).
 * @param syncConfig Optional, initial sync behaviour configuration. Sync will be disabled if this is `null`.
 * @param applicationScopes A set of scopes which will be requested during account authentication.
 */
@Suppress("TooManyFunctions", "LargeClass")
open class FxaAccountManager(
    private val context: Context,
    @get:VisibleForTesting val serverConfig: ServerConfig,
    private val deviceConfig: DeviceConfig,
    private val syncConfig: SyncConfig?,
    private val applicationScopes: Set<String> = emptySet(),
    private val crashReporter: CrashReporting? = null,
    // We want a single-threaded execution model for our account-related "actions" (state machine side-effects).
    // That is, we want to ensure a sequential execution flow, but on a background thread.
    private val coroutineContext: CoroutineContext = Executors.newSingleThreadExecutor(
        NamedThreadFactory("FxaAccountManager"),
    ).asCoroutineDispatcher() + SupervisorJob(),
) : Closeable, Observable<AccountObserver> by ObserverRegistry() {
    private val logger = Logger("FirefoxAccountStateMachine")

    init {
        GlobalAccountManager.setInstance(this)
    }

    private val accountOnDisk by lazy { getStorageWrapper().account() }
    private val account by lazy { accountOnDisk.account() }

    // Note on threading: we use a single-threaded executor, so there's no concurrent access possible.
    // However, that executor doesn't guarantee that it'll always use the same thread, and so vars
    // are marked as volatile for across-thread visibility. Similarly, event queue uses a concurrent
    // list, although that's probably an overkill.
    @Volatile private var profile: Profile? = null

    // We'd like to persist this state, so that we can short-circuit transition to AuthenticationProblem on
    // initialization, instead of triggering the full state machine knowing in advance we'll hit auth problems.
    // See https://github.com/mozilla-mobile/android-components/issues/5102
    @Volatile private var state: State = State.Idle(AccountState.NotAuthenticated)

    @Volatile private var isAccountManagerReady: Boolean = false

    @VisibleForTesting
    val accountEventObserverRegistry = ObserverRegistry<AccountEventsObserver>()

    @VisibleForTesting
    open val syncStatusObserverRegistry = ObserverRegistry<SyncStatusObserver>()

    // We always obtain a "profile" scope, as that's assumed to be needed for any application integration.
    // We obtain a sync scope only if this was requested by the application via SyncConfig.
    // Additionally, we obtain any scopes that the application requested explicitly.
    private val scopes: Set<String>
        get() = if (syncConfig != null) {
            setOf(SCOPE_PROFILE, SCOPE_SYNC)
        } else {
            setOf(SCOPE_PROFILE)
        }.plus(applicationScopes)

    // Internal backing field for the syncManager. This will be `null` if passed in SyncConfig (either
    // via the constructor, or via [setSyncConfig]) is also `null` - that is, sync will be disabled.
    // Note that trying to perform a sync while account isn't authenticated will not succeed.
    @GuardedBy("this")
    private var syncManager: SyncManager? = null

    init {
        syncConfig?.let {
            // Initialize sync manager with the passed-in config.
            if (syncConfig.supportedEngines.isEmpty()) {
                throw IllegalArgumentException("Set of supported engines can't be empty")
            }

            syncManager = createSyncManager(syncConfig).also {
                // Observe account state changes.
                this.register(AccountsToSyncIntegration(it))

                // Observe sync status changes.
                it.registerSyncStatusObserver(SyncToAccountsIntegration(this))
            }
        }

        if (syncManager == null) {
            logger.info("Sync is disabled")
        } else {
            logger.info("Sync is enabled")
        }
    }

    /**
     * @return A list of currently supported [SyncEngine]s. `null` if sync isn't configured.
     */
    fun supportedSyncEngines(): Set<SyncEngine>? {
        // Notes on why this exists:
        // Parts of the system that make up an "fxa + sync" experience need to know which engines
        // are supported by an application. For example, FxA web content UI may present a "choose what
        // to sync" dialog during account sign-up, and application needs to be able to configure that
        // dialog. A list of supported engines comes to us from the application via passed-in SyncConfig.
        // Naturally, we could let the application configure any other part of the system that needs
        // to have access to supported engines. From the implementor's point of view, this is an extra
        // hurdle - instead of configuring only the account manager, they need to configure additional
        // classes. Additionally, we currently allow updating sync config "in-flight", not just at
        // the time of initialization. Providing an API for accessing currently configured engines
        // makes re-configuring SyncConfig less error-prone, as only one class needs to be told of the
        // new config.
        // Merits of allowing applications to re-configure SyncConfig after initialization are under
        // question, however: currently, we do not use that capability.
        return syncConfig?.supportedEngines
    }

    /**
     * Request an immediate synchronization, as configured according to [syncConfig].
     *
     * @param reason A [SyncReason] indicating why this sync is being requested.
     * @param debounce Boolean flag indicating if this sync may be debounced (in case another sync executed recently).
     * @param customEngineSubset A subset of supported engines to sync. Defaults to all supported engines.
     */
    suspend fun syncNow(
        reason: SyncReason,
        debounce: Boolean = false,
        customEngineSubset: List<SyncEngine> = listOf(),
    ) = withContext(coroutineContext) {
        check(
            customEngineSubset.isEmpty() ||
                syncConfig?.supportedEngines?.containsAll(customEngineSubset) == true,
        ) {
            "Custom engines for sync must be a subset of supported engines."
        }
        when (val s = state) {
            // Can't sync while we're still doing stuff.
            is State.Active -> Unit
            is State.Idle -> when (s.accountState) {
                // All good, request a sync.
                AccountState.Authenticated -> {
                    // Make sure auth cache is populated before we try to sync.
                    try {
                        maybeUpdateSyncAuthInfoCache()
                    } catch (e: AccessTokenUnexpectedlyWithoutKey) {
                        crashReporter?.submitCaughtException(
                            AccountManagerException.MissingKeyFromSyncScopedAccessToken("syncNow"),
                        )
                        processQueue(Event.Account.AccessTokenKeyError)
                        // No point in trying to sync now.
                        return@withContext
                    }

                    // Access to syncManager is guarded by `this`.
                    synchronized(this@FxaAccountManager) {
                        checkNotNull(syncManager == null) {
                            "Sync is not configured. Construct this class with a 'syncConfig' or use 'setSyncConfig'"
                        }
                        syncManager?.now(reason, debounce, customEngineSubset)
                    }
                }
                else -> logger.info("Ignoring syncNow request, not in the right state: $s")
            }
        }
    }

    /**
     * Indicates if sync is currently running.
     */
    fun isSyncActive() = syncManager?.isSyncActive() ?: false

    /**
     * Call this after registering your observers, and before interacting with this class.
     */
    suspend fun start() = withContext(coroutineContext) {
        processQueue(Event.Account.Start)

        if (!isAccountManagerReady) {
            notifyObservers { onReady(authenticatedAccount()) }
            isAccountManagerReady = true
        }
    }

    /**
     * Main point for interaction with an [OAuthAccount] instance.
     * @return [OAuthAccount] if we're in an authenticated state, null otherwise. Returned [OAuthAccount]
     * may need to be re-authenticated; consumers are expected to check [accountNeedsReauth].
     */
    fun authenticatedAccount(): OAuthAccount? = when (val s = state) {
        is State.Idle -> when (s.accountState) {
            AccountState.Authenticated,
            AccountState.AuthenticationProblem,
            -> account
            else -> null
        }
        else -> null
    }

    /**
     * Indicates if account needs to be re-authenticated via [beginAuthentication].
     * Most common reason for an account to need re-authentication is a password change.
     *
     * TODO this may return a false-positive, if we're currently going through a recovery flow.
     * Prefer to be notified of auth problems via [AccountObserver], which is reliable.
     *
     * @return A boolean flag indicating if account needs to be re-authenticated.
     */
    fun accountNeedsReauth() = when (val s = state) {
        is State.Idle -> when (s.accountState) {
            AccountState.AuthenticationProblem -> true
            else -> false
        }
        else -> false
    }

    /**
     * Returns a [Profile] for an account, attempting to fetch it if necessary.
     * @return [Profile] if one is available and account is an authenticated state.
     */
    fun accountProfile(): Profile? = when (val s = state) {
        is State.Idle -> when (s.accountState) {
            AccountState.Authenticated,
            AccountState.AuthenticationProblem,
            -> profile
            else -> null
        }
        else -> null
    }

    @VisibleForTesting
    internal suspend fun refreshProfile(ignoreCache: Boolean): Profile? {
        return authenticatedAccount()?.getProfile(ignoreCache = ignoreCache)?.let { newProfile ->
            profile = newProfile
            notifyObservers {
                onProfileUpdated(newProfile)
            }
            profile
        }
    }

    /**
     * Begins an authentication process. Should be finalized by calling [finishAuthentication] once
     * user successfully goes through the authentication at the returned url.
     * @param pairingUrl Optional pairing URL in case a pairing flow is being initiated.
     * @param entrypoint an enum representing the feature entrypoint requesting the URL.
     * the entrypoint is used in telemetry.
     * @return An authentication url which is to be presented to the user.
     */
    suspend fun beginAuthentication(
        pairingUrl: String? = null,
        entrypoint: FxAEntryPoint,
    ): String? = withContext(coroutineContext) {
        val event = if (pairingUrl != null) {
            Event.Account.BeginPairingFlow(pairingUrl, entrypoint)
        } else {
            Event.Account.BeginEmailFlow(entrypoint)
        }

        // Process the event, then use the new state to check the result of the operation
        processQueue(event)
        when (val state = state) {
            is State.Idle -> (state.accountState as? AccountState.Authenticating)?.oAuthUrl
            else -> null
        }.also { result ->
            if (result == null) {
                logger.warn("beginAuthentication: error processing next state ($state)")
            }
        }
    }

    /**
     * Finalize authentication that was started via [beginAuthentication].
     *
     * If authentication wasn't started via this manager we won't accept this authentication attempt,
     * returning `false`. This may happen if [WebChannelFeature] is enabled, and user is manually
     * logging into accounts.firefox.com in a regular tab.
     *
     * Guiding principle behind this is that logging into accounts.firefox.com should not affect
     * logged-in state of the browser itself, even though the two may have an established communication
     * channel via [WebChannelFeature].
     */
    suspend fun finishAuthentication(authData: FxaAuthData) = withContext(coroutineContext) {
        authData.declinedEngines?.let { persistDeclinedEngines(it) }
        processQueue(Event.Progress.AuthData(authData))
        true
    }

    /**
     * Logout of the account, if currently logged-in.
     */
    suspend fun logout() = withContext(coroutineContext) { processQueue(Event.Account.Logout) }

    /**
     * Register a [AccountEventsObserver] to monitor events relevant to an account/device.
     */
    fun registerForAccountEvents(observer: AccountEventsObserver, owner: LifecycleOwner, autoPause: Boolean) {
        accountEventObserverRegistry.register(observer, owner, autoPause)
    }

    /**
     * Register a [SyncStatusObserver] to monitor sync activity performed by this manager.
     */
    fun registerForSyncEvents(observer: SyncStatusObserver, owner: LifecycleOwner, autoPause: Boolean) {
        syncStatusObserverRegistry.register(observer, owner, autoPause)
    }

    /**
     * Unregister a [SyncStatusObserver] from being informed about "sync lifecycle" events.
     * The method is safe to call even if the provided observer was not registered before.
     */
    fun unregisterForSyncEvents(observer: SyncStatusObserver) {
        syncStatusObserverRegistry.unregister(observer)
    }

    override fun close() {
        GlobalAccountManager.close()
        coroutineContext.cancel()
        account.close()
    }

    internal suspend fun encounteredAuthError(
        operation: String,
        errorCountWithinTheTimeWindow: Int = 1,
    ) {
        return withContext(coroutineContext) {
            processQueue(
                Event.Account.AuthenticationError(operation, errorCountWithinTheTimeWindow),
            )
        }
    }

    /**
     * Pumps the state machine until all events are processed and their side-effects resolve.
     */
    private suspend fun processQueue(event: Event) {
        var fxaEvent = calcFxaEvent(event)
        if (fxaEvent == null) {
            logger.warn("processQueue: Got invalid event '$event'")
            return
        }

        val newFxaState = try {
            account.processEvent(fxaEvent)
        } catch (e: FxaException) {
            logger.warn("processQueue: Error in processEvent: $e")
            return
        }

        try {
            accountStateSideEffects(newFxaState, event)
        } catch (_: AccountManagerException.AuthenticationSideEffectsFailed) {
            account.processEvent(FxaEvent.Disconnect)
            accountStateSideEffects(FxaState.Disconnected, Event.Account.Logout)
        }
    }

    private fun calcFxaEvent(event: Event): FxaEvent? = when (event) {
        Event.Account.Start -> FxaEvent.Initialize(
            ASDeviceConfig(
                name = deviceConfig.name,
                deviceType = deviceConfig.type.into(),
                capabilities = ArrayList(deviceConfig.capabilities.map { it.into() }),
            ),
        )
        is Event.Account.BeginEmailFlow -> FxaEvent.BeginOAuthFlow(ArrayList(scopes), event.entrypoint.entryName)
        is Event.Account.BeginPairingFlow -> {
            if (event.pairingUrl != null) {
                FxaEvent.BeginPairingFlow(event.pairingUrl, ArrayList(scopes), event.entrypoint.entryName)
            } else {
                crashReporter?.recordCrashBreadcrumb(Breadcrumb("event.pairingUrl is null"))
                null
            }
        }
        is Event.Account.AuthenticationError -> FxaEvent.CheckAuthorizationStatus
        Event.Account.AccessTokenKeyError -> FxaEvent.CheckAuthorizationStatus
        Event.Account.Logout -> FxaEvent.Disconnect
        // This is the one ProgressEvent that's considered a "public event" in app-services
        is Event.Progress.AuthData -> FxaEvent.CompleteOAuthFlow(event.authData.code, event.authData.state)
        else -> null
    }

    /**
     * Side-effects of entering a new FxaState
     *
     * Upon entering these states, observers are typically notified. The sole exception occurs
     * during the completion of authentication, where it is necessary to populate the
     * SyncAuthInfoCache for the background synchronization worker.
     *
     * @throws [AccountManagerException.AuthenticationSideEffectsFailed] if there was a failure to
     * run the side effects for a newly authenticated account.
     */
    private suspend fun accountStateSideEffects(
        forState: FxaState,
        via: Event,
    ) {
        when (forState) {
            FxaState.Disconnected -> when (via) {
                Event.Account.Logout -> {
                    resetAccount()
                    notifyObservers { onLoggedOut() }
                }
                is Event.Account.BeginEmailFlow, is Event.Account.BeginPairingFlow -> {
                    resetAccount()
                    notifyObservers { onFlowError(AuthFlowError.FailedToBeginAuth) }
                }
                is Event.Progress.AuthData -> {
                    resetAccount()
                    notifyObservers { onFlowError(AuthFlowError.FailedToCompleteAuth) }
                }
                else -> Unit
            }
            FxaState.Connected -> when (via) {
                is Event.Account.Start -> {
                    if (authenticationSideEffects("CompletingAuthentication:accountRestored")) {
                        notifyObservers { onAuthenticated(account, AuthType.Existing) }
                        refreshProfile(ignoreCache = false)
                    } else {
                        throw AccountManagerException.AuthenticationSideEffectsFailed()
                    }
                }
                is Event.Progress.AuthData -> {
                    if (authenticationSideEffects("CompletingAuthentication:AuthData")) {
                        notifyObservers { onAuthenticated(account, via.authData.authType) }
                        refreshProfile(ignoreCache = false)
                    } else {
                        throw AccountManagerException.AuthenticationSideEffectsFailed()
                    }
                }
                is Event.Account.AuthenticationError, Event.Account.AccessTokenKeyError -> {
                    // Clear our access token cache; it'll be re-populated as part of the
                    // regular state machine flow.
                    SyncAuthInfoCache(context).clear()
                    // Should we also call authenticationSideEffects here?
                    // (https://bugzilla.mozilla.org/show_bug.cgi?id=1865086)
                    notifyObservers { onAuthenticated(account, AuthType.Recovered) }
                    refreshProfile(ignoreCache = true)
                }
                else -> Unit
            }
            FxaState.AuthIssues -> {
                SyncAuthInfoCache(context).clear()
                notifyObservers { onAuthenticationProblems() }
            }
            else -> Unit
        }
    }

    private suspend fun resetAccount() {
        // Clean up resources.
        profile = null
        // Delete persisted state.
        getAccountStorage().clear()
        // Even though we might not have Sync enabled, clear out sync-related storage
        // layers as well; if they're already empty (unused), nothing bad will happen
        // and extra overhead is quite small.
        SyncAuthInfoCache(context).clear()
        SyncEnginesStorage(context).clear()
        clearSyncState(context)
    }

    private suspend fun maybeUpdateSyncAuthInfoCache() {
        // Update cached sync auth info only if we have a syncConfig (e.g. sync is enabled)...
        if (syncConfig == null) {
            return
        }

        // .. and our cache is stale.
        val cache = SyncAuthInfoCache(context)
        if (!cache.expired()) {
            return
        }

        val accessToken = try {
            account.getAccessToken(SCOPE_SYNC)
        } catch (e: FxaSyncScopedKeyMissingException) {
            // We received an access token, but no sync key which means we can't really use the
            // connected FxA account.  Throw an exception so that the account transitions to the
            // `AuthenticationProblem` state.  Things should be fixed when the user re-logs in.
            //
            // This used to be thrown when the android-components code noticed the issue in
            // `asSyncAuthInfo()`.  However, the application-services code now also checks for this
            // and throws its own error.  To keep the flow above this the same, we catch the
            // app-services exception and throw the android-components one.
            //
            // Eventually, we should remove AccessTokenUnexpectedlyWithoutKey and have the higher
            // functions catch `FxaSyncScopedKeyMissingException` directly
            // (https://bugzilla.mozilla.org/show_bug.cgi?id=1869862)
            throw AccessTokenUnexpectedlyWithoutKey()
        }
        val tokenServerUrl = if (accessToken != null) {
            // Only try to get the endpoint if we have an access token.
            account.getTokenServerEndpointURL()
        } else {
            null
        }

        if (accessToken != null && tokenServerUrl != null) {
            SyncAuthInfoCache(context).setToCache(accessToken.asSyncAuthInfo(tokenServerUrl))
        } else {
            // At this point, SyncAuthInfoCache may be entirely empty. In this case, we won't be
            // able to sync via the background worker. We will attempt to populate SyncAuthInfoCache
            // again in `syncNow` (in response to a direct user action) and after application restarts.
            logger.warn("Couldn't populate SyncAuthInfoCache. Sync may not work.")
            logger.warn("Is null? - accessToken: ${accessToken == null}, tokenServerUrl: ${tokenServerUrl == null}")
        }
    }

    private fun persistDeclinedEngines(declinedEngines: Set<SyncEngine>) {
        // Sync may not be configured at all (e.g. won't run), but if we received a
        // list of declined engines, that indicates user intent, so we preserve it
        // within SyncEnginesStorage regardless. If sync becomes enabled later on,
        // we will be respecting user choice around what to sync.
        val enginesStorage = SyncEnginesStorage(context)
        declinedEngines.forEach { declinedEngine ->
            enginesStorage.setStatus(declinedEngine, false)
        }

        // Enable all engines that were not explicitly disabled. Only do this in
        // the presence of a "declinedEngines" list, since that indicates user
        // intent. Absence of that list means that user was not presented with a
        // choice during authentication, and so we can't assume an enabled state
        // for any of the engines.
        syncConfig?.supportedEngines?.forEach { supportedEngine ->
            if (!declinedEngines.contains(supportedEngine)) {
                enginesStorage.setStatus(supportedEngine, true)
            }
        }
    }

    private suspend fun finalizeDevice(authType: AuthType) = account.deviceConstellation().finalizeDevice(
        authType,
        deviceConfig,
    )

    /**
     * Populates caches necessary for the sync worker (sync auth info and FxA device).
     * @return 'true' on success, 'false' on failure, indicating that sync won't work.
     */
    private suspend fun authenticationSideEffects(operation: String): Boolean {
        // Make sure our SyncAuthInfo cache is hot, background sync worker needs it to function.
        try {
            maybeUpdateSyncAuthInfoCache()
        } catch (e: AccessTokenUnexpectedlyWithoutKey) {
            crashReporter?.submitCaughtException(
                AccountManagerException.MissingKeyFromSyncScopedAccessToken(operation),
            )
            // Since we don't know what's causing a missing key for the SCOPE_SYNC access tokens, we
            // do not attempt to recover here. If this is a persistent state for an account, a recovery
            // will just enter into a loop that our circuit breaker logic is unlikely to catch, due
            // to recovery attempts likely being made on startup.
            // See https://github.com/mozilla-mobile/android-components/issues/8527
            return false
        }

        // Sync workers also need to know about the current FxA device.
        FxaDeviceSettingsCache(context).setToCache(
            DeviceSettings(
                fxaDeviceId = account.getCurrentDeviceId()!!,
                name = deviceConfig.name,
                kind = deviceConfig.type.into(),
            ),
        )
        return true
    }

    /**
     * Exists strictly for testing purposes, allowing tests to specify their own implementation of [OAuthAccount].
     */
    @VisibleForTesting
    open fun getStorageWrapper(): StorageWrapper {
        return StorageWrapper(this, accountEventObserverRegistry, serverConfig, crashReporter)
    }

    @VisibleForTesting
    internal open fun createSyncManager(config: SyncConfig): SyncManager {
        return WorkManagerSyncManager(context, config)
    }

    internal open fun getAccountStorage(): AccountStorage {
        return if (deviceConfig.secureStateAtRest) {
            SecureAbove22AccountStorage(context, crashReporter)
        } else {
            SharedPrefAccountStorage(context, crashReporter)
        }
    }

    /**
     * Account status events flowing into the sync manager.
     */
    @VisibleForTesting
    internal class AccountsToSyncIntegration(
        private val syncManager: SyncManager,
    ) : AccountObserver {
        override fun onLoggedOut() {
            syncManager.stop()
        }

        override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
            val reason = when (authType) {
                is AuthType.OtherExternal, AuthType.Signin, AuthType.Signup, AuthType.MigratedReuse,
                AuthType.MigratedCopy, AuthType.Pairing,
                -> SyncReason.FirstSync
                AuthType.Existing, AuthType.Recovered -> SyncReason.Startup
            }
            syncManager.start()
            syncManager.now(reason)
        }

        override fun onProfileUpdated(profile: Profile) {
            // Sync currently doesn't care about the FxA profile.
            // In the future, we might kick-off an immediate sync here.
        }

        override fun onAuthenticationProblems() {
            emitSyncFailedFact()
            syncManager.stop()
        }
    }

    /**
     * Sync status changes flowing into account manager.
     */
    private class SyncToAccountsIntegration(
        private val accountManager: FxaAccountManager,
    ) : SyncStatusObserver {
        override fun onStarted() {
            accountManager.syncStatusObserverRegistry.notifyObservers { onStarted() }
        }

        override fun onIdle() {
            accountManager.syncStatusObserverRegistry.notifyObservers { onIdle() }
        }

        override fun onError(error: Exception?) {
            accountManager.syncStatusObserverRegistry.notifyObservers { onError(error) }
        }
    }

    /**
     * Hook this up to the secret debug menu to simulate a network error
     *
     * Typical usage is:
     *   - `adb logcat | grep fxa_client`
     *   - Trigger this via the secret debug menu item.
     *   - Watch the logs.  You should see the client perform a call to `get_profile', see a
     *     network error, then recover.
     *   - Check the UI, it should be in an authenticated state.
     */
    public fun simulateNetworkError() {
        account.simulateNetworkError()
        account.processEvent(FxaEvent.CallGetProfile)
    }

    /**
     * Hook this up to the secret debug menu to simulate a temporary auth error
     *
     * Typical usage is:
     *   - `adb logcat | grep fxa_client`
     *   - Trigger this via the secret debug menu item.
     *   - Watch the logs.  You should see the client perform a call to `get_profile', see an
     *     auth error, then recover.
     *   - Check the UI, it should be in an authenticated state.
     */
    public fun simulateTemporaryAuthTokenIssue() {
        account.simulateTemporaryAuthTokenIssue()
        SyncAuthInfoCache(context).clear()
        account.processEvent(FxaEvent.CallGetProfile)
    }

    /**
     * Hook this up to the secret debug menu to simulate an unrecoverable auth error
     *
     * Typical usage is:
     *   - `adb logcat | grep fxa_client`
     *   - Trigger this via the secret debug menu item.
     *   - Initiaite a sync, or perform some other action that requires authentication.
     *   - Watch the logs.  You should see the client perform a call to `get_profile', see an
     *     auth error, then fail to recover.
     *   - Check the UI, it should be in an authentication problems state.
     */
    public fun simulatePermanentAuthTokenIssue() {
        account.simulatePermanentAuthTokenIssue()
        SyncAuthInfoCache(context).clear()
        account.processEvent(FxaEvent.CallGetProfile)
    }
}
