/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.AppAndSystemHelper.openAppFromExternalLink
import org.mozilla.fenix.helpers.DataGenerationHelper.createCustomTabIntent
import org.mozilla.fenix.helpers.FeatureSettingsHelperDelegate
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdAndText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithText
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.customTabScreen
import org.mozilla.fenix.ui.robots.enhancedTrackingProtection
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.notificationShade
import org.mozilla.fenix.ui.robots.openEditURLView
import org.mozilla.fenix.ui.robots.searchScreen

class CustomTabsTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val customMenuItem = "TestMenuItem"
    private val customTabActionButton = "CustomActionButton"

    /* Updated externalLinks.html to v2.0,
       changed the hypertext reference to mozilla-mobile.github.io/testapp/downloads for "External link"
     */
    private val externalLinksPWAPage = "https://mozilla-mobile.github.io/testapp/v2.0/externalLinks.html"
    private val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    @get: Rule
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java,
        true,
        false,
    )

    private val featureSettingsHelper = FeatureSettingsHelperDelegate()

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/249659
    @SmokeTest
    @Test
    fun verifyLoginSaveInCustomTabTest() {
        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                loginPage.toUri().toString(),
                customMenuItem,
            ),
        )

        customTabScreen {
            waitForPageToLoad()
            fillAndSubmitLoginCredentials("mozilla", "firefox")
        }

        browserScreen {
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(itemWithText("Save"))
        }

        openAppFromExternalLink(loginPage)

        browserScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins {
            verifySecurityPromptForLogins()
            tapSetupLater()
            verifySavedLoginsSectionUsername("mozilla")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2334762
    @Test
    fun copyCustomTabToolbarUrlTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem,
            ),
        )

        customTabScreen {
            longCLickAndCopyToolbarUrl()
        }

        openAppFromExternalLink(customTabPage.url.toString())

        navigationToolbar {
            openEditURLView()
        }

        searchScreen {
            clickClearButton()
            longClickToolbar()
            clickPasteText()
            verifyTypedToolbarText(customTabPage.url.toString())
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2334761
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1807289")
    @SmokeTest
    @Test
    fun verifyDownloadInACustomTabTest() {
        val customTabPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
        val downloadFile = "web_icon.png"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.toUri().toString(),
                customMenuItem,
            ),
        )

        customTabScreen {
            waitForPageToLoad()
        }

        browserScreen {
        }.clickDownloadLink(downloadFile) {
            verifyDownloadPrompt(downloadFile)
        }.clickDownload {
            verifyDownloadCompleteNotificationPopup()
        }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationExists("Download completed")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/249644
    // Verifies the main menu of a custom tab with a custom menu item
    @SmokeTest
    @Test
    fun verifyCustomTabMenuItemsTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem,
            ),
        )

        customTabScreen {
            verifyCustomTabCloseButton()
        }.openMainMenu {
            verifyPoweredByTextIsDisplayed()
            verifyCustomMenuItem(customMenuItem)
            verifyDesktopSiteButtonExists()
            verifyFindInPageButtonExists()
            verifyOpenInBrowserButtonExists()
            verifyBackButtonExists()
            verifyForwardButtonExists()
            verifyRefreshButtonExists()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/249645
    // The test opens a link in a custom tab then sends it to the browser
    @SmokeTest
    @Test
    fun openCustomTabInFirefoxTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
            verifyCustomTabCloseButton()
        }.openMainMenu {
        }.clickOpenInBrowserButton {
            verifyTabCounter("1")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2239548
    @Test
    fun shareCustomTabUsingToolbarButtonTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
        }.clickShareButton {
            verifyShareTabLayout()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/249643
    @Test
    fun verifyCustomTabViewItemsTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage.url.toString(),
                customActionButtonDescription = customTabActionButton,
            ),
        )

        customTabScreen {
            verifyCustomTabCloseButton()
            verifyCustomTabsSiteInfoButton()
            verifyCustomTabToolbarTitle(customTabPage.title)
            verifyCustomTabUrl(customTabPage.url.toString())
            verifyCustomTabActionButton(customTabActionButton)
            verifyCustomTabsShareButton()
            verifyMainMenuButton()
            clickCustomTabCloseButton()
        }
        homeScreen {
            verifyHomeScreenAppBarItems()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2239544
    @Test
    fun verifyPDFViewerInACustomTabTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)
        val pdfFormResource = TestAssetHelper.getPdfFormAsset(mockWebServer)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
            clickPageObject(itemWithText("PDF form file"))
            clickPageObject(itemWithResIdAndText("android:id/button2", "CANCEL"))
            waitForPageToLoad()
            verifyPDFReaderToolbarItems()
            verifyCustomTabCloseButton()
            verifyCustomTabsSiteInfoButton()
            verifyCustomTabToolbarTitle("pdfForm.pdf")
            verifyCustomTabUrl(pdfFormResource.url.toString())
            verifyCustomTabsShareButton()
            verifyMainMenuButton()
            clickCustomTabCloseButton()
        }
        homeScreen {
            verifyHomeScreenAppBarItems()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2239117
    @Test
    fun verifyCustomTabETPSheetAndToggleTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage.url.toString(),
                customActionButtonDescription = customTabActionButton,
            ),
        )

        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyEnhancedTrackingProtectionSheetStatus(status = "ON", state = true)
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus(status = "OFF", state = false)
        }

        openAppFromExternalLink(customTabPage.url.toString())

        browserScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            switchEnhancedTrackingProtectionToggle()
            verifyEnhancedTrackingProtectionOptionsEnabled(enabled = false)
        }

        exitMenu()

        browserScreen {
        }.goBack {
            // Actually exiting to the previously opened custom tab
        }

        enhancedTrackingProtection {
            verifyETPSectionIsDisplayedInQuickSettingsSheet(isDisplayed = false)
        }
    }
}
