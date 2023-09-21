/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.manager

import android.content.Context
import android.net.Uri
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.fxaclient.FxaAction
import mozilla.appservices.fxaclient.FxaAuthEventKind
import mozilla.appservices.fxaclient.FxaAuthState
import mozilla.appservices.fxaclient.FxaEvent
import mozilla.appservices.fxaclient.FxaEventHandler
import mozilla.appservices.syncmanager.DeviceSettings
import mozilla.appservices.syncmanager.SyncTelemetry
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.sync.AccountEventsObserver
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthFlowError
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.DeviceConfig
import mozilla.components.concept.sync.FxAEntryPoint
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.AccountStorage
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.FxaAuthData
import mozilla.components.service.fxa.FxaDeviceSettingsCache
import mozilla.components.service.fxa.SecureAbove22AccountStorage
import mozilla.components.service.fxa.ServerConfig
import mozilla.components.service.fxa.SharedPrefAccountStorage
import mozilla.components.service.fxa.StorageWrapper
import mozilla.components.service.fxa.SyncAuthInfoCache
import mozilla.components.service.fxa.SyncConfig
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.asSyncAuthInfo
import mozilla.components.service.fxa.emitSyncFailedFact
import mozilla.components.service.fxa.sync.SyncManager
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.service.fxa.sync.SyncStatusObserver
import mozilla.components.service.fxa.sync.WorkManagerSyncManager
import mozilla.components.service.fxa.sync.clearSyncState
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import java.io.Closeable

// Necessary to fetch a profile.
const val SCOPE_PROFILE = "profile"

// Necessary to obtain sync keys.
const val SCOPE_SYNC = "https://identity.mozilla.com/apps/oldsync"

