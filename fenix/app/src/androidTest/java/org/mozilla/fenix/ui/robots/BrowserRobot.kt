/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")

package org.mozilla.fenix.ui.robots

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.widget.TimePicker
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.Constants.LONG_CLICK_DURATION
import org.mozilla.fenix.helpers.Constants.RETRY_COUNT
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.HomeActivityComposeTestRule
import org.mozilla.fenix.helpers.MatcherHelper.assertItemContainingTextExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdAndTextExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdExists
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithDescription
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdAndText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithText
import org.mozilla.fenix.helpers.SessionLoadedIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.waitForObjects
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.utils.Settings
import java.time.LocalDate

class BrowserRobot {
    private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

    fun waitForPageToLoad() {
        progressBar.waitUntilGone(waitingTime)
        Log.i(TAG, "waitForPageToLoad: The page was loaded, the progress bar is gone")
    }

    fun verifyCurrentPrivateSession(context: Context) {
        val selectedTab = context.components.core.store.state.selectedTab
        assertTrue("Current session is private", selectedTab?.content?.private ?: false)
    }

    fun verifyUrl(url: String) {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        runWithIdleRes(sessionLoadedIdlingResource) {
            assertTrue(
                mDevice.findObject(
                    UiSelector()
                        .resourceId("$packageName:id/mozac_browser_toolbar_url_view")
                        .textContains(url.replace("http://", "")),
                ).waitForExists(waitingTime),
            )
        }
    }

    fun verifyHelpUrl() {
        verifyUrl("support.mozilla.org/")
    }

    fun verifyWhatsNewURL() {
        verifyUrl("mozilla.org/")
    }

    fun verifyRateOnGooglePlayURL() {
        verifyUrl("play.google.com/store/apps/details?id=org.mozilla.fenix")
    }

    /* Asserts that the text within DOM element with ID="testContent" has the given text, i.e.
     *  document.querySelector('#testContent').innerText == expectedText
     *
     */

