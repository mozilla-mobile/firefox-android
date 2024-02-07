/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.navbar

import androidx.navigation.NavController
import mozilla.components.browser.menu.view.MenuButton
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.GleanMetrics.StartOnHome
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.toolbar.ToolbarMenu
import org.mozilla.fenix.components.toolbar.interactor.BrowserToolbarInteractor
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.tabstray.Page

/**
 * Navigation bar button types.
 */
sealed class NavBarButton {
    /**
     * A general action button, like Home or Back buttons.
     */
    data class ActionButton(val iconId: Int, val descriptionResourceId: Int) : NavBarButton() {
        var onClick: () -> Unit = {}
        var onLongPress: () -> Unit = {}
    }

    /**
     * A tab counter.
     */
    object TabCounterButton : NavBarButton() {
        var onClick: () -> Unit = {}
    }

    /**
     * Three dot menu button.
     */
    data class ThreeDotMenuButton(val menuButton: MenuButton) : NavBarButton()
}

/**
 * Holds a collection of default buttons.
 *
 * Items could be copied and customized for usage across the app.
 * Also used for building previews.
 */
object DefaultButtons {
    val home = NavBarButton.ActionButton(
        iconId = R.drawable.mozac_ic_home_24,
        descriptionResourceId = R.string.browser_toolbar_home,
    )

    val search = NavBarButton.ActionButton(
        iconId = R.drawable.mozac_ic_search_24,
        descriptionResourceId = R.string.browser_toolbar_home,
    )

    val menu = NavBarButton.ActionButton(
        iconId = R.drawable.mozac_ic_ellipsis_vertical_24,
        descriptionResourceId = R.string.mozac_browser_menu_button,
    )

    val back = NavBarButton.ActionButton(
        iconId = R.drawable.mozac_ic_back_24,
        descriptionResourceId = R.string.browser_menu_back,
    )

    val forward = NavBarButton.ActionButton(
        iconId = R.drawable.mozac_ic_forward_24,
        descriptionResourceId = R.string.browser_menu_forward,
    )

    val tabs = NavBarButton.TabCounterButton

    val defaultItems = listOf(back, forward, home, tabs, menu)
}

/**
 * Defines navigation bar locations with required dependencies for building the bav bar.
 */
sealed class NavBarLocation {

    /**
     * Represents BrowserFragment.
     */
    data class BrowserFragment(
        val interactor: BrowserToolbarInteractor,
        val navController: NavController,
        val thumbnailsFeature: ViewBoundFeatureWrapper<BrowserThumbnails>,
        val browsingModeManager: BrowsingModeManager,
        val menuButton: MenuButton,
    ) : NavBarLocation()

    /**
     * Represents HomeFragment.
     */
    data class HomeFragment(
        val navController: NavController,
        val browsingModeManager: BrowsingModeManager,
        val menuButton: MenuButton,
    ) : NavBarLocation()
}

/**
 * Constructs a list of button for a given navigation target.
 *
 * This function takes a [NavBarLocation] instance, identifying the current UI context,
 * and returns a tailored list of [NavBarButton]s suitable for that location. Each [NavBarButton]
 * is configured based on the specific requirements and characteristics of the navigation target,
 * ensuring a context-appropriate user interface.
 */
object ActionItemListBuilder {

    /**
     * This function takes a [NavBarLocation] instance, identifying the current UI context,
     * and returns a tailored list of [NavBarButton]s suitable for that location. Each [NavBarButton]
     * is configured based on the specific requirements and characteristics of the navigation target,
     * ensuring a context-appropriate user interface.
     */
    fun build(location: NavBarLocation): List<NavBarButton> {
        return when (location) {
            is NavBarLocation.BrowserFragment -> buildNavButtonsForOpenTab(location)
            is NavBarLocation.HomeFragment -> buildNavButtonsForHomePage(location)
        }
    }

    private fun buildNavButtonsForOpenTab(location: NavBarLocation.BrowserFragment): List<NavBarButton> {
        return listOf(
            DefaultButtons.back.copy().apply {
                onClick = {
                    location.interactor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Back(viewHistory = false),
                    )
                }
                onLongPress = {
                    location.interactor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Back(viewHistory = true),
                    )
                }
            },
            DefaultButtons.forward.copy().apply {
                onClick = {
                    location.interactor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Forward(viewHistory = false),
                    )
                }
                onLongPress = {
                    location.interactor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Forward(viewHistory = true),
                    )
                }
            },
            DefaultButtons.home.copy().apply {
                onClick = {
                    location.navController.navigate(
                        BrowserFragmentDirections.actionGlobalHome(),
                    )
                }
            },
            NavBarButton.TabCounterButton.apply {
                onClick = {
                    location.thumbnailsFeature.get()?.requestScreenshot()
                    location.navController.nav(
                        R.id.browserFragment,
                        BrowserFragmentDirections.actionGlobalTabsTrayFragment(
                            page = when (location.browsingModeManager.mode) {
                                BrowsingMode.Normal -> Page.NormalTabs
                                BrowsingMode.Private -> Page.PrivateTabs
                            },
                        ),
                    )
                }
            },
            NavBarButton.ThreeDotMenuButton(location.menuButton),
        )
    }

    private fun buildNavButtonsForHomePage(location: NavBarLocation.HomeFragment): List<NavBarButton> {
        return listOf(
            DefaultButtons.back.copy(),
            DefaultButtons.forward.copy(),
            DefaultButtons.search.copy(),
            NavBarButton.TabCounterButton.apply {
                onClick = {
                    StartOnHome.openTabsTray.record(mozilla.telemetry.glean.private.NoExtras())
                    location.navController.nav(
                        location.navController.currentDestination?.id,
                        NavGraphDirections.actionGlobalTabsTrayFragment(
                            page = when (location.browsingModeManager.mode) {
                                BrowsingMode.Normal -> Page.NormalTabs
                                BrowsingMode.Private -> Page.PrivateTabs
                            },
                        ),
                    )
                }
            },
            NavBarButton.ThreeDotMenuButton(location.menuButton),
        )
    }
}