// Necessary to obtain a sessionToken, which gives full access to the account.
const val SCOPE_SESSION = "https://identity.mozilla.com/tokens/session"

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
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
open class FxaAccountManager(
    private val context: Context,
    @get:VisibleForTesting val serverConfig: ServerConfig,
    private val deviceConfig: DeviceConfig,
    private val syncConfig: SyncConfig?,
    private val applicationScopes: Set<String> = emptySet(),
    private val crashReporter: CrashReporting? = null,
) : Closeable, FxaEventHandler, Observable<AccountObserver> by ObserverRegistry() {
    private val logger = Logger("FirefoxAccountStateMachine")

    @Volatile
    private var authFlowState: AuthFlowState = AuthFlowState.NotStarted

    private sealed class AuthFlowState {
        object NotStarted : AuthFlowState()
        data class InProgress(val oAuthState: String) : AuthFlowState()
        data class Completing(val authType: AuthType) : AuthFlowState()
    }

    init {
        GlobalAccountManager.setInstance(this)
    }

    private val accountOnDisk by lazy { getStorageWrapper().account() }
    private val account: FirefoxAccount by lazy { accountOnDisk.account() }

    // Note on threading: we use a single-threaded executor, so there's no concurrent access possible.
    // However, that executor doesn't guarantee that it'll always use the same thread, and so vars
    // are marked as volatile for across-thread visibility. Similarly, event queue uses a concurrent
    // list, although that's probably an overkill.
    @Volatile private var profile: Profile? = null

    @Volatile private var state: FxaAuthState = FxaAuthState.DISCONNECTED

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
    ) = withContext(account.clientCoroutineContext) {
        check(
            customEngineSubset.isEmpty() ||
                syncConfig?.supportedEngines?.containsAll(customEngineSubset) == true,
        ) {
            "Custom engines for sync must be a subset of supported engines."
        }
        val state = state
        if (state.isConnected()) {
            // Try to populate the auth cache before we try to sync.
            maybeUpdateSyncAuthInfoCache()

            // Access to syncManager is guarded by `this`.
            synchronized(this@FxaAccountManager) {
                checkNotNull(syncManager == null) {
                    "Sync is not configured. Construct this class with a 'syncConfig' or use 'setSyncConfig'"
                }
                syncManager?.now(reason, debounce, customEngineSubset)
            }
        } else {
            logger.info("Ignoring syncNow request, not in the right state: $state")
        }
    }

    /**
     * Indicates if sync is currently running.
     */
    fun isSyncActive() = syncManager?.isSyncActive() ?: false

    /**
     * Call this after registering your observers, and before interacting with this class.
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        // Note: we can't use account.clientCoroutineContext to wrap this, since this is the
        // function that causes the account to be lazily created.

        if (!isAccountManagerReady) {
            account.registerEventHandler(this@FxaAccountManager)
            state = account.getAuthState()
            notifyObservers { onReady(authenticatedAccount()) }
            if (state.isConnected()) {
                setupAuthenticatedAccount(AuthType.Existing)
            }
            isAccountManagerReady = true
        }
    }

    /**
     * Main point for interaction with an [OAuthAccount] instance.
     * @return [OAuthAccount] if we're in are connected or needs reauthentication, null otherwise.
     * Returned [OAuthAccount] may need to be re-authenticated; consumers are expected to check
     * [accountNeedsReauth].
     */
    fun authenticatedAccount(): OAuthAccount? {
        val state = state
        if (state in listOf(FxaAuthState.CONNECTED, FxaAuthState.CHECKING_AUTH, FxaAuthState.AUTH_ISSUES)) {
            return account
        } else {
            return null
        }
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
    fun accountNeedsReauth() = (state == FxaAuthState.AUTH_ISSUES)

    /**
     * Returns a [Profile] for an account, attempting to fetch it if necessary.
     * @return [Profile] if one is available and account is an authenticated state.
     */
    fun accountProfile(): Profile? {
        if (state in listOf(FxaAuthState.CONNECTED, FxaAuthState.CHECKING_AUTH, FxaAuthState.AUTH_ISSUES)) {
            return profile
        } else {
            return null
        }
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
    ): String? {
        val result = CompletableDeferred<String?>()
        val action = if (pairingUrl != null) {
            FxaAction.BeginPairingFlow(pairingUrl, scopes.toTypedArray(), entrypoint.entryName, result)
        } else {
            FxaAction.BeginOAuthFlow(scopes.toTypedArray(), entrypoint.entryName, result)
        }
        account.queueAction(action)
        val url = result.await() ?: return null
        val state = Uri.parse(url).getQueryParameter("state")
        if (state != null) {
            authFlowState = AuthFlowState.InProgress(state)
            return url
        } else {
            logger.error("OAuth URL missing state param")
            return null
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
     *
     * @return A deferred boolean flag indicating if authentication state was accepted.
     */
    suspend fun finishAuthentication(authData: FxaAuthData): Boolean {
        val authFlowState = authFlowState
        return when (authFlowState) {
            is AuthFlowState.InProgress -> {
                if (authFlowState.oAuthState == authData.state) {
                    // Happy path: the incoming authData matches the authFlowState set in
                    // beginAuthentication
                    authData.declinedEngines?.let { persistDeclinedEngines(it) }
                    this.authFlowState = AuthFlowState.Completing(authData.authType)
                    account.queueAction(FxaAction.CompleteOAuthFlow(authData.code, authData.state))
                    true
                } else {
                    logger.warn("finishAuthentication: ignoring OAuth state mismatch")
                    false
                }
            }
            else -> {
                logger.warn("finishAuthentication: invalid state $authFlowState")
                false
            }
        }
    }

    /**
     * Handle Fxa events
     *
     * This is our chance to respond to the results of actions sent to [FxaClient.queueAction]
     */
    override suspend fun onFxaEvent(event: FxaEvent) {
        when (event) {
            is FxaEvent.DeviceOperationComplete -> {
                FxaDeviceSettingsCache(context).setToCache(
                    DeviceSettings(
                        fxaDeviceId = event.localDevice.id,
                        name = event.localDevice.displayName,
                        kind = event.localDevice.deviceType,
                    ),
                )
                account.deviceConstellation().refreshDevices()
                gatherTelemetry()
            }
            is FxaEvent.DeviceOperationFailed -> {
                logger.warn("FxA device operation failure: ${event.operation}")
            }
            is FxaEvent.AuthEvent -> {
                state = event.state
                when (event.kind) {
                    FxaAuthEventKind.OAUTH_COMPLETE -> completeOauth()
                    FxaAuthEventKind.OAUTH_FAILED_TO_BEGIN -> {
                        notifyObservers { onFlowError(AuthFlowError.FailedToBeginAuth) }
                    }
                    FxaAuthEventKind.OAUTH_FAILED_TO_COMPLETE -> {
                        notifyObservers { onFlowError(AuthFlowError.FailedToCompleteAuth) }
                    }
                    FxaAuthEventKind.DISCONNECTED -> {
                        resetAccount()
                        notifyObservers { onLoggedOut() }
                    }
                    FxaAuthEventKind.AUTH_CHECK_STARTED -> {
                        SyncAuthInfoCache(context).clear()
                    }
                    FxaAuthEventKind.AUTH_CHECK_SUCCESS -> {
                        setupAuthenticatedAccount(AuthType.Recovered)
                    }
                    FxaAuthEventKind.AUTH_CHECK_FAILED,
                    FxaAuthEventKind.LOGOUT_FROM_AUTH_ISSUES,
                    -> {
                        notifyObservers { onAuthenticationProblems() }
                    }
                    else -> Unit
                }
            }
            else -> Unit
        }
    }

    private suspend fun completeOauth() {
        val authFlowState = authFlowState
        when (authFlowState) {
            is AuthFlowState.Completing -> {
                setupAuthenticatedAccount(authFlowState.authType)
                this.authFlowState = AuthFlowState.NotStarted
            }
            else -> {
                logger.warn("completeOauth: invalid state $authFlowState")
            }
        }
    }

    internal suspend fun encounteredAuthError(operation: String) {
        logger.warn("encounteredAuthError: $operation")
        account.queueAction(FxaAction.CheckAuthorization)
    }

    /**
     * Logout of the account, if currently logged-in.
     */
    suspend fun logout() = account.queueAction(FxaAction.Disconnect)

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
        account.close()
    }

    private suspend fun gatherTelemetry() {
        val errors: List<Throwable> = SyncTelemetry.processFxaTelemetry(account.gatherTelemetry())
        for (error in errors) {
            crashReporter?.submitCaughtException(error)
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

        val accessToken = account.getAccessToken(SCOPE_SYNC)
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

    // Setup a newly authenticated account.
    private suspend fun setupAuthenticatedAccount(authType: AuthType) {
        // Try to sure our SyncAuthInfo cache is hot, background sync worker needs it to function.
        maybeUpdateSyncAuthInfoCache()
        // Update our device record, which should result in a DeviceOperationComplete event coming
        // back, which we will use to populate FxaDeviceSettingsCache.
        account.deviceConstellation().finalizeDevice(authType, deviceConfig)
        // Fetch new profile data if we need to
        when (authType) {
            AuthType.Recovered -> refreshProfile(ignoreCache = true)
            else -> refreshProfile(ignoreCache = false)
        }
        // and finally notify all observers
        notifyObservers { onAuthenticated(account, authType) }
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
     *   - Trigger this
     *   - Try to change your device name, or perform some other action that hits the network
     *   - Watch the logs.  You should see the client see a network error then recover.
     */
    public fun simulateNetworkError() {
        account.simulateNetworkError()
    }

    /**
     * Hook this up to the secret debug menu to simulate a temporary auth error
     *
     * Typical usage is:
     *   - `adb logcat | grep fxa_client`
     *   - Trigger this.
     *   - There's not a great way to manually test this via user interaction, so this function also
     *     initiates a profile fetch.
     *   - Watch the logs, you should see the client fail because of an invalid auth token, get a
     *     new auth token, then recover.
     */
    public fun simulateTemporaryAuthTokenIssue() {
        account.simulateTemporaryAuthTokenIssue()
        SyncAuthInfoCache(context).clear()
        // There doesn't seem to be a good way to test this in the UI, so force a profile refresh to
        // kick things off
        CoroutineScope(account.clientCoroutineContext).launch {
            refreshProfile(ignoreCache = true)
        }
    }

    /**
     * Hook this up to the secret debug menu to simulate an unrecoverable auth error
     *
     * Typical usage is:
     *   - `adb logcat | grep fxa_client`
     *   - Trigger this
     *   - Try to change your device name, or perform some other action that hits the network
     *   - Watch the logs.  You should see the client see an auth error, fail to recover, then log
     *     the user out.
     *   - Check the UI, it should be in an authentication problems state.
     */
    public fun simulatePermanentAuthTokenIssue() {
        account.simulatePermanentAuthTokenIssue()
        SyncAuthInfoCache(context).clear()
    }
}
