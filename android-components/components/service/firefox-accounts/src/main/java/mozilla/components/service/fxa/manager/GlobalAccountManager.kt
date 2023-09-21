/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.manager

import java.lang.ref.WeakReference

/**
 * A singleton which exposes an instance of [FxaAccountManager] for internal consumption.
 * Populated during initialization of [FxaAccountManager].
 * This exists to allow various internal parts without a direct reference to an instance of
 * [FxaAccountManager] to notify it of encountered auth errors via [authError].
 */
internal object GlobalAccountManager {
    private var instance: WeakReference<FxaAccountManager>? = null
    private var lastAuthErrorCheckPoint: Long = 0L
    private var authErrorCountWithinWindow: Int = 0

    internal interface Clock {
        fun getTimeCheckPoint(): Long
    }

    internal fun setInstance(am: FxaAccountManager) {
        instance = WeakReference(am)
        lastAuthErrorCheckPoint = 0
        authErrorCountWithinWindow = 0
    }

    internal fun close() {
        instance = null
    }

    internal suspend fun authError(operation: String) {
        instance?.get()?.encounteredAuthError(operation)
    }
}
