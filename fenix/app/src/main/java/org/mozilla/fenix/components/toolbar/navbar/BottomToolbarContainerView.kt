/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.navbar

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.lib.state.ext.observeAsState
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.compose.Divider
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.FirefoxTheme

/**
 *  A helper class to add NavigationBar composable to a [ViewGroup].
 *
 * @param context The Context the view is running in.
 * @param container The ViewGroup into which the NavigationBar composable will be added.
 * @param location Fragment where the navigation bar is being used.
 * @param androidToolbarView An option toolbar view that will be added atop of the navigation bar.
 * @param browsingModeManager A helper class that provides access to the current [BrowsingMode].
 *
 * Defaults to [DefaultButtons.defaultItems] which provides a standard set of navigation items.
 */
class BottomToolbarContainerView(
    context: Context,
    container: ViewGroup,
    location: NavBarLocation,
    androidToolbarView: View? = null,
    browsingModeManager: BrowsingModeManager,
) {

    init {
        val composeView = ComposeView(context).apply {
            setContent {
                val isPrivate = browsingModeManager.mode.isPrivate
                val tabCount = context.components.core.store.observeAsState(initialValue = 0) { browserState ->
                    if (isPrivate) {
                        browserState.privateTabs.size
                    } else {
                        browserState.normalTabs.size
                    }
                }.value

                // In future, this will also accept tab.content.canGoForward
                // to manage active/inactive state of navigation buttons
                val buttons = ActionItemListBuilder.build(location)

                FirefoxTheme {
                    Column {
                        if (androidToolbarView != null) {
                            AndroidView(factory = { _ -> androidToolbarView })
                        } else {
                            Divider()
                        }

                        NavigationBar(
                            buttons = buttons,
                            tabCount = tabCount,
                        )
                    }
                }
            }
        }

        val layoutParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.MATCH_PARENT,
            CoordinatorLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }

        composeView.layoutParams = layoutParams
        container.addView(composeView)
    }
}
