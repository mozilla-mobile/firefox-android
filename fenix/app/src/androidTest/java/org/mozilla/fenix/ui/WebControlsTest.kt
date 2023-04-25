/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithDescription
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdAndText
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestAssetHelper.getHTMLControlsFormAsset
import org.mozilla.fenix.helpers.TestHelper.assertNativeAppOpens
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.navigationToolbar
import java.time.LocalDate

/**
 *  Tests for verifying basic interactions with web control elements
 *
 */

class WebControlsTest {
    private lateinit var mockWebServer: MockWebServer

    private val hour = 10
    private val minute = 10
    private val colorHexValue = "#5b2067"
    private val emailLink = "mailto://example@example.com"
    private val phoneLink = "tel://1234567890"

    @get:Rule
    val activityTestRule = HomeActivityTestRule(
        isJumpBackInCFREnabled = false,
        isTCPCFREnabled = false,
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
    fun cancelCalendarFormTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("calendar"))
            clickPageObject(itemContainingText("CANCEL"))
            clickPageObject(itemWithResId("submitDate"))
            verifyNoDateIsSelected()
        }
    }

    @Test
    fun setAndClearCalendarFormTest() {
        val currentDate = LocalDate.now()
        val currentDay = currentDate.dayOfMonth
        val currentMonth = currentDate.month
        val currentYear = currentDate.year
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("calendar"))
            clickPageObject(itemWithDescription("$currentDay $currentMonth $currentYear"))
            clickPageObject(itemContainingText("OK"))
            clickPageObject(itemWithResId("submitDate"))
            verifySelectedDate()
            clickPageObject(itemWithResId("calendar"))
            clickPageObject(itemContainingText("CLEAR"))
            clickPageObject(itemWithResId("submitDate"))
            verifyNoDateIsSelected()
        }
    }

    @Test
    fun cancelClockFormTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("clock"))
            clickPageObject(itemContainingText("CANCEL"))
            clickPageObject(itemWithResId("submitTime"))
            verifyNoTimeIsSelected(hour, minute)
        }
    }

    @Test
    fun setAndClearClockFormTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("clock"))
            selectTime(hour, minute)
            clickPageObject(itemContainingText("OK"))
            clickPageObject(itemWithResId("submitTime"))
            verifySelectedTime(hour, minute)
            clickPageObject(itemWithResId("clock"))
            clickPageObject(itemContainingText("CLEAR"))
            clickPageObject(itemWithResId("submitTime"))
            verifyNoTimeIsSelected(hour, minute)
        }
    }

    @Test
    fun cancelColorFormTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("colorPicker"))
            clickPageObject(itemWithDescription(colorHexValue))
            clickPageObject(itemContainingText("CANCEL"))
            clickPageObject(itemWithResId("submitColor"))
            verifyColorIsNotSelected(colorHexValue)
        }
    }

    @Test
    fun setColorFormTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("colorPicker"))
            clickPageObject(itemWithDescription(colorHexValue))
            clickPageObject(itemContainingText("SET"))
            clickPageObject(itemWithResId("submitColor"))
            verifySelectedColor(colorHexValue)
        }
    }

    @Test
    fun verifyDropdownMenuTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("dropDown"))
            clickPageObject(itemContainingText("The National"))
            clickPageObject(itemWithResId("submitOption"))
            verifySelectedDropDownOption("The National")
        }
    }

    @Test
    fun externalLinkTest() {
        val externalLinksPage = TestAssetHelper.getExternalLinksAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(itemContainingText("External link"))
            verifyUrl("duckduckgo")
        }
    }

    @Test
    fun emailLinkTest() {
        val externalLinksPage = TestAssetHelper.getExternalLinksAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(itemContainingText("Email link"))
            clickPageObject(itemWithResIdAndText("android:id/button1", "OPEN"))
            assertNativeAppOpens(Constants.PackageName.GMAIL_APP, emailLink)
        }
    }

    @Test
    fun telephoneLinkTest() {
        val externalLinksPage = TestAssetHelper.getExternalLinksAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(itemContainingText("Telephone link"))
            clickPageObject(itemWithResIdAndText("android:id/button1", "OPEN"))
            assertNativeAppOpens(Constants.PackageName.PHONE_APP, phoneLink)
        }
    }
}
