/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
@file:SuppressWarnings("TooManyFunctions", "MatchingDeclarationName")
package mozilla.components.service.fxa

import mozilla.appservices.fxaclient.AccessTokenInfo
import mozilla.appservices.fxaclient.AccountEvent
import mozilla.appservices.fxaclient.Device
import mozilla.appservices.fxaclient.Profile
import mozilla.appservices.fxaclient.ScopedKey
import mozilla.appservices.fxaclient.TabHistoryEntry
import mozilla.components.concept.sync.AuthException
import mozilla.components.concept.sync.AuthExceptionType
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.Avatar
import mozilla.components.concept.sync.DeviceCapability
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.OAuthScopedKey
import mozilla.components.concept.sync.SyncAuthInfo

/**
 * Converts a raw 'action' string into an [AuthType] instance.
 * Actions come to us from FxA during an OAuth login, either over the WebChannel or via the redirect URL.
 */
fun String.toAuthType(): AuthType {
    return when (this) {
        "signin" -> AuthType.Signin
        "signup" -> AuthType.Signup
        "pairing" -> AuthType.Pairing
        // We want to gracefully handle 'actions' we don't know about.
        else -> AuthType.OtherExternal(this)
    }
}

/**
 * Captures basic OAuth authentication data (code, state) and any additional data FxA passes along.
 * @property authType Type of authentication which caused this object to be created.
 * @property code OAuth code.
 * @property state OAuth state.
 */
data class FxaAuthData(val authType: AuthType, val code: String, val state: String)

// The rest of this file describes translations between fxaclient's internal type definitions and analogous
// types defined by concept-sync. It's a little tedious, but ensures decoupling between abstract
// definitions and a concrete implementation. In practice, this means that concept-sync doesn't need
// impose a dependency on fxaclient native library.

fun AccessTokenInfo.into(): mozilla.components.concept.sync.AccessTokenInfo {
    return mozilla.components.concept.sync.AccessTokenInfo(
        scope = this.scope,
        token = this.token,
        key = this.key?.into(),
        expiresAt = this.expiresAt
    )
}

/**
 * Converts a generic [AccessTokenInfo] into a Firefox Sync-friendly [SyncAuthInfo] instance which
 * may be used for data synchronization.
 *
 * @return An [SyncAuthInfo] which is guaranteed to have a sync key.
 * @throws AuthException if [AccessTokenInfo] didn't have key information.
 */
fun mozilla.components.concept.sync.AccessTokenInfo.asSyncAuthInfo(tokenServerUrl: String): SyncAuthInfo {
    val keyInfo = this.key ?: throw AuthException(AuthExceptionType.KEY_INFO)

    return SyncAuthInfo(
        kid = keyInfo.kid,
        fxaAccessToken = this.token,
        fxaAccessTokenExpiresAt = this.expiresAt,
        syncKey = keyInfo.k,
        tokenServerUrl = tokenServerUrl
    )
}

fun ScopedKey.into(): OAuthScopedKey {
    return OAuthScopedKey(kid = this.kid, k = this.k, kty = this.kty, scope = this.scope)
}

fun Profile.into(): mozilla.components.concept.sync.Profile {
    return mozilla.components.concept.sync.Profile(
        uid = this.uid,
        email = this.email,
        avatar = this.avatar?.let {
            Avatar(
                url = it,
                isDefault = this.avatarDefault
            )
        },
        displayName = this.displayName
    )
}

fun Device.Type.into(): mozilla.components.concept.sync.DeviceType {
    return when (this) {
        Device.Type.DESKTOP -> DeviceType.DESKTOP
        Device.Type.MOBILE -> DeviceType.MOBILE
        Device.Type.TABLET -> DeviceType.TABLET
        Device.Type.TV -> DeviceType.TV
        Device.Type.VR -> DeviceType.VR
        Device.Type.UNKNOWN -> DeviceType.UNKNOWN
    }
}

fun DeviceType.into(): Device.Type {
    return when (this) {
        DeviceType.DESKTOP -> Device.Type.DESKTOP
        DeviceType.MOBILE -> Device.Type.MOBILE
        DeviceType.TABLET -> Device.Type.TABLET
        DeviceType.TV -> Device.Type.TV
        DeviceType.VR -> Device.Type.VR
        DeviceType.UNKNOWN -> Device.Type.UNKNOWN
    }
}

fun DeviceCapability.into(): Device.Capability {
    return when (this) {
        DeviceCapability.SEND_TAB -> Device.Capability.SEND_TAB
    }
}

fun Device.Capability.into(): DeviceCapability {
    return when (this) {
        Device.Capability.SEND_TAB -> DeviceCapability.SEND_TAB
    }
}

fun mozilla.components.concept.sync.DevicePushSubscription.into(): Device.PushSubscription {
    return Device.PushSubscription(
        endpoint = this.endpoint,
        authKey = this.authKey,
        publicKey = this.publicKey
    )
}

fun Device.PushSubscription.into(): mozilla.components.concept.sync.DevicePushSubscription {
    return mozilla.components.concept.sync.DevicePushSubscription(
        endpoint = this.endpoint,
        authKey = this.authKey,
        publicKey = this.publicKey
    )
}

fun Device.into(): mozilla.components.concept.sync.Device {
    return mozilla.components.concept.sync.Device(
        id = this.id,
        isCurrentDevice = this.isCurrentDevice,
        deviceType = this.deviceType.into(),
        displayName = this.displayName,
        lastAccessTime = this.lastAccessTime,
        subscriptionExpired = this.pushEndpointExpired,
        capabilities = this.capabilities.map { it.into() },
        subscription = this.pushSubscription?.into()
    )
}

fun mozilla.components.concept.sync.Device.into(): Device {
    return Device(
        id = this.id,
        isCurrentDevice = this.isCurrentDevice,
        deviceType = this.deviceType.into(),
        displayName = this.displayName,
        lastAccessTime = this.lastAccessTime,
        pushEndpointExpired = this.subscriptionExpired,
        capabilities = this.capabilities.map { it.into() },
        pushSubscription = this.subscription?.into()
    )
}

fun TabHistoryEntry.into(): mozilla.components.concept.sync.TabData {
    return mozilla.components.concept.sync.TabData(
        title = this.title,
        url = this.url
    )
}

fun mozilla.components.concept.sync.TabData.into(): TabHistoryEntry {
    return TabHistoryEntry(
        title = this.title,
        url = this.url
    )
}

fun AccountEvent.into(): mozilla.components.concept.sync.DeviceEvent {
    return when (this) {
        is AccountEvent.TabReceived -> this.into()
    }
}

fun AccountEvent.TabReceived.into(): mozilla.components.concept.sync.DeviceEvent.TabReceived {
    return mozilla.components.concept.sync.DeviceEvent.TabReceived(
        from = this.from?.into(),
        entries = this.entries.map { it.into() }
    )
}

fun mozilla.components.concept.sync.DeviceEvent.TabReceived.into(): AccountEvent.TabReceived {
    return AccountEvent.TabReceived(
        from = this.from?.into(),
        entries = this.entries.map { it.into() }.toTypedArray()
    )
}
