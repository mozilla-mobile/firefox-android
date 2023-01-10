/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.cookiebannerexception

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged

/**
 * Integration for cookie banner reduction
 *
 * @property store The [BrowserStore] this feature should subscribe to.
 * @property cookieBannerExceptionStore The [CookieBannerExceptionStore] to dispatch the action
 * @property sessionId current selected sessionId
 */
class CookieBannerIntegration(
    private val store: BrowserStore,
    private val cookieBannerExceptionStore: CookieBannerExceptionStore,
    private val sessionId: String,
) : LifecycleAwareFeature {
    private var scope: CoroutineScope? = null

    override fun start() {
        scope = store.flowScoped { flow ->
            flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(sessionId) }
                .ifAnyChanged { tab ->
                    arrayOf(
                        tab.cookieBanner,
                    )
                }
                .collect {
                    if (it.cookieBanner == EngineSession.CookieBannerHandlingStatus.NO_DETECTED) {
                        cookieBannerExceptionStore.dispatch(
                            CookieBannerExceptionAction.CookieBannerDetected(false),
                        )
                    } else {
                        cookieBannerExceptionStore.dispatch(
                            CookieBannerExceptionAction.CookieBannerDetected(true),
                        )
                    }
                }
        }
    }

    override fun stop() {
        scope?.cancel()
    }
}
