/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.res.Configuration
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.verifyDarkThemeApplied
import org.mozilla.fenix.helpers.TestHelper.verifyLightThemeApplied
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

class SettingsCustomizeTest {
    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun getUiTheme(): Boolean {
        val mode =
            activityIntentTestRule.activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)

        return when (mode) {
            Configuration.UI_MODE_NIGHT_YES -> true // dark theme is set
            Configuration.UI_MODE_NIGHT_NO -> false // dark theme is not set, using light theme
            else -> false // default option is light theme
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/344212
    @Test
    fun changeThemeOfTheAppTest() {
        // Goes through the settings and changes the default search engine, then verifies it changes.
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            verifyThemes()
            selectDarkMode()
            verifyDarkThemeApplied(getUiTheme())
            selectLightMode()
            verifyLightThemeApplied(getUiTheme())
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/466571
    @Test
    fun setToolbarPositionTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            verifyToolbarPositionPreference("Bottom")
            clickTopToolbarToggle()
            verifyToolbarPositionPreference("Top")
        }.goBack {
        }.goBack {
            verifyToolbarPosition(defaultPosition = false)
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            clickBottomToolbarToggle()
            verifyToolbarPositionPreference("Bottom")
            exitMenu()
        }
        homeScreen {
            verifyToolbarPosition(defaultPosition = true)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1058682
    @Test
    fun turnOffSwipeToSwitchTabsPreferenceTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            verifySwipeToolbarGesturePrefState(true)
            clickSwipeToolbarToSwitchTabToggle()
            verifySwipeToolbarGesturePrefState(false)
            exitMenu()
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
            swipeNavBarRight(secondWebPage.url.toString())
            verifyUrl(secondWebPage.url.toString())
            swipeNavBarLeft(secondWebPage.url.toString())
            verifyUrl(secondWebPage.url.toString())
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1992289
    @Test
    fun pullToRefreshPreferenceTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            verifyPullToRefreshGesturePrefState(isEnabled = true)
            clickPullToRefreshToggle()
            verifyPullToRefreshGesturePrefState(isEnabled = false)
        }
    }
}