    fun verifyPageContent(expectedText: String) {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        mDevice.waitNotNull(
            Until.findObject(By.res("$packageName:id/engineView")),
            waitingTime,
        )

        runWithIdleRes(sessionLoadedIdlingResource) {
            assertTrue(
                "Page didn't load or doesn't contain the expected text",
                mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime),
            )
        }
    }

    /* Verifies the information displayed on the about:cache page */
    fun verifyNetworkCacheIsEmpty(storage: String) {
        val memorySection = mDevice.findObject(UiSelector().description(storage))

        val gridView =
            if (storage == "memory") {
                memorySection.getFromParent(
                    UiSelector()
                        .className("android.widget.GridView")
                        .index(2),
                )
            } else {
                memorySection.getFromParent(
                    UiSelector()
                        .className("android.widget.GridView")
                        .index(4),
                )
            }

        val cacheSizeInfo =
            gridView.getChild(
                UiSelector().text("Number of entries:"),
            ).getFromParent(
                UiSelector().text("0"),
            )

        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(cacheSizeInfo.waitForExists(waitingTime))
                break
            } catch (e: AssertionError) {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage { }
            }
        }
    }

    fun verifyTabCounter(expectedText: String) {
        val counter =
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/counter_text")
                    .text(expectedText),
            )
        assertTrue(counter.waitForExists(waitingTime))
    }

    fun verifySnackBarText(expectedText: String) {
        mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains(expectedText)))

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .textContains(expectedText),
            ).waitForExists(waitingTime),
        )
    }

    fun verifyContextMenuForLocalHostLinks(containsURL: Uri) {
        // If the link is directing to another local asset the "Download link" option is not available
        // If the link is not re-directing to an external app the "Open link in external app" option is not available
        assertItemContainingTextExists(
            contextMenuLinkUrl(containsURL.toString()),
            contextMenuOpenLinkInNewTab,
            contextMenuOpenLinkInPrivateTab,
            contextMenuCopyLink,
            contextMenuShareLink,
        )
    }

    fun verifyContextMenuForLinksToOtherApps(containsURL: String) {
        // If the link is re-directing to an external app the "Open link in external app" option is available
        // If the link is not directing to another local asset the "Download link" option is not available
        assertItemContainingTextExists(
            contextMenuLinkUrl(containsURL),
            contextMenuOpenLinkInNewTab,
            contextMenuOpenLinkInPrivateTab,
            contextMenuCopyLink,
            contextMenuDownloadLink,
            contextMenuShareLink,
            contextMenuOpenInExternalApp,
        )
    }

    fun verifyContextMenuForLinksToOtherHosts(containsURL: Uri) {
        // If the link is re-directing to another host the "Download link" option is available
        // If the link is not re-directing to an external app the "Open link in external app" option is not available
        assertItemContainingTextExists(
            contextMenuLinkUrl(containsURL.toString()),
            contextMenuOpenLinkInNewTab,
            contextMenuOpenLinkInPrivateTab,
            contextMenuCopyLink,
            contextMenuDownloadLink,
            contextMenuShareLink,
        )
    }

    fun verifyLinkImageContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(Until.findObject(By.textContains(containsURL.toString())))
        mDevice.waitNotNull(
            Until.findObject(text("Open link in new tab")),
            waitingTime,
        )
        mDevice.waitNotNull(
            Until.findObject(text("Open link in private tab")),
            waitingTime,
        )
        mDevice.waitNotNull(Until.findObject(text("Copy link")), waitingTime)
        mDevice.waitNotNull(Until.findObject(text("Share link")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")),
            waitingTime,
        )
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")),
            waitingTime,
        )
    }

    fun verifyNavURLBarHidden() =
        assertTrue(navURLBar().waitUntilGone(waitingTime))

    fun verifySecureConnectionLockIcon() =
        onView(withId(R.id.mozac_browser_toolbar_security_indicator))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    fun verifyMenuButton() = threeDotButton().check(matches(isDisplayed()))

    fun verifyNoLinkImageContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(Until.findObject(By.textContains(containsURL.toString())))
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")),
            waitingTime,
        )
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")),
            waitingTime,
        )
    }

    fun verifyNotificationDotOnMainMenu() {
        assertTrue(
            mDevice.findObject(UiSelector().resourceId("$packageName:id/notification_dot"))
                .waitForExists(waitingTime),
        )
    }

    fun verifyHomeScreenButton() =
        homeScreenButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    fun verifySearchBar() = assertTrue(searchBar().waitForExists(waitingTime))

    fun dismissContentContextMenu() {
        mDevice.pressBack()
        assertItemWithResIdExists(itemWithResId("$packageName:id/engineView"))
    }

    fun createBookmark(url: Uri, folder: String? = null) {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(url) {
            // needs to wait for the right url to load before saving a bookmark
            verifyUrl(url.toString())
        }.openThreeDotMenu {
        }.bookmarkPage {
        }.takeIf { !folder.isNullOrBlank() }?.let {
            it.openThreeDotMenu {
            }.editBookmarkPage {
                setParentFolder(folder!!)
                saveEditBookmark()
            }
        }
    }

    fun longClickPDFImage() = longClickPageObject(itemWithResId("pdfjs_internal_id_13R"))

    fun verifyPDFReaderToolbarItems() {
        assertTrue(
            itemWithResIdAndText("download", "Download")
                .waitForExists(waitingTime),
        )
        assertTrue(
            itemWithResIdAndText("openInApp", "Open in app")
                .waitForExists(waitingTime),
        )
    }

    fun clickSubmitLoginButton() {
        clickPageObject(itemWithResId("submit"))
        itemWithResId("submit").waitUntilGone(waitingTime)
        mDevice.waitForIdle(waitingTimeLong)
    }

    fun enterPassword(password: String) {
        clickPageObject(itemWithResId("password"))
        setPageObjectText(itemWithResId("password"), password)

        assertTrue(mDevice.findObject(UiSelector().text(password)).waitUntilGone(waitingTime))
    }

    /**
     * Get the current playback state of the currently selected tab.
     * The result may be null if there if the currently playing media tab cannot be found in [store]
     *
     * @param store [BrowserStore] from which to get data about the current tab's state.
     * @return nullable [MediaSession.PlaybackState] indicating the media playback state for the current tab.
     */
    private fun getCurrentPlaybackState(store: BrowserStore): MediaSession.PlaybackState? {
        return store.state.selectedTab?.mediaSessionState?.playbackState
    }

    /**
     * Asserts that in [waitingTime] the playback state of the current tab will be [expectedState].
     *
     * @param store [BrowserStore] from which to get data about the current tab's state.
     * @param expectedState [MediaSession.PlaybackState] the playback state that will be asserted
     * @param waitingTime maximum time the test will wait for the playback state to become [expectedState]
     * before failing the assertion.
     */
    fun assertPlaybackState(store: BrowserStore, expectedState: MediaSession.PlaybackState) {
        val startMills = SystemClock.uptimeMillis()
        var currentMills: Long = 0
        while (currentMills <= waitingTime) {
            if (expectedState == getCurrentPlaybackState(store)) return
            currentMills = SystemClock.uptimeMillis() - startMills
        }
        fail("Playback did not moved to state: $expectedState")
    }

    fun swipeNavBarRight(tabUrl: String) {
        // failing to swipe on Firebase sometimes, so it tries again
        try {
            navURLBar().swipeRight(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        } catch (e: AssertionError) {
            navURLBar().swipeRight(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        }
    }

    fun swipeNavBarLeft(tabUrl: String) {
        // failing to swipe on Firebase sometimes, so it tries again
        try {
            navURLBar().swipeLeft(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        } catch (e: AssertionError) {
            navURLBar().swipeLeft(2)
            assertTrue(mDevice.findObject(UiSelector().text(tabUrl)).waitUntilGone(waitingTime))
        }
    }

    fun clickSuggestedLoginsButton() {
        for (i in 1..RETRY_COUNT) {
            try {
                mDevice.waitForObjects(suggestedLogins)
                suggestedLogins.click()
                mDevice.waitForObjects(suggestedLogins)
                break
            } catch (e: UiObjectNotFoundException) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    clickPageObject(itemWithResId("username"))
                }
            }
        }
    }

    fun setTextForApartmentTextBox(apartment: String) =
        itemWithResId("apartment").setText(apartment)

    fun clearAddressForm() {
        clearTextFieldItem(itemWithResId("streetAddress"))
        clearTextFieldItem(itemWithResId("city"))
        clearTextFieldItem(itemWithResId("country"))
        clearTextFieldItem(itemWithResId("zipCode"))
        clearTextFieldItem(itemWithResId("telephone"))
        clearTextFieldItem(itemWithResId("email"))
    }

    fun clickSelectAddressButton() {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(selectAddressButton.waitForExists(waitingTime))
                selectAddressButton.clickAndWaitForNewWindow(waitingTime)

                break
            } catch (e: AssertionError) {
                // Retrying to trigger the prompt, in case we hit https://bugzilla.mozilla.org/show_bug.cgi?id=1816869
                // This should be removed when the bug is fixed.
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    clickPageObject(itemWithResId("city"))
                    clickPageObject(itemWithResId("country"))
                }
            }
        }
    }

    fun verifySelectAddressButtonExists(exists: Boolean) = assertItemWithResIdExists(selectAddressButton, exists = exists)

    fun changeCreditCardExpiryDate(expiryDate: String) =
        itemWithResId("expiryMonthAndYear").setText(expiryDate)

    fun clickCreditCardNumberTextBox() {
        mDevice.wait(Until.findObject(By.res("cardNumber")), waitingTime)
        mDevice.findObject(By.res("cardNumber")).click()
        mDevice.waitForWindowUpdate(appName, waitingTimeShort)
    }

    fun clickCreditCardFormSubmitButton() =
        itemWithResId("submit").clickAndWaitForNewWindow(waitingTime)

    fun fillAndSaveCreditCard(cardNumber: String, cardName: String, expiryMonthAndYear: String) {
        itemWithResId("cardNumber").setText(cardNumber)
        mDevice.waitForIdle(waitingTime)
        itemWithResId("nameOnCard").setText(cardName)
        mDevice.waitForIdle(waitingTime)
        itemWithResId("expiryMonthAndYear").setText(expiryMonthAndYear)
        mDevice.waitForIdle(waitingTime)
        itemWithResId("submit").clickAndWaitForNewWindow(waitingTime)
        waitForPageToLoad()
        mDevice.waitForWindowUpdate(packageName, waitingTime)
    }

    fun verifyUpdateOrSaveCreditCardPromptExists(exists: Boolean) =
        assertItemWithResIdAndTextExists(
            itemWithResId("$packageName:id/save_credit_card_header"),
            exists = exists,
        )

    fun verifySelectCreditCardPromptExists(exists: Boolean) =
        assertItemWithResIdExists(selectCreditCardButton, exists = exists)

    fun verifyCreditCardSuggestion(vararg creditCardNumbers: String) {
        for (creditCardNumber in creditCardNumbers) {
            assertTrue(creditCardSuggestion(creditCardNumber).waitForExists(waitingTime))
        }
    }

    fun verifySuggestedUserName(userName: String) {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_feature_login_multiselect_expand"),
        ).waitForExists(waitingTime)

        assertTrue(
            mDevice.findObject(UiSelector().textContains(userName)).waitForExists(waitingTime),
        )
    }

    fun verifyPrefilledLoginCredentials(userName: String, password: String, credentialsArePrefilled: Boolean) {
        // Sometimes the assertion of the pre-filled logins fails so we are re-trying after refreshing the page
        for (i in 1..RETRY_COUNT) {
            try {
                if (credentialsArePrefilled) {
                    mDevice.waitForObjects(itemWithResId("username"))
                    assertTrue(itemWithResId("username").text.equals(userName))

                    mDevice.waitForObjects(itemWithResId("password"))
                    assertTrue(itemWithResId("password").text.equals(password))
                } else {
                    mDevice.waitForObjects(itemWithResId("username"))
                    assertFalse(itemWithResId("username").text.equals(userName))

                    mDevice.waitForObjects(itemWithResId("password"))
                    assertFalse(itemWithResId("password").text.equals(password))
                }

                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        clearTextFieldItem(itemWithResId("username"))
                        clickSuggestedLoginsButton()
                        verifySuggestedUserName(userName)
                        clickPageObject(itemWithResIdAndText("$packageName:id/username", userName))
                        clickPageObject(itemWithResId("togglePassword"))
                    }
                }
            }
        }
    }

    fun verifyAutofilledAddress(streetAddress: String) {
        mDevice.waitForObjects(itemWithResIdAndText("streetAddress", streetAddress))
        assertTrue(
            itemWithResIdAndText("streetAddress", streetAddress)
                .waitForExists(waitingTime),
        )
    }

    fun verifyManuallyFilledAddress(apartment: String) {
        mDevice.waitForObjects(itemWithResIdAndText("apartment", apartment))
        assertTrue(
            itemWithResIdAndText("apartment", apartment)
                .waitForExists(waitingTime),
        )
    }

    fun verifyAutofilledCreditCard(creditCardNumber: String) {
        mDevice.waitForObjects(itemWithResIdAndText("cardNumber", creditCardNumber))
        assertTrue(
            itemWithResIdAndText("cardNumber", creditCardNumber)
                .waitForExists(waitingTime),
        )
    }

    fun verifyPrefilledPWALoginCredentials(userName: String, shortcutTitle: String) {
        mDevice.waitForIdle(waitingTime)

        var currentTries = 0
        while (currentTries++ < 3) {
            try {
                assertTrue(itemWithResId("submit").waitForExists(waitingTime))
                itemWithResId("submit").click()
                assertTrue(itemWithResId("username").text.equals(userName))
                break
            } catch (e: AssertionError) {
                addToHomeScreen {
                }.searchAndOpenHomeScreenShortcut(shortcutTitle) {}
            }
        }
    }

    fun verifySaveLoginPromptIsDisplayed() =
        assertItemWithResIdExists(
            itemWithResId("$packageName:id/feature_prompt_login_fragment"),
        )

    fun verifySaveLoginPromptIsNotDisplayed() =
        assertItemWithResIdExists(
            itemWithResId("$packageName:id/feature_prompt_login_fragment"),
            exists = false,
        )

    fun verifyTrackingProtectionWebContent(state: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector().textContains(state),
                    ).waitForExists(waitingTimeLong),
                )

                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    Log.e("TestLog", "On try $i, trackers are not: $state")

                    navigationToolbar {
                    }.openThreeDotMenu {
                    }.refreshPage {
                    }
                }
            }
        }
    }

    fun verifyCookiesProtectionHintIsDisplayed(composeTestRule: HomeActivityComposeTestRule, isDisplayed: Boolean) {
        if (isDisplayed) {
            composeTestRule.onNodeWithTag("tcp_cfr.message").assertIsDisplayed()
            composeTestRule.onNodeWithTag("tcp_cfr.action").assertIsDisplayed()
            composeTestRule.onNodeWithTag("cfr.dismiss").assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithTag("tcp_cfr.message").assertDoesNotExist()
            composeTestRule.onNodeWithTag("tcp_cfr.action").assertDoesNotExist()
            composeTestRule.onNodeWithTag("cfr.dismiss").assertDoesNotExist()
        }
    }

    fun clickTCPCFRLearnMore(composeTestRule: HomeActivityComposeTestRule) {
        composeTestRule.onNodeWithTag("tcp_cfr.action").performClick()
    }

    fun dismissTCPCFRPopup(composeTestRule: HomeActivityComposeTestRule) {
        composeTestRule.onNodeWithTag("cfr.dismiss").performClick()
    }

    fun verifyShouldShowCFRTCP(shouldShow: Boolean, settings: Settings) {
        if (shouldShow) {
            assertTrue(settings.shouldShowTotalCookieProtectionCFR)
        } else {
            assertFalse(settings.shouldShowTotalCookieProtectionCFR)
        }
    }

    fun selectTime(hour: Int, minute: Int) =
        onView(
            isAssignableFrom(TimePicker::class.java),
        ).inRoot(
            isDialog(),
        ).perform(PickerActions.setTime(hour, minute))

    fun verifySelectedDate() {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected date is: $currentDate"),
                    ).waitForExists(waitingTime),
                )

                break
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected time isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("calendar"))
                clickPageObject(itemWithDescription("$currentDay $currentMonth $currentYear"))
                clickPageObject(itemContainingText("OK"))
                clickPageObject(itemWithResId("submitDate"))
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate"),
            ).waitForExists(waitingTime),
        )
    }

    fun verifyNoDateIsSelected() {
        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate"),
            ).waitForExists(waitingTimeShort),
        )
    }

    fun verifySelectedTime(hour: Int, minute: Int) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected time is: $hour:$minute"),
                    ).waitForExists(waitingTime),
                )

                break
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected time isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("clock"))
                clickPageObject(itemContainingText("CLEAR"))
                clickPageObject(itemWithResId("clock"))
                selectTime(hour, minute)
                clickPageObject(itemContainingText("OK"))
                clickPageObject(itemWithResId("submitTime"))
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected time is: $hour:$minute"),
            ).waitForExists(waitingTime),
        )
    }

    fun verifySelectedColor(hexValue: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected color is: $hexValue"),
                    ).waitForExists(waitingTime),
                )

                break
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected color isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("colorPicker"))
                clickPageObject(itemWithDescription(hexValue))
                clickPageObject(itemContainingText("SET"))
                clickPageObject(itemWithResId("submitColor"))
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected color is: $hexValue"),
            ).waitForExists(waitingTime),
        )
    }

    fun verifySelectedDropDownOption(optionName: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                mDevice.findObject(
                    UiSelector()
                        .textContains("Submit drop down option")
                        .resourceId("submitOption"),
                ).waitForExists(waitingTime)

                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected option is: $optionName"),
                    ).waitForExists(waitingTime),
                )

                break
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected option isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("dropDown"))
                clickPageObject(itemContainingText(optionName))
                clickPageObject(itemWithResId("submitOption"))
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected option is: $optionName"),
            ).waitForExists(waitingTime),
        )
    }

    fun verifyNoTimeIsSelected(hour: Int, minute: Int) {
        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $hour:$minute"),
            ).waitForExists(waitingTimeShort),
        )
    }

    fun verifyColorIsNotSelected(hexValue: String) {
        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $hexValue"),
            ).waitForExists(waitingTimeShort),
        )
    }

    fun verifyCookieBannerExists(exists: Boolean) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertItemWithResIdExists(cookieBanner, exists = exists)
                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        waitForPageToLoad()
                    }
                }
            }
        }
        assertItemWithResIdExists(cookieBanner, exists = exists)
    }

    fun verifyOpenLinkInAnotherAppPrompt() {
        assertItemWithResIdExists(itemWithResId("$packageName:id/parentPanel"))
        assertItemContainingTextExists(
            itemContainingText(
                getStringResource(R.string.mozac_feature_applinks_normal_confirm_dialog_title),
            ),
            itemContainingText(
                getStringResource(R.string.mozac_feature_applinks_normal_confirm_dialog_message),
            ),
        )
    }

    fun verifyPrivateBrowsingOpenLinkInAnotherAppPrompt(url: String, pageObject: UiObject) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertItemContainingTextExists(
                    itemContainingText(
                        getStringResource(R.string.mozac_feature_applinks_confirm_dialog_title),
                    ),
                    itemContainingText(url),
                )

                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        waitForPageToLoad()
                        clickPageObject(pageObject)
                    }
                }
            }
        }
    }

    fun verifyFindInPageBar(exists: Boolean) =
        assertItemWithResIdExists(
            itemWithResId("$packageName:id/findInPageView"),
            exists = exists,
        )

    fun verifyConnectionErrorMessage() {
        assertItemContainingTextExists(
            itemContainingText(getStringResource(R.string.mozac_browser_errorpages_connection_failure_title)),
        )
        assertItemWithResIdExists(itemWithResId("errorTryAgain"))
    }

    fun verifyAddressNotFoundErrorMessage() {
        assertItemContainingTextExists(
            itemContainingText(getStringResource(R.string.mozac_browser_errorpages_unknown_host_title)),
        )
        assertItemWithResIdExists(itemWithResId("errorTryAgain"))
    }

    fun verifyNoInternetConnectionErrorMessage() {
        assertItemContainingTextExists(
            itemContainingText(getStringResource(R.string.mozac_browser_errorpages_no_internet_title)),
        )
        assertItemWithResIdExists(itemWithResId("errorTryAgain"))
    }

    fun verifyOpenLinksInAppsCFRExists(exists: Boolean) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertItemWithResIdExists(
                    itemWithResId("$packageName:id/banner_container"),
                    exists = exists,
                )
                assertItemWithResIdAndTextExists(
                    itemWithResIdContainingText(
                        "$packageName:id/banner_info_message",
                        getStringResource(R.string.open_in_app_cfr_info_message_2),
                    ),
                    itemWithResIdContainingText(
                        "$packageName:id/dismiss",
                        getStringResource(R.string.open_in_app_cfr_negative_button_text),
                    ),
                    itemWithResIdContainingText(
                        "$packageName:id/action",
                        getStringResource(R.string.open_in_app_cfr_positive_button_text),
                    ),
                    exists = exists,
                )
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        progressBar.waitUntilGone(waitingTimeLong)
                    }
                }
            }
        }
    }

    fun verifySurveyButton() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_take_survey,
                ),
            ),
        )
        assertTrue(button.waitForExists(waitingTime))
    }

    fun verifySurveyButtonDoesNotExist() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_take_survey,
                ),
            ),
        )
        assertTrue(button.waitUntilGone(waitingTime))
    }

    fun verifySurveyNoThanksButton() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_not_take_survey,
                ),
            ),
        )
        assertTrue(button.waitForExists(waitingTime))
    }

    fun verifyHomeScreenSurveyCloseButton() {
        val button = mDevice.findObject(
            UiSelector().descriptionContains(
                "Close",
            ),
        )
        assertTrue(button.waitForExists(waitingTime))
    }

    fun clickOpenLinksInAppsDismissCFRButton() =
        itemWithResIdContainingText(
            "$packageName:id/dismiss",
            getStringResource(R.string.open_in_app_cfr_negative_button_text),
        ).click()

    fun clickTakeSurveyButton() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_take_survey,
                ),
            ),
        )
        button.waitForExists(waitingTime)
        button.click()
    }
    fun clickNoThanksSurveyButton() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_not_take_survey,
                ),
            ),
        )
        button.waitForExists(waitingTime)
        button.click()
    }

    fun longClickToolbar() = mDevice.findObject(By.res("$packageName:id/mozac_browser_toolbar_url_view")).click(LONG_CLICK_DURATION)

    fun verifyDownloadPromptIsDismissed() =
        assertItemWithResIdExists(
            itemWithResId("$packageName:id/viewDynamicDownloadDialog"),
            exists = false,
        )

    fun verifyCancelPrivateDownloadsPrompt(numberOfActiveDownloads: String) {
        assertItemWithResIdAndTextExists(
            itemWithResIdContainingText(
                "$packageName:id/title",
                getStringResource(R.string.mozac_feature_downloads_cancel_active_downloads_warning_content_title),
            ),
            itemWithResIdContainingText(
                "$packageName:id/body",
                "If you close all Private tabs now, $numberOfActiveDownloads download will be canceled. Are you sure you want to leave Private Browsing?",
            ),
            itemWithResIdContainingText(
                "$packageName:id/deny_button",
                getStringResource(R.string.mozac_feature_downloads_cancel_active_private_downloads_deny),
            ),
            itemWithResIdContainingText(
                "$packageName:id/accept_button",
                getStringResource(R.string.mozac_feature_downloads_cancel_active_downloads_accept),
            ),
        )
    }

    fun clickStayInPrivateBrowsingPromptButton() =
        itemWithResIdContainingText(
            "$packageName:id/deny_button",
            getStringResource(R.string.mozac_feature_downloads_cancel_active_private_downloads_deny),
        ).click()

    fun clickCancelPrivateDownloadsPromptButton() {
        itemWithResIdContainingText(
            "$packageName:id/accept_button",
            getStringResource(R.string.mozac_feature_downloads_cancel_active_downloads_accept),
        ).click()

        mDevice.waitForWindowUpdate(packageName, waitingTime)
    }

    fun fillPdfForm(name: String) {
        // Set PDF form text for the text box
        itemWithResId("pdfjs_internal_id_10R").setText(name)
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        if (
            !itemWithResId("pdfjs_internal_id_11R").exists() &&
            mDevice
                .executeShellCommand("dumpsys input_method | grep mInputShown")
                .contains("mInputShown=true")
        ) {
            // Close the keyboard
            mDevice.pressBack()
        }
        // Click PDF form check box
        itemWithResId("pdfjs_internal_id_11R").click()
    }

    class Transition {
        fun openThreeDotMenu(interact: ThreeDotMenuMainRobot.() -> Unit): ThreeDotMenuMainRobot.Transition {
            mDevice.waitForIdle(waitingTime)
            Log.i(TAG, "openThreeDotMenu: Device was idle for $waitingTime")
            threeDotButton().perform(click())
            Log.i(TAG, "openThreeDotMenu: Clicked the main menu button")

            ThreeDotMenuMainRobot().interact()
            return ThreeDotMenuMainRobot.Transition()
        }

        fun openNavigationToolbar(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            clickPageObject(navURLBar())
            searchBar().waitForExists(waitingTime)

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openTabDrawer(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            for (i in 1..RETRY_COUNT) {
                try {
                    mDevice.waitForObjects(
                        mDevice.findObject(
                            UiSelector()
                                .resourceId("$packageName:id/mozac_browser_toolbar_browser_actions"),
                        ),
                        waitingTime,
                    )

                    tabsCounter().click()
                    assertTrue(
                        itemWithResId("$packageName:id/new_tab_button")
                            .waitForExists(waitingTime),
                    )

                    break
                } catch (e: AssertionError) {
                    if (i == RETRY_COUNT) {
                        throw e
                    } else {
                        mDevice.waitForIdle()
                    }
                }
            }

            assertTrue(
                itemWithResId("$packageName:id/new_tab_button")
                    .waitForExists(waitingTime),
            )

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun openComposeTabDrawer(composeTestRule: HomeActivityComposeTestRule, interact: ComposeTabDrawerRobot.() -> Unit): ComposeTabDrawerRobot.Transition {
            for (i in 1..RETRY_COUNT) {
                try {
                    mDevice.waitForObjects(
                        mDevice.findObject(
                            UiSelector()
                                .resourceId("$packageName:id/mozac_browser_toolbar_browser_actions"),
                        ),
                        waitingTime,
                    )

                    tabsCounter().click()

                    composeTestRule.onNodeWithTag(TabsTrayTestTag.tabsTray).assertExists()

                    break
                } catch (e: AssertionError) {
                    if (i == RETRY_COUNT) {
                        throw e
                    } else {
                        mDevice.waitForIdle()
                    }
                }
            }

            composeTestRule.onNodeWithTag(TabsTrayTestTag.fab).assertExists()

            ComposeTabDrawerRobot(composeTestRule).interact()
            return ComposeTabDrawerRobot.Transition(composeTestRule)
        }

        fun openNotificationShade(interact: NotificationRobot.() -> Unit): NotificationRobot.Transition {
            mDevice.openNotification()

            NotificationRobot().interact()
            return NotificationRobot.Transition()
        }

        fun goToHomescreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            clickPageObject(itemWithDescription("Home screen"))

            mDevice.findObject(UiSelector().resourceId("$packageName:id/homeLayout"))
                .waitForExists(waitingTime) ||
                mDevice.findObject(
                    UiSelector().text(
                        getStringResource(R.string.onboarding_home_screen_jump_back_contextual_hint_2),
                    ),
                ).waitForExists(waitingTime)

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun goToHomescreenWithComposeTopSites(composeTestRule: HomeActivityComposeTestRule, interact: ComposeTopSitesRobot.() -> Unit): ComposeTopSitesRobot.Transition {
            clickPageObject(itemWithDescription("Home screen"))

            mDevice.findObject(UiSelector().resourceId("$packageName:id/homeLayout"))
                .waitForExists(waitingTime) ||
                mDevice.findObject(
                    UiSelector().text(
                        getStringResource(R.string.onboarding_home_screen_jump_back_contextual_hint_2),
                    ),
                ).waitForExists(waitingTime)

            ComposeTopSitesRobot(composeTestRule).interact()
            return ComposeTopSitesRobot.Transition(composeTestRule)
        }

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.pressBack()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickTabCrashedCloseButton(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            clickPageObject(itemWithText("Close tab"))
            mDevice.waitForIdle()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickShareSelectedText(interact: ShareOverlayRobot.() -> Unit): ShareOverlayRobot.Transition {
            clickContextMenuItem("Share")

            ShareOverlayRobot().interact()
            return ShareOverlayRobot.Transition()
        }

        fun clickDownloadLink(title: String, interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
            clickPageObject(itemContainingText(title))

            DownloadRobot().interact()
            return DownloadRobot.Transition()
        }

        fun clickStartCameraButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Open camera"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickStartMicrophoneButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Open microphone"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickStartAudioVideoButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Camera & Microphone"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickOpenNotificationButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Open notifications dialogue"))
            mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains("Allow to send notifications?")))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickGetLocationButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Get Location"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickRequestStorageAccessButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Clicks the "request storage access" button from the "cross-site-cookies.html" local asset
            clickPageObject(itemContainingText("requestStorageAccess()"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickRequestPersistentStorageAccessButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Clicks the "Persistent storage" button from "https://mozilla-mobile.github.io/testapp/permissions"
            clickPageObject(itemWithResId("persistentStorageButton"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickRequestDRMControlledContentAccessButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Clicks the "DRM-controlled content" button from "https://mozilla-mobile.github.io/testapp/permissions"
            clickPageObject(itemWithResId("drmPermissionButton"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun openSiteSecuritySheet(interact: SiteSecurityRobot.() -> Unit): SiteSecurityRobot.Transition {
            siteSecurityToolbarButton().waitForExists(waitingTime)
            siteSecurityToolbarButton().clickAndWaitForNewWindow(waitingTime)

            SiteSecurityRobot().interact()
            return SiteSecurityRobot.Transition()
        }

        fun clickManageAddressButton(interact: SettingsSubMenuAutofillRobot.() -> Unit): SettingsSubMenuAutofillRobot.Transition {
            itemWithResId("$packageName:id/manage_addresses")
                .clickAndWaitForNewWindow(waitingTime)

            SettingsSubMenuAutofillRobot().interact()
            return SettingsSubMenuAutofillRobot.Transition()
        }

        fun clickManageCreditCardsButton(interact: SettingsSubMenuAutofillRobot.() -> Unit): SettingsSubMenuAutofillRobot.Transition {
            itemWithResId("$packageName:id/manage_credit_cards")
                .clickAndWaitForNewWindow(waitingTime)

            SettingsSubMenuAutofillRobot().interact()
            return SettingsSubMenuAutofillRobot.Transition()
        }

        fun clickOpenLinksInAppsGoToSettingsCFRButton(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            itemWithResIdContainingText(
                "$packageName:id/action",
                getStringResource(R.string.open_in_app_cfr_positive_button_text),
            ).clickAndWaitForNewWindow(waitingTime)

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun clickDownloadPDFButton(interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
            itemWithResIdContainingText(
                "download",
                "Download",
            ).click()

            DownloadRobot().interact()
            return DownloadRobot.Transition()
        }

        fun clickSurveyButton(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            surveyButton.waitForExists(waitingTime)
            surveyButton.click()

            BrowserRobot().interact()
            return Transition()
        }

        fun clickNoThanksSurveyButton(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            surveyNoThanksButton.waitForExists(waitingTime)
            surveyNoThanksButton.click()

            BrowserRobot().interact()
            return Transition()
        }
        fun clickHomeScreenSurveyCloseButton(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            homescreenSurveyCloseButton.waitForExists(waitingTime)
            homescreenSurveyCloseButton.click()

            BrowserRobot().interact()
            return Transition()
        }
    }
}

fun browserScreen(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
    BrowserRobot().interact()
    return BrowserRobot.Transition()
}

fun navURLBar() = itemWithResId("$packageName:id/toolbar")

fun searchBar() = itemWithResId("$packageName:id/mozac_browser_toolbar_url_view")

fun homeScreenButton() = onView(withContentDescription(R.string.browser_toolbar_home))

private fun threeDotButton() = onView(withContentDescription("Menu"))

private fun tabsCounter() =
    mDevice.findObject(By.res("$packageName:id/counter_root"))

private val progressBar =
    itemWithResId("$packageName:id/mozac_browser_toolbar_progress")

private val suggestedLogins = itemWithResId("$packageName:id/loginSelectBar")
private val selectAddressButton = itemWithResId("$packageName:id/select_address_header")
private val selectCreditCardButton = itemWithResId("$packageName:id/select_credit_card_header")

private fun creditCardSuggestion(creditCardNumber: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/credit_card_number")
            .textContains(creditCardNumber),
    )

private fun siteSecurityToolbarButton() =
    itemWithResId("$packageName:id/mozac_browser_toolbar_security_indicator")

fun clickPageObject(item: UiObject) {
    for (i in 1..RETRY_COUNT) {
        Log.i(TAG, "clickPageObject: For loop i = $i")
        try {
            Log.i(TAG, "clickPageObject: Try block")
            item.waitForExists(waitingTime)
            item.click()
            Log.i(TAG, "clickPageObject: Clicked ${item.selector}")

            break
        } catch (e: UiObjectNotFoundException) {
            Log.i(TAG, "clickPageObject: Catch block")
            if (i == RETRY_COUNT) {
                throw e
            } else {
                browserScreen {
                    Log.i(TAG, "clickPageObject: Browser screen")
                }.openThreeDotMenu {
                    Log.i(TAG, "clickPageObject: Opened main menu")
                }.refreshPage {
                    progressBar.waitUntilGone(waitingTime)
                    Log.i(TAG, "clickPageObject: Page refreshed, progress bar is gone")
                }
            }
        }
    }
}

fun longClickPageObject(item: UiObject) {
    for (i in 1..RETRY_COUNT) {
        try {
            item.waitForExists(waitingTime)
            item.longClick()

            break
        } catch (e: UiObjectNotFoundException) {
            if (i == RETRY_COUNT) {
                throw e
            } else {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage {
                    progressBar.waitUntilGone(waitingTime)
                }
            }
        }
    }
}

fun clickContextMenuItem(item: String) {
    mDevice.waitNotNull(
        Until.findObject(text(item)),
        waitingTime,
    )
    mDevice.findObject(text(item)).click()
}

fun setPageObjectText(webPageItem: UiObject, text: String) {
    for (i in 1..RETRY_COUNT) {
        try {
            webPageItem.also {
                it.waitForExists(waitingTime)
                it.clearTextField()
                it.text = text
            }

            break
        } catch (e: UiObjectNotFoundException) {
            if (i == RETRY_COUNT) {
                throw e
            } else {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage {
                    progressBar.waitUntilGone(waitingTime)
                }
            }
        }
    }
}

fun clearTextFieldItem(item: UiObject) {
    item.waitForExists(waitingTime)
    item.clearTextField()
}

private val currentDate = LocalDate.now()
private val currentDay = currentDate.dayOfMonth
private val currentMonth = currentDate.month
private val currentYear = currentDate.year
private val cookieBanner = itemWithResId("startsiden-gdpr-disclaimer")

// Context menu items
// Link URL
private fun contextMenuLinkUrl(linkUrl: String) =
    itemContainingText(linkUrl)

// Open link in new tab option
private val contextMenuOpenLinkInNewTab =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_open_link_in_new_tab))

// Open link in private tab option
private val contextMenuOpenLinkInPrivateTab =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_open_link_in_private_tab))

// Copy link option
private val contextMenuCopyLink =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_copy_link))

// Download link option
private val contextMenuDownloadLink =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_download_link))

// Share link option
private val contextMenuShareLink =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_share_link))

// Open in external app option
private val contextMenuOpenInExternalApp =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_open_link_in_external_app))

private val surveyButton =
    itemContainingText(getStringResource(R.string.preferences_take_survey))

private val surveyNoThanksButton =
    itemContainingText(getStringResource(R.string.preferences_not_take_survey))

private val homescreenSurveyCloseButton =
    itemWithDescription("Close")
