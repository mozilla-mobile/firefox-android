/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.concept.engine.serviceworker.ServiceWorkerDelegate
import org.mozilla.fenix.ext.components

/**
 * Fenix own version of the `ServiceWorkerSupportFeature` from Android-Components
 * which adds the ability to navigate to the browser before opening a new tab.
 *
 * Will automatically register callbacks for service workers requests and cleanup when [fenixActivity] is destroyed.
 *
 * @param fenixActivity [FenixActivity] used for navigating to browser or accessing various app components.
 */
class ServiceWorkerSupportFeature(
    private val fenixActivity: FenixActivity,
) : ServiceWorkerDelegate, DefaultLifecycleObserver {
    override fun onDestroy(owner: LifecycleOwner) {
        fenixActivity.components.core.engine.unregisterServiceWorkerDelegate()
    }

    override fun onCreate(owner: LifecycleOwner) {
        fenixActivity.components.core.engine.registerServiceWorkerDelegate(this)
    }

    override fun addNewTab(engineSession: EngineSession): Boolean {
        with(fenixActivity) {
            openToBrowser(BrowserDirection.FromHome)

            components.useCases.tabsUseCases.addTab(
                flags = LoadUrlFlags.external(),
                engineSession = engineSession,
                source = SessionState.Source.Internal.None,
            )
        }

        return true
    }
}
