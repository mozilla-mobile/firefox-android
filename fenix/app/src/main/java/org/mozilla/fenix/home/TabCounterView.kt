/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.ui.tabcounter.TabCounter
import mozilla.components.ui.tabcounter.TabCounterMenu
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.StartOnHome
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.toolbar.FenixTabCounterMenu
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.tabstray.Page

/**
 * Helper class for building the [FenixTabCounterMenu].
 *
 * @param context An Android [Context].
 * @param browsingModeManager [BrowsingModeManager] used for fetching the current browsing mode.
 * @param navController [NavController] used for navigation.
 * @param tabCounter The [TabCounter] that will be setup with event handlers.
 */
class TabCounterView(
    private val context: Context,
    private val browsingModeManager: BrowsingModeManager,
    private val navController: NavController,
    private val tabCounter: TabCounter,
) {

    init {
        val tabCounterMenu = FenixTabCounterMenu(
            context = context,
            onItemTapped = ::onItemTapped,
            iconColor = if (browsingModeManager.mode == BrowsingMode.Private) {
                ContextCompat.getColor(context, R.color.fx_mobile_private_text_color_primary)
            } else {
                null
            },
        )

        tabCounterMenu.updateMenu(
            showOnly = when (browsingModeManager.mode) {
                BrowsingMode.Normal -> BrowsingMode.Private
                BrowsingMode.Private -> BrowsingMode.Normal
            },
        )

        tabCounter.setOnLongClickListener {
            tabCounterMenu.menuController.show(anchor = it)
            true
        }

        tabCounter.setOnClickListener {
            StartOnHome.openTabsTray.record(NoExtras())

            navController.nav(
                navController.currentDestination?.id,
                NavGraphDirections.actionGlobalTabsTrayFragment(
                    page = when (browsingModeManager.mode) {
                        BrowsingMode.Normal -> Page.NormalTabs
                        BrowsingMode.Private -> Page.PrivateTabs
                    },
                ),
            )
        }
    }

    /**
     * Updates the tab counter count based on the current browser state.
     *
     * @param browserState [BrowserState] used to get the current tab count for the current
     * browsing mode.
     */
    fun update(browserState: BrowserState) {
        val isPrivate = browsingModeManager.mode.isPrivate
        val tabCount = if (isPrivate) {
            browserState.privateTabs.size
        } else {
            browserState.normalTabs.size
        }

        tabCounter.setCountWithAnimation(tabCount)

        if (context.settings().feltPrivateBrowsingEnabled) {
            tabCounter.toggleCounterMask(isPrivate)
        }
    }

    /**
     * Callback invoked when a menu item is tapped on.
     */
    internal fun onItemTapped(item: TabCounterMenu.Item) {
        if (item is TabCounterMenu.Item.NewTab) {
            browsingModeManager.mode = BrowsingMode.Normal
        } else if (item is TabCounterMenu.Item.NewPrivateTab) {
            browsingModeManager.mode = BrowsingMode.Private
        }
    }
}
