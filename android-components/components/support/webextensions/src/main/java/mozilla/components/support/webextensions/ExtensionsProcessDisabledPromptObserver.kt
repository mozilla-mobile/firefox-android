/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.webextensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Observes the [BrowserStore] state for when the extensions process spawning has been disabled and
 * the user should be prompted. This requires running in both the foreground and background.
 *
 * @property store the application's [BrowserStore].
 * @property onShowExtensionsProcessDisabledPrompt a callback invoked when the application should
 * open a prompt.
 */
open class ExtensionsProcessDisabledPromptObserver(
    private val store: BrowserStore,
    private val onShowExtensionsProcessDisabledPrompt: () -> Unit = { },
) : LifecycleAwareFeature {
    private var scope: CoroutineScope? = null

    override fun start() {
        if (scope == null) {
            scope = store.flowScoped { flow ->
                flow.distinctUntilChangedBy { it.showExtensionsProcessDisabledPrompt }
                    .collect { state ->
                        if (state.showExtensionsProcessDisabledPrompt) {
                            onShowExtensionsProcessDisabledPrompt()
                        }
                    }
            }
        }
    }

    /**
     * This observer needs to run indefinitely because we need to react to state changes when the
     * app is either in the foreground or in the background.
     */
    override fun stop() {
        /** NO-OP */
    }
}
