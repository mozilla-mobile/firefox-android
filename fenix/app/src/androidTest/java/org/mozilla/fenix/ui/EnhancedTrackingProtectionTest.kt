/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import androidx.test.espresso.Espresso.pressBack
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getEnhancedTrackingProtectionAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.helpers.TestHelper.scrollToElementByText
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.enhancedTrackingProtection
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic UI functionality of Enhanced Tracking Protection
 *
 *  Including
 *  - Verifying default states
 *  - Verifying Enhanced Tracking Protection notification bubble
 *  - Verifying Enhanced Tracking Protection content sheet
 *  - Verifying Enhanced Tracking Protection content sheet details
 *  - Verifying Enhanced Tracking Protection toggle
 *  - Verifying Enhanced Tracking Protection options and functionality
 *  - Verifying Enhanced Tracking Protection site exceptions
 */

class EnhancedTrackingProtectionTest {
    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule(
        isJumpBackInCFREnabled = false,
        isTCPCFREnabled = false,
        isWallpaperOnboardingEnabled = false,
    )

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

    @Test
    fun testETPSettingsItemsAndSubMenus() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyEnhancedTrackingProtectionButton()
            verifySettingsOptionSummary("Enhanced Tracking Protection", "Standard")
        }.openEnhancedTrackingProtectionSubMenu {
            verifyEnhancedTrackingProtectionSummary()
            verifyLearnMoreText()
            verifyEnhancedTrackingProtectionTextWithSwitchWidget()
            verifyTrackingProtectionSwitchEnabled()
            verifyEnhancedTrackingProtectionOptionsEnabled()
            verifyEnhancedTrackingProtectionLevelSelected("Standard (default)", true)
            verifyStandardOptionDescription()
            verifyStrictOptionDescription()
            selectTrackingProtectionOption("Custom")
            verifyCustomTrackingProtectionSettings()
            scrollToElementByText("Standard (default)")
            verifyWhatsBlockedByStandardETPInfo()
            pressBack()
            verifyWhatsBlockedByStrictETPInfo()
            pressBack()
            verifyWhatsBlockedByCustomETPInfo()
            pressBack()
        }.openExceptions {
            verifyTPExceptionsDefaultView()
            openExceptionsLearnMoreLink()
        }
        browserScreen {
            verifyUrl("support.mozilla.org/en-US/kb/enhanced-tracking-protection-firefox-android")
        }
    }

    @Test
    fun testETPSettingsSummaryChange() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyEnhancedTrackingProtectionButton()
            verifySettingsOptionSummary("Enhanced Tracking Protection", "Standard")
        }.openEnhancedTrackingProtectionSubMenu {
            selectTrackingProtectionOption("Strict")
        }.goBack {
            verifySettingsOptionSummary("Enhanced Tracking Protection", "Strict")
        }.openEnhancedTrackingProtectionSubMenu {
            selectTrackingProtectionOption("Custom")
        }.goBack {
            verifySettingsOptionSummary("Enhanced Tracking Protection", "Custom")
        }.openEnhancedTrackingProtectionSubMenu {
            switchEnhancedTrackingProtectionToggle()
        }.goBack {
            verifySettingsOptionSummary("Enhanced Tracking Protection", "Off")
        }
    }

    @Test
    fun testETPOffGlobally() {
        val genericPage = getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            switchEnhancedTrackingProtectionToggle()
            verifyEnhancedTrackingProtectionOptionsEnabled(false)
            exitMenu()
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericPage.url) { }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyETPSwitchVisibility(false)
        }.closeEnhancedTrackingProtectionSheet {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            switchEnhancedTrackingProtectionToggle()
            verifyEnhancedTrackingProtectionOptionsEnabled(true)
        }.goBack {
        }.goBackToBrowser { }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyETPSwitchVisibility(true)
        }
    }

    // Tests adding ETP exceptions to websites and keeping that preference after restart
    @SmokeTest
    @Test
    fun testDisableETPExceptionToggle() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = "example.com"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {}
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus("OFF", false)
        }.closeEnhancedTrackingProtectionSheet {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondPage.toUri()) {
            verifyPageContent("Example Domain")
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyEnhancedTrackingProtectionSheetStatus("ON", true)
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus("OFF", false)
        }
        restartApp(activityTestRule)
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyEnhancedTrackingProtectionSheetStatus("OFF", false)
        }
    }

    @Test
    fun trackingProtectionSwitchEnabledRemovesExceptionTest() {
        val trackingPage = getEnhancedTrackingProtectionAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {
            waitForPageToLoad()
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus("OFF", false)
        }.closeEnhancedTrackingProtectionSheet {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
        }.openExceptions {
            verifySiteExceptionExists(trackingPage.url.host.toString(), true)
            exitMenu()
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus("ON", true)
        }.openProtectionSettings {
        }.openExceptions {
            verifySiteExceptionExists(trackingPage.url.host.toString(), false)
        }
    }

    // Tests removing TP exceptions individually or all at once
    @Test
    fun clearTrackingProtectionExceptionsTest() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = "example.com"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {}
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus("OFF", false)
        }.closeEnhancedTrackingProtectionSheet {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondPage.toUri()) {
            verifyPageContent("Example Domain")
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.toggleEnhancedTrackingProtectionFromSheet {
            verifyEnhancedTrackingProtectionSheetStatus("OFF", false)
        }.closeEnhancedTrackingProtectionSheet {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
        }.openExceptions {
            removeOneSiteException(secondPage)
        }.disableExceptions {
            verifyTPExceptionsDefaultView()
            exitMenu()
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyEnhancedTrackingProtectionSheetStatus("ON", true)
        }
    }

    @Test
    fun testStandardETPVisitSheetDetails() {
        val genericPage = getGenericAsset(mockWebServer, 1)
        val trackingProtectionTest = getEnhancedTrackingProtectionAsset(mockWebServer).url

        // browsing a generic page to allow GV to load on a fresh run
        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericPage.url) {
            verifyPageContent(genericPage.content)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingProtectionTest) {
            verifyTrackingProtectionWebContent("social not blocked")
            verifyTrackingProtectionWebContent("ads not blocked")
            verifyTrackingProtectionWebContent("analytics not blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyEnhancedTrackingProtectionSheetStatus("ON", true)
        }.openDetails {
            verifyCrossSiteCookiesBlocked(true)
            navigateBackToDetails()
            verifyCryptominersBlocked(true)
            navigateBackToDetails()
            verifyFingerprintersBlocked(true)
            navigateBackToDetails()
            verifyTrackingContentBlocked(false)
        }.closeEnhancedTrackingProtectionSheet {}
    }

    @Test
    fun testStrictVisitSheetDetails() {
        appContext.settings().setStrictETP()
        val genericPage = getGenericAsset(mockWebServer, 1)
        val trackingProtectionTest = getEnhancedTrackingProtectionAsset(mockWebServer).url

        // browsing a generic page to allow GV to load on a fresh run
        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericPage.url) {
        }.openTabDrawer {
            closeTab()
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(trackingProtectionTest) {
            verifyTrackingProtectionWebContent("social blocked")
            verifyTrackingProtectionWebContent("ads blocked")
            verifyTrackingProtectionWebContent("analytics blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
            verifyEnhancedTrackingProtectionSheetStatus("ON", true)
        }.openDetails {
            verifySocialMediaTrackersBlocked(true)
            navigateBackToDetails()
            verifyCryptominersBlocked(true)
            navigateBackToDetails()
            verifyFingerprintersBlocked(true)
            navigateBackToDetails()
            verifyTrackingContentBlocked(true)
            viewTrackingContentBlockList()
        }
    }

    @SmokeTest
    @Test
    fun defaultCustomTrackingProtectionSettingsTest() {
        val genericWebPage = getGenericAsset(mockWebServer, 1)
        val trackingPage = getEnhancedTrackingProtectionAsset(mockWebServer)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            selectTrackingProtectionOption("Custom")
            verifyCustomTrackingProtectionSettings()
        }.goBackToHomeScreen {
        }.openNavigationToolbar {
            // browsing a basic page to allow GV to load on a fresh run
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {
            verifyTrackingProtectionWebContent("social blocked")
            verifyTrackingProtectionWebContent("ads blocked")
            verifyTrackingProtectionWebContent("analytics blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }

        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.openDetails {
            verifyCrossSiteCookiesBlocked(true)
            navigateBackToDetails()
            verifyCryptominersBlocked(true)
            navigateBackToDetails()
            verifyFingerprintersBlocked(true)
            navigateBackToDetails()
            verifyTrackingContentBlocked(true)
            viewTrackingContentBlockList()
        }
    }

    // Tests the trackers blocked with the following Custom TP set up:
    // - Cookies set to "All cookies"
    // - Tracking content option OFF
    // - Fingerprinters, cryptominers and redirect trackers checked
    @Test
    fun customizedTrackingProtectionOptionsTest() {
        val genericWebPage = getGenericAsset(mockWebServer, 1)
        val trackingPage = getEnhancedTrackingProtectionAsset(mockWebServer)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            selectTrackingProtectionOption("Custom")
            verifyCustomTrackingProtectionSettings()
            selectTrackingProtectionOption("Isolate cross-site cookies")
            selectTrackingProtectionOption("All cookies (will cause websites to break)")
            selectTrackingProtectionOption("Tracking content")
        }.goBackToHomeScreen {
            mDevice.waitForIdle()
        }.openNavigationToolbar {
            // browsing a basic page to allow GV to load on a fresh run
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
            waitForPageToLoad()
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {
            verifyTrackingProtectionWebContent("social not blocked")
            verifyTrackingProtectionWebContent("ads not blocked")
            verifyTrackingProtectionWebContent("analytics not blocked")
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.openDetails {
            verifyCrossSiteCookiesBlocked(true)
            navigateBackToDetails()
            verifyCryptominersBlocked(true)
            navigateBackToDetails()
            verifyFingerprintersBlocked(true)
            navigateBackToDetails()
            verifyTrackingContentBlocked(false)
        }
    }

    @Test
    fun disableCustomTrackingProtectionOptionsTest() {
        val genericWebPage = getGenericAsset(mockWebServer, 1)
        val trackingPage = getEnhancedTrackingProtectionAsset(mockWebServer)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            selectTrackingProtectionOption("Custom")
            verifyCustomTrackingProtectionSettings()
            selectTrackingProtectionOption("Cookies")
            selectTrackingProtectionOption("Tracking content")
            selectTrackingProtectionOption("Cryptominers")
            selectTrackingProtectionOption("Fingerprinters")
            selectTrackingProtectionOption("Redirect Trackers")
        }.goBackToHomeScreen {
            mDevice.waitForIdle()
        }.openNavigationToolbar {
            // browsing a basic page to allow GV to load on a fresh run
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
            waitForPageToLoad()
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {
            verifyTrackingProtectionWebContent("social not blocked")
            verifyTrackingProtectionWebContent("ads not blocked")
            verifyTrackingProtectionWebContent("analytics not blocked")
            verifyTrackingProtectionWebContent("Fingerprinting not blocked")
            verifyTrackingProtectionWebContent("Cryptomining not blocked")
        }
    }

    @Test
    fun testTrackingContentBlockedOnlyInPrivateTabs() {
        val genericWebPage = getGenericAsset(mockWebServer, 1)
        val trackingPage = getEnhancedTrackingProtectionAsset(mockWebServer)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            verifyEnhancedTrackingProtectionOptionsEnabled()
            selectTrackingProtectionOption("Custom")
            verifyCustomTrackingProtectionSettings()
            selectTrackingProtectionOption("In all tabs")
            selectTrackingProtectionOption("Only in Private tabs")
        }.goBackToHomeScreen {
        }.openNavigationToolbar {
            // browsing a basic page to allow GV to load on a fresh run
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
            waitForPageToLoad()
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {
            verifyTrackingProtectionWebContent("social not blocked")
            verifyTrackingProtectionWebContent("ads not blocked")
            verifyTrackingProtectionWebContent("analytics not blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }.goToHomescreen {
        }.togglePrivateBrowsingMode()
        navigationToolbar {
        }.enterURLAndEnterToBrowser(trackingPage.url) {
            verifyTrackingProtectionWebContent("social blocked")
            verifyTrackingProtectionWebContent("ads blocked")
            verifyTrackingProtectionWebContent("analytics blocked")
            verifyTrackingProtectionWebContent("Fingerprinting blocked")
            verifyTrackingProtectionWebContent("Cryptomining blocked")
        }
        enhancedTrackingProtection {
        }.openEnhancedTrackingProtectionSheet {
        }.openDetails {
            verifyCrossSiteCookiesBlocked(true)
            navigateBackToDetails()
            verifyCryptominersBlocked(true)
            navigateBackToDetails()
            verifyFingerprintersBlocked(true)
            navigateBackToDetails()
            verifyTrackingContentBlocked(true)
            viewTrackingContentBlockList()
        }
    }

    @SmokeTest
    @Test
    fun blockCookiesStorageAccessTest() {
        // With Standard TrackingProtection settings
        val genericWebPage = getGenericAsset(mockWebServer, 1)
        val testPage = mockWebServer.url("pages/cross-site-cookies.html").toString().toUri()
        val originSite = "https://mozilla-mobile.github.io"
        val currentSite = "http://localhost:${mockWebServer.port}"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
            waitForPageToLoad()
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(testPage) {
            waitForPageToLoad()
        }.clickRequestStorageAccessButton {
            verifyCrossOriginCookiesPermissionPrompt(originSite, currentSite)
        }.clickPagePermissionButton(allow = false) {
            verifyPageContent("access denied")
        }
    }

    @SmokeTest
    @Test
    fun allowCookiesStorageAccessTest() {
        // With Standard TrackingProtection settings
        val genericWebPage = getGenericAsset(mockWebServer, 1)
        val testPage = mockWebServer.url("pages/cross-site-cookies.html").toString().toUri()
        val originSite = "https://mozilla-mobile.github.io"
        val currentSite = "http://localhost:${mockWebServer.port}"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericWebPage.url) {
            waitForPageToLoad()
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(testPage) {
            waitForPageToLoad()
        }.clickRequestStorageAccessButton {
            verifyCrossOriginCookiesPermissionPrompt(originSite, currentSite)
        }.clickPagePermissionButton(allow = true) {
            verifyPageContent("access granted")
        }
    }
}
