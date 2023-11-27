/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.account

import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.support.utils.ext.getSerializableExtraCompat
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity
import org.mozilla.fenix.ext.components

/**
 * A special custom tab for signing into a Firefox Account. The activity is closed once the user is signed in.
 */
class AccountCustomTabActivity : ExternalAppBrowserActivity() {

    private val mode: Mode by lazy {
        intent.getSerializableExtraCompat(MODE_INTENT_KEY, Mode::class.java) as Mode
    }

    private val accountStateObserver = object : AccountObserver {
        /**
         * Navigate away from this activity when we have successful authentication
         */
        override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
            if (mode == Mode.AUTHENTICATION) {
                finish()
            }
        }

        /**
         * Navigate away from this activity if the user has deleted the account
         */
        override fun onAccountDeleted() {
            if (mode == Mode.ACCOUNT_MANAGEMENT) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val accountManager = components.backgroundServices.accountManager
        accountManager.register(accountStateObserver, this, true)
    }

    companion object {
        const val MODE_INTENT_KEY = "mode"

        enum class Mode {
            AUTHENTICATION,
            ACCOUNT_MANAGEMENT,
        }
    }
}
