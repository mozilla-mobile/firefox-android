/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import mozilla.appservices.fxaclient.FxaAction
import mozilla.appservices.fxaclient.FxaClient
import mozilla.appservices.fxaclient.FxaEvent
import mozilla.appservices.fxaclient.FxaEventHandler
import mozilla.appservices.fxaclient.FxaPersistCallback
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.components.concept.sync.AuthFlowUrl
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.FxAEntryPoint
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.StatePersistenceCallback
import mozilla.components.support.base.log.logger.Logger

/**
 * Adapts the app-services FxaClient class to work with firefox-android
 *
 * This class has two responsibilities:
 *
 * It implements the OAuthAccount interface which is mostly only used by the sample apps at this
 * point. Newer code uses [FxaAccountManager] for FxA functionality.
 *
 * It lightly wraps FxaClient interface for [FxaAccountManager].  The main thing it does here is
 * work around lifecycle issues that require [FirefoxAccount] to be created first, then have its
 * [FxaEventHandler] registered later.
 */
@Suppress("TooManyFunctions")
class FirefoxAccount internal constructor(
    createFxaClient: (FxaPersistCallback, FxaEventHandler) -> FxaClient,
    crashReporter: CrashReporting? = null,
) : OAuthAccount {
    private var wrappedPersistenceCallback = WrappedPersistenceCallback()
    private var wrappedEventHandler = WrappedFxaEventHandler()
    private val inner = createFxaClient(wrappedPersistenceCallback, wrappedEventHandler)
    private val logger = Logger("FirefoxAccount")
    private val deviceConstellation = FxaDeviceConstellation(inner, inner.coroutineContext, crashReporter)

    // == Construction, Destruction, Serialization, and Registration ==
    //
    //
    // Event handler registration:
    //
    // - [OAuthAccount] consumers call [registerPersistenceCallback]
    // - [FxaAccountManager] calls [registerEventHandler].  The passed [FxaEventHandler] manages
    //   persistance, as well as state change notifications.
    // - Do not call both of these methods.
    //
    // Note: we create [FirefoxAccount] before we have an event handler or persistence callback
    // ready. Code that uses FxaEventHandler must call one of those registration methods before
    // using the [FirefoxAccount].

    /**
     * Construct a FirefoxAccount
     *
     * @param config Fxa server configuration
     * @param crashReporter A crash reporter instance.
     */
    constructor(
        config: ServerConfig,
        crashReporter: CrashReporting? = null,
    ) : this(
        { persistCallback, eventHandler -> FxaClient(config, persistCallback, eventHandler, Dispatchers.IO) },
        crashReporter,
    )

    /**
     * Register an FxaEventHandler
     *
     * This is called by [FxaAccountManager] to register its event handler.
     */
    fun registerEventHandler(eventHandler: FxaEventHandler) {
        wrappedEventHandler.setEventHandler(eventHandler)
    }

    /**
     * Register an StatePersistenceCallback
     *
     * This is called by OAuthAccount consumers to register the persistence callback.
     */
    override fun registerPersistenceCallback(callback: StatePersistenceCallback) {
        wrappedPersistenceCallback.setCallback(callback)
    }

    override fun toJSONString(): String = inner.toJsonString()

    override fun close() {
        inner.close()
    }

    companion object {
        /**
         * Restores the account's authentication state from a JSON string produced by
         * [FirefoxAccount.toJSONString].
         *
         * @param crashReporter object used for logging caught exceptions
         *
         * @return [FirefoxAccount] representing the authentication state
         */
        fun fromJSONString(
            json: String,
            crashReporter: CrashReporting?,
        ): FirefoxAccount {
            return FirefoxAccount(
                { persistCallback, eventHandler ->
                    FxaClient.fromJson(
                        json,
                        persistCallback,
                        eventHandler,
                        Dispatchers.IO,
                    )
                },
                crashReporter,
            )
        }
    }

    // == OAuthAccount OAuth implementation ==
    //
    // Code below here implements OAuthAccount functionality using the OAuthAccount interface.
    // Newer code should use [FxaAccountManager] for this instead.
    //
    // Do not mix calls to these methods and the [FxaAccountManager] OAuth methods.

    override suspend fun beginOAuthFlow(
        scopes: Set<String>,
        entryPoint: FxAEntryPoint,
    ) = handleFxaExceptions(logger, "begin oauth flow") {
        @Suppress("DEPRECATION")
        val url = inner.beginOAuthFlow(scopes.toTypedArray(), entryPoint.entryName)
        val state = Uri.parse(url).getQueryParameter("state")!!
        AuthFlowUrl(state, url)
    }

    override suspend fun beginPairingFlow(
        pairingUrl: String,
        scopes: Set<String>,
        entryPoint: FxAEntryPoint,
    ) = handleFxaExceptions(logger, "begin oauth pairing flow") {
        // Eventually we should specify this as a param here, but for now, let's
        // use a generic value (it's used only for server-side telemetry, so the
        // actual value doesn't matter much)
        @Suppress("DEPRECATION")
        val url = inner.beginPairingFlow(pairingUrl, scopes.toTypedArray(), entryPoint.entryName)
        val state = Uri.parse(url).getQueryParameter("state")!!
        AuthFlowUrl(state, url)
    }

    override suspend fun completeOAuthFlow(code: String, state: String): Boolean {
        val result = handleFxaExceptions(logger, "complete oauth flow") {
            @Suppress("DEPRECATION")
            inner.completeOAuthFlow(code, state)
        }
        return result != null
    }

    override suspend fun checkAuthorizationStatus(singleScope: String): Boolean? {
        // Now that internal token caches are cleared, we can perform a connectivity check.
        // Do so by requesting a new access token using an internally-stored "refresh token".
        // Success here means that we're still able to connect - our cached access token simply expired.
        // Failure indicates that we need to re-authenticate.
        try {
            inner.getAccessToken(singleScope)
            // We were able to obtain a token, so we're in a good authorization state.
            return true
        } catch (e: FxaUnauthorizedException) {
            // We got back a 401 while trying to obtain a new access token, which means our refresh
            // token is also in a bad state. We need re-authentication for the tested scope.
            return false
        } catch (e: FxaPanicException) {
            // Re-throw any panics we may encounter.
            throw e
        } catch (e: FxaException) {
            // On any other FxaExceptions (networking, etc) we have to return an indeterminate result.
            return null
        }
        // Re-throw all other exceptions.
    }

    override fun authErrorDetected() {
        // fxalib maintains some internal token caches that need to be cleared whenever we
        // hit an auth problem. Call below makes that clean-up happen.
        inner.clearAccessTokenCache()
    }

    override suspend fun disconnect(): Boolean {
        inner.queueAction(FxaAction.Disconnect)
        // The caller expects a result, but this can never throw, so just return true
        return true
    }

    // == FxaClient wrapping ==
    //
    // There are the methods that FxaClient has that OAuthAccount doesn't.  All are internal, since
    // only FxaAccountManager uses them.

    internal fun getAuthState() = inner.getAuthState()
    internal fun queueAction(action: FxaAction) = inner.queueAction(action)
    internal suspend fun gatherTelemetry() = inner.gatherTelemetry()
    internal fun simulateNetworkError() = inner.simulateNetworkError()
    internal fun simulateTemporaryAuthTokenIssue() = inner.simulateTemporaryAuthTokenIssue()
    internal fun simulatePermanentAuthTokenIssue() = inner.simulatePermanentAuthTokenIssue()

    /*
     * CoroutineContext for the client account
     *
     * Jobs that use this CoroutineContext will be cancelled when [close] is called.
     */
    val clientCoroutineContext = inner.coroutineContext

    // == Shared functionality ==
    //
    // These methods are shared between FxaClient and OAuthAccount and are useful to both
    // [OAuthAccount] consumers and [FxaAccountManager].
    //
    // It's safe to mix calls to [FxaAccountManager] methods and these methods.

    override fun deviceConstellation(): DeviceConstellation {
        return deviceConstellation
    }

    override suspend fun getProfile(ignoreCache: Boolean) = inner.getProfile(ignoreCache).into()

    override fun getCurrentDeviceId(): String? {
        // This is awkward, yes. Underlying method simply reads some data from in-memory state, and yet it throws
        // in case that data isn't there. See https://github.com/mozilla/application-services/issues/2202.
        return try {
            inner.getCurrentDeviceId()
        } catch (e: FxaPanicException) {
            throw e
        } catch (e: FxaException) {
            null
        }
    }

    override fun getSessionToken(): String? {
        return try {
            // This is awkward, yes. Underlying method simply reads some data from in-memory state, and yet it throws
            // in case that data isn't there. See https://github.com/mozilla/application-services/issues/2202.
            inner.getSessionToken()
        } catch (e: FxaPanicException) {
            throw e
        } catch (e: FxaException) {
            null
        }
    }

    override suspend fun getTokenServerEndpointURL() = inner.getTokenServerEndpointURL()

    override suspend fun getManageAccountURL(entryPoint: FxAEntryPoint): String? {
        return handleFxaExceptions(logger, "getManageAccountURL") {
            inner.getManageAccountURL(entryPoint.entryName)
        }
    }

    override fun getPairingAuthorityURL(): String {
        return inner.getPairingAuthorityURL()
    }

    /**
     * Fetches the connection success url.
     */
    fun getConnectionSuccessURL(): String {
        return inner.getConnectionSuccessURL()
    }

    override suspend fun getAccessToken(singleScope: String): AccessTokenInfo? {
        try {
            return inner.getAccessToken(singleScope).into()
        } catch (e: FxaException) {
            return when (e) {
                // For network errors we want to return null here.
                //
                // FxaUnspecifiedException shouldn't happen, but if it does returning `null` is a
                // reasonable thing to do.  These errors are tracked in Sentry at the Rust level, so
                // if they're happening at too high a rate we should track them down and fix them.
                is FxaNetworkException, is FxaUnspecifiedException -> null
                // If there's an auth error, then re-check our authorization and return `null` in
                // the meantime
                is FxaUnauthorizedException -> {
                    inner.queueAction(FxaAction.CheckAuthorization)
                    null
                }
                // FxaSyncScopedKeyMissingException means we've hit
                // https://github.com/mozilla-mobile/android-components/issues/8527
                //
                // Since we don't know what's causing a missing key for the SCOPE_SYNC access tokens, we
                // do not attempt to recover here.  Disconnect and things should be fixed on
                // the next login.  Set the `fromAuthIssues` flag, so that the user sees the
                // authentication problems state in the UI.
                is FxaSyncScopedKeyMissingException -> {
                    inner.queueAction(FxaAction.LogoutFromAuthIssues)
                    null
                }
                else -> throw e
            }
        }
    }
}

