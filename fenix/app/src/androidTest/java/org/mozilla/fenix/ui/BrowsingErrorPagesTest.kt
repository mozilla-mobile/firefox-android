/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.setNetworkEnabled
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 * Tests that verify errors encountered while browsing websites: unsafe pages, connection errors, etc
 */
class BrowsingErrorPagesTest {
    private val malwareWarning = getStringResource(R.string.mozac_browser_errorpages_safe_browsing_malware_uri_title)
    private val phishingWarning = getStringResource(R.string.mozac_browser_errorpages_safe_phishing_uri_title)
    private val unwantedSoftwareWarning =
        getStringResource(R.string.mozac_browser_errorpages_safe_browsing_unwanted_uri_title)
    private val harmfulSiteWarning = getStringResource(R.string.mozac_browser_errorpages_safe_harmful_uri_title)
    private lateinit var mockWebServer: MockWebServer

    @get: Rule
    val mActivityTestRule = HomeActivityTestRule.withDefaultSettingsOverrides()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        // Restoring network connection
        setNetworkEnabled(true)
        mockWebServer.shutdown()
    }

    @SmokeTest
    @Test
    fun blockMalwarePageTest() {
        val malwareURl = "http://itisatrap.org/firefox/its-an-attack.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(malwareURl.toUri()) {
            verifyPageContent(malwareWarning)
        }
    }

    @SmokeTest
    @Test
    fun blockPhishingPageTest() {
        val phishingURl = "http://itisatrap.org/firefox/its-a-trap.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(phishingURl.toUri()) {
            verifyPageContent(phishingWarning)
        }
    }

    @SmokeTest
    @Test
    fun blockUnwantedSoftwarePageTest() {
        val unwantedURl = "http://itisatrap.org/firefox/unwanted.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(unwantedURl.toUri()) {
            verifyPageContent(unwantedSoftwareWarning)
        }
    }

    @SmokeTest
    @Test
    fun blockHarmfulPageTest() {
        val harmfulURl = "https://itisatrap.org/firefox/harmful.html"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(harmfulURl.toUri()) {
            verifyPageContent(harmfulSiteWarning)
        }
    }

    // Failing with network interruption, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1833874
    // This tests the server ERROR_CONNECTION_REFUSED
    @Test
    fun connectionRefusedErrorMessageTest() {
        val testUrl = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testUrl.url) {
            waitForPageToLoad()
            verifyPageContent(testUrl.content)
            // Disconnecting the server
            mockWebServer.shutdown()
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
            verifyConnectionErrorMessage()
        }
    }

    @Test
    fun addressNotFoundErrorMessageTest() {
        val url = "ww.example.com"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(url.toUri()) {
            waitForPageToLoad()
            verifyAddressNotFoundErrorMessage()
            clickPageObject(itemWithResId("errorTryAgain"))
            verifyAddressNotFoundErrorMessage()
        }
    }

    @Test
    fun noInternetConnectionErrorMessageTest() {
        val url = "www.example.com"

        setNetworkEnabled(false)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(url.toUri()) {
            verifyNoInternetConnectionErrorMessage()
        }

        setNetworkEnabled(true)

        browserScreen {
            clickPageObject(itemWithResId("errorTryAgain"))
            waitForPageToLoad()
            verifyPageContent("Example Domain")
        }
    }
}
