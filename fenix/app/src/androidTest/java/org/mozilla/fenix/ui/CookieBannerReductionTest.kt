package org.mozilla.fenix.ui

import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

class CookieBannerReductionTest {
    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    // Bug causing flakiness https://bugzilla.mozilla.org/show_bug.cgi?id=1807440
    @SmokeTest
    @Test
    fun verifyCookieBannerReductionTest() {
        val webSite = "voetbal24.be"

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(webSite.toUri()) {
            waitForPageToLoad()
            verifyCookieBannerExists(exists = true)
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsOptionSummary("Cookie banner reduction", "Off")
        }.openCookieBannerReductionSubMenu {
            verifyCookieBannerView(isCookieBannerReductionChecked = false)
            clickCookieBannerReductionToggle()
            verifyCheckedCookieBannerReductionToggle(isCookieBannerReductionChecked = true)
        }.goBack {
            verifySettingsOptionSummary("Cookie banner reduction", "On")
        }

        exitMenu()

        browserScreen {
            verifyCookieBannerExists(exists = false)
        }

        restartApp(activityTestRule)

        browserScreen {
            verifyCookieBannerExists(exists = false)
        }.openThreeDotMenu {
        }.openSettings {
        }.openCookieBannerReductionSubMenu {
            clickCookieBannerReductionToggle()
            verifyCheckedCookieBannerReductionToggle(false)
        }

        exitMenu()

        browserScreen {
        }.openThreeDotMenu {
        }.refreshPage {
            verifyCookieBannerExists(exists = false)
        }
    }

    // Bug causing flakiness https://bugzilla.mozilla.org/show_bug.cgi?id=1807440
    @SmokeTest
    @Test
    fun verifyCookieBannerReductionInPrivateBrowsingTest() {
        val webSite = "voetbal24.be"

        homeScreen {
        }.togglePrivateBrowsingMode()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webSite.toUri()) {
            waitForPageToLoad()
            verifyCookieBannerExists(exists = true)
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsOptionSummary("Cookie banner reduction", "Off")
        }.openCookieBannerReductionSubMenu {
            verifyCookieBannerView(isCookieBannerReductionChecked = false)
            clickCookieBannerReductionToggle()
            verifyCheckedCookieBannerReductionToggle(isCookieBannerReductionChecked = true)
        }.goBack {
            verifySettingsOptionSummary("Cookie banner reduction", "On")
        }

        exitMenu()

        browserScreen {
            verifyCookieBannerExists(exists = false)
        }

        restartApp(activityTestRule)

        homeScreen {
        }.openTabDrawer {
        }.openTab("Voetbal24") {
            verifyCookieBannerExists(exists = false)
        }.openThreeDotMenu {
        }.openSettings {
        }.openCookieBannerReductionSubMenu {
            clickCookieBannerReductionToggle()
            verifyCheckedCookieBannerReductionToggle(false)
            exitMenu()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.refreshPage {
            verifyCookieBannerExists(exists = false)
        }
    }
}
