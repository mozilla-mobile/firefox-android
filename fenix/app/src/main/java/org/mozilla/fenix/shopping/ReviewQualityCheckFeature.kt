/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Feature implementation that provides review quality check information for supported product
 * pages.
 *
 * @property browserStore Reference to the application's [BrowserStore].
 * @property shoppingExperienceFeature Reference to the [ShoppingExperienceFeature].
 * @property onAvailabilityChange Invoked when availability of this feature changes based on feature
 * flag and when the loaded page is a supported product page.
 */
class ReviewQualityCheckFeature(
    private val browserStore: BrowserStore,
    private val shoppingExperienceFeature: ShoppingExperienceFeature,
    private val onAvailabilityChange: (isAvailable: Boolean) -> Unit,
) : LifecycleAwareFeature {
    private var scope: CoroutineScope? = null

    override fun start() {
        if (!shoppingExperienceFeature.isEnabled) {
            onAvailabilityChange(false)
            return
        }

        scope = browserStore.flowScoped { flow ->
            flow.mapNotNull { it.selectedTab }
                .map { it.isProductUrl }
                .distinctUntilChanged()
                .collect(onAvailabilityChange)
        }
    }

    override fun stop() {
        scope?.cancel()
    }
}