private class WrappedPersistenceCallback : FxaPersistCallback {
    private val logger = Logger("WrappedPersistenceCallback")

    @Volatile
    private var persistenceCallback: StatePersistenceCallback? = null

    fun setCallback(callback: StatePersistenceCallback) {
        logger.debug("Setting persistence callback")
        persistenceCallback = callback
    }

    override fun persist(data: String) {
        val callback = persistenceCallback

        if (callback == null) {
            logger.warn(
                "InternalFxAcct tried persist state, but persistence callback is not set",
                throwable = Exception(),
            )
        } else {
            logger.debug("Logging state to $callback")
            callback.persist(data)
        }
    }
}

internal class WrappedFxaEventHandler : FxaEventHandler {
    private val logger = Logger("WrappedFxaEventHandler")

    @Volatile
    private var eventHandler: FxaEventHandler? = null

    fun setEventHandler(handler: FxaEventHandler) {
        logger.debug("Setting event handler")
        eventHandler = handler
    }

    override suspend fun onFxaEvent(event: FxaEvent) {
        val eventHandler = eventHandler

        if (eventHandler == null) {
            logger.error("onFxaEvent called with $event but eventHandler is not set")
        } else {
            logger.debug("Handling event: $event")
            eventHandler.onFxaEvent(event)
        }
    }
}
