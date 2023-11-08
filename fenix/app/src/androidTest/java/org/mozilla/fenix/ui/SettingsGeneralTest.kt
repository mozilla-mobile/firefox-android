/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.helpers.AppAndSystemHelper.runWithSystemLocaleChanged
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RecyclerViewIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.getLoremIpsumAsset
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.ui.robots.checkTextSizeOnWebsite
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.util.FRENCH_LANGUAGE_HEADER
import org.mozilla.fenix.ui.util.FRENCH_SYSTEM_LOCALE_OPTION
import org.mozilla.fenix.ui.util.FR_SETTINGS
import org.mozilla.fenix.ui.util.ROMANIAN_LANGUAGE_HEADER
import java.util.Locale

/**
 *  Tests for verifying the General section of the Settings menu
 *
 */
class SettingsGeneralTest {
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

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2092697
    @Test
    fun verifyGeneralSettingsItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsToolbar()
            verifyGeneralHeading()
            verifySearchButton()
            verifySettingsOptionSummary("Search", "Google")
            verifyTabsButton()
            verifySettingsOptionSummary("Tabs", "Close manually")
            verifyHomepageButton()
            verifySettingsOptionSummary("Homepage", "Open on homepage after four hours")
            verifyCustomizeButton()
            verifyLoginsAndPasswordsButton()
            verifyAutofillButton()
            verifyAccessibilityButton()
            verifyLanguageButton()
            verifySetAsDefaultBrowserButton()
            verifyDefaultBrowserToggle(false)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/344213
    @SmokeTest
    @Test
    fun verifyFontSizingChangeTest() {
        // Goes through the settings and changes the default text on a webpage, then verifies if the text has changed.
        val fenixApp = activityIntentTestRule.activity.applicationContext as FenixApplication
        val webpage = getLoremIpsumAsset(mockWebServer).url

        // This value will represent the text size percentage the webpage will scale to. The default value is 100%.
        val textSizePercentage = 180

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAccessibilitySubMenu {
            clickFontSizingSwitch()
            verifyEnabledMenuItems()
            changeTextSizeSlider(textSizePercentage)
            verifyTextSizePercentage(textSizePercentage)
        }.goBack {
        }.goBack {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(webpage) {
            checkTextSizeOnWebsite(textSizePercentage, fenixApp.components)
        }.openThreeDotMenu {
        }.openSettings {
        }.openAccessibilitySubMenu {
            clickFontSizingSwitch()
            verifyMenuItemsAreDisabled()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/516079
    @SmokeTest
    @Test
    fun setAppLanguageDifferentThanSystemLanguageTest() {
        val enLanguageHeaderText = getStringResource(R.string.preferences_language)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLanguageSubMenu {
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(
                    activityIntentTestRule.activity.findViewById(R.id.locale_list),
                    2,
                ),
            ) {
                selectLanguage("Romanian")
                verifyLanguageHeaderIsTranslated(ROMANIAN_LANGUAGE_HEADER)
                selectLanguage("Français")
                verifyLanguageHeaderIsTranslated(FRENCH_LANGUAGE_HEADER)
                selectLanguage(FRENCH_SYSTEM_LOCALE_OPTION)
                verifyLanguageHeaderIsTranslated(enLanguageHeaderText)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/516080
    @Test
    fun searchInLanguagesListTest() {
        val systemLocaleDefault = getStringResource(R.string.default_locale_text)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLanguageSubMenu {
            verifyLanguageListIsDisplayed()
            openSearchBar()
            typeInSearchBar("French")
            verifySearchResultsContains(systemLocaleDefault)
            clearSearchBar()
            typeInSearchBar("French")
            selectLanguageSearchResult("Français")
            verifyLanguageHeaderIsTranslated(FRENCH_LANGUAGE_HEADER)
            // Add this step when https://github.com/mozilla-mobile/fenix/issues/26733 is fixed
            // verifyLanguageListIsDisplayed()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/516078
    // Because it requires changing system prefs, this test will run only on Debug builds
    @Ignore("Failing due to app translation bug, see: https://github.com/mozilla-mobile/fenix/issues/26729")
    @Test
    fun verifyFollowDeviceLanguageTest() {
        val frenchLocale = Locale("fr", "FR")

        runWithSystemLocaleChanged(frenchLocale, activityIntentTestRule) {
            mDevice.waitForIdle(waitingTimeLong)

            homeScreen {
            }.openThreeDotMenu {
            }.openSettings(localizedText = FR_SETTINGS) {
            }.openLanguageSubMenu(localizedText = FRENCH_LANGUAGE_HEADER) {
                verifyLanguageHeaderIsTranslated(FRENCH_LANGUAGE_HEADER)
                verifySelectedLanguage(FRENCH_SYSTEM_LOCALE_OPTION)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1360557
    @Test
    fun tabsSettingsMenuItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyTabsButton()
            verifySettingsOptionSummary("Tabs", "Close manually")
        }.openTabsSubMenu {
            verifyTabViewOptions()
            verifyCloseTabsOptions()
            verifyMoveOldTabsToInactiveOptions()
            verifySelectedCloseTabsOption("Never")
            clickClosedTabsOption("After one day")
            verifySelectedCloseTabsOption("After one day")
        }.goBack {
            verifySettingsOptionSummary("Tabs", "Close after one day")
        }.openTabsSubMenu {
            clickClosedTabsOption("After one week")
            verifySelectedCloseTabsOption("After one week")
        }.goBack {
            verifySettingsOptionSummary("Tabs", "Close after one week")
        }.openTabsSubMenu {
            clickClosedTabsOption("After one month")
            verifySelectedCloseTabsOption("After one month")
        }.goBack {
            verifySettingsOptionSummary("Tabs", "Close after one month")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/243583
    // For API>23
    // Verifies the default browser switch opens the system default apps menu.
    @SmokeTest
    @Test
    fun changeDefaultBrowserSetting() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyDefaultBrowserToggle(false)
            clickDefaultBrowserSwitch()
            verifyAndroidDefaultAppsMenuAppears()
        }
        // Dismiss the request
        mDevice.pressBack()
    }
}
