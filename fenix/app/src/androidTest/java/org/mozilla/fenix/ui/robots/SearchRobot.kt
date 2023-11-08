/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.PositionAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.AppAndSystemHelper.grantSystemPermission
import org.mozilla.fenix.helpers.AppAndSystemHelper.isPackageInstalled
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.Constants.LONG_CLICK_DURATION
import org.mozilla.fenix.helpers.Constants.RETRY_COUNT
import org.mozilla.fenix.helpers.Constants.SPEECH_RECOGNITION
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdExists
import org.mozilla.fenix.helpers.MatcherHelper.itemWithDescription
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.SessionLoadedIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.waitForObjects

/**
 * Implementation of Robot Pattern for the search fragment.
 */
class SearchRobot {
    fun verifySearchView() =
        assertTrue(
            mDevice.findObject(
                UiSelector().resourceId("$packageName:id/search_wrapper"),
            ).waitForExists(waitingTime),
        )

    fun verifySearchToolbar(isDisplayed: Boolean) =
        assertItemWithResIdExists(
            itemWithResId("$packageName:id/mozac_browser_toolbar_edit_url_view"),
            exists = isDisplayed,
        )

    fun verifyScanButtonVisibility(visible: Boolean = true) {
        if (visible) {
            assertTrue(
                scanButton.waitForExists(waitingTime),
            )
        } else {
            assertTrue(
                scanButton.waitUntilGone(waitingTime),
            )
        }
    }

    fun verifyVoiceSearchButtonVisibility(enabled: Boolean) {
        if (enabled) {
            assertTrue(voiceSearchButton.waitForExists(waitingTime))
        } else {
            assertFalse(voiceSearchButton.waitForExists(waitingTimeShort))
        }
    }

    // Device or AVD requires a Google Services Android OS installation
    fun startVoiceSearch() {
        voiceSearchButton.click()
        grantSystemPermission()

        if (isPackageInstalled(Constants.PackageName.GOOGLE_QUICK_SEARCH)) {
            Intents.intended(IntentMatchers.hasAction(SPEECH_RECOGNITION))
        }
    }

    fun verifySearchEngineSuggestionResults(rule: ComposeTestRule, vararg searchSuggestions: String, searchTerm: String) {
        rule.waitForIdle()
        for (i in 1..RETRY_COUNT) {
            try {
                for (searchSuggestion in searchSuggestions) {
                    mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains(searchSuggestion)))
                    rule.onNodeWithTag("mozac.awesomebar.suggestions")
                        .performScrollToNode(hasText(searchSuggestion))
                        .assertExists()
                }
                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    mDevice.pressBack()
                    homeScreen {
                    }.openSearch {
                        typeSearch(searchTerm)
                    }
                }
            }
        }
    }

    fun verifySuggestionsAreNotDisplayed(rule: ComposeTestRule, vararg searchSuggestions: String) {
        rule.waitForIdle()
        for (searchSuggestion in searchSuggestions) {
            rule.onAllNodesWithTag("mozac.awesomebar.suggestions")
                .assertAny(
                    hasText(searchSuggestion)
                        .not(),
                )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun verifySearchSuggestionsCount(rule: ComposeTestRule, numberOfSuggestions: Int, searchTerm: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                rule.waitUntilNodeCount(hasTestTag("mozac.awesomebar.suggestion"), numberOfSuggestions, waitingTime)
                rule.onAllNodesWithTag("mozac.awesomebar.suggestion").assertCountEquals(numberOfSuggestions)

                break
            } catch (e: ComposeTimeoutException) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    mDevice.pressBack()
                    homeScreen {
                    }.openSearch {
                        typeSearch(searchTerm)
                    }
                }
            }
        }
    }

    fun verifyAllowSuggestionsInPrivateModeDialog() {
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_title)),
            ).waitForExists(waitingTime),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_text)),
            ).exists(),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text("Learn more"),
            ).exists(),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_allow_button)),
            ).exists(),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_do_not_allow_button)),
            ).exists(),
        )
    }

    fun denySuggestionsInPrivateMode() {
        mDevice.findObject(
            UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_do_not_allow_button)),
        ).click()
    }

    fun allowSuggestionsInPrivateMode() {
        mDevice.findObject(
            UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_allow_button)),
        ).click()
    }

    fun verifySearchSelectorButton() {
        assertTrue(searchSelectorButton.waitForExists(waitingTime))
    }

    fun clickSearchSelectorButton() {
        searchSelectorButton.waitForExists(waitingTime)
        searchSelectorButton.click()
    }

    fun verifySearchEngineIcon(name: String) =
        assertTrue(itemWithDescription(name).waitForExists(waitingTime))

    fun verifySearchBarPlaceholder(text: String) {
        browserToolbarEditView().waitForExists(waitingTime)
        assertTrue(
            browserToolbarEditView().text == text,
        )
    }

    fun verifySearchShortcutListContains(vararg searchEngineName: String, shouldExist: Boolean = true) {
        searchEngineName.forEach {
            if (shouldExist) {
                assertTrue(
                    searchShortcutList.getChild(UiSelector().text(it))
                        .waitForExists(waitingTimeShort),
                )
            } else {
                assertTrue(
                    searchShortcutList.getChild(UiSelector().text(it))
                        .waitUntilGone(waitingTimeShort),
                )
            }
        }
    }

    // New unified search UI search selector.
    fun selectTemporarySearchMethod(searchEngineName: String) {
        searchShortcutList.getChild(UiSelector().text(searchEngineName)).click()
    }

    fun clickScanButton() =
        scanButton.also {
            it.waitForExists(waitingTime)
            it.click()
        }

    fun clickDismissPermissionRequiredDialog() {
        dismissPermissionButton.waitForExists(waitingTime)
        dismissPermissionButton.click()
    }

    fun clickGoToPermissionsSettings() {
        goToPermissionsSettingsButton.waitForExists(waitingTime)
        goToPermissionsSettingsButton.click()
    }

    fun verifyScannerOpen() {
        assertTrue(
            mDevice.findObject(UiSelector().resourceId("$packageName:id/view_finder"))
                .waitForExists(waitingTime) ||
                // In case there is no camera available, an error will be shown.
                mDevice.findObject(UiSelector().resourceId("$packageName:id/camera_error"))
                    .exists(),
        )
    }

    fun typeSearch(searchTerm: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"),
        ).waitForExists(waitingTime)

        browserToolbarEditView().setText(searchTerm)

        mDevice.waitForIdle()
    }

    fun clickClearButton() {
        clearButton().click()
    }

    fun tapOutsideToDismissSearchBar() {
        itemWithResId("$packageName:id/search_wrapper").click()
        itemWithResId("$packageName:id/mozac_browser_toolbar_edit_url_view")
            .waitUntilGone(waitingTime)
    }

    fun longClickToolbar() {
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/awesomeBar"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
            .waitForExists(waitingTime)
        val toolbar = mDevice.findObject(By.res("$packageName:id/toolbar"))
        toolbar.click(LONG_CLICK_DURATION)
    }

    fun clickPasteText() {
        mDevice.findObject(UiSelector().textContains("Paste")).waitForExists(waitingTime)
        val pasteText = mDevice.findObject(By.textContains("Paste"))
        pasteText.click()
    }

    fun expandSearchSuggestionsList() {
        onView(allOf(withId(R.id.search_wrapper))).perform(
            closeSoftKeyboard(),
        )
        browserToolbarEditView().swipeUp(2)
    }

    fun verifyTranslatedFocusedNavigationToolbar(toolbarHintString: String) =
        assertTrue(browserToolbarEditView().text.contains(toolbarHintString))

    fun verifyTypedToolbarText(expectedText: String) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_url_view"))
            .waitForExists(waitingTime)
        onView(
            allOf(
                withText(expectedText),
                withId(R.id.mozac_browser_toolbar_edit_url_view),
            ),
        ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    fun verifySearchBarPosition(bottomPosition: Boolean) {
        onView(withId(R.id.toolbar))
            .check(
                if (bottomPosition) {
                    PositionAssertions.isCompletelyBelow(withId(R.id.keyboard_divider))
                } else {
                    PositionAssertions.isCompletelyAbove(withId(R.id.keyboard_divider))
                },
            )
    }

    class Transition {
        private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

        fun dismissSearchBar(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle()
            closeSoftKeyboard()
            mDevice.pressBack()
            try {
                assertTrue(searchWrapper().waitUntilGone(waitingTimeShort))
            } catch (e: AssertionError) {
                mDevice.pressBack()
                assertTrue(searchWrapper().waitUntilGone(waitingTimeShort))
            }

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitForIdle()
            browserToolbarEditView().setText("mozilla\n")
            mDevice.pressEnter()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun submitQuery(query: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            sessionLoadedIdlingResource = SessionLoadedIdlingResource()
            searchWrapper().waitForExists(waitingTime)
            browserToolbarEditView().setText(query)
            mDevice.pressEnter()

            runWithIdleRes(sessionLoadedIdlingResource) {
                assertTrue(
                    mDevice.findObject(
                        UiSelector().resourceId("$packageName:id/browserLayout"),
                    ).waitForExists(waitingTime),
                )
            }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickSearchEngineSettings(interact: SettingsSubMenuSearchRobot.() -> Unit): SettingsSubMenuSearchRobot.Transition {
            searchShortcutList.getChild(UiSelector().text("Search settings")).click()

            SettingsSubMenuSearchRobot().interact()
            return SettingsSubMenuSearchRobot.Transition()
        }

        fun clickSearchSuggestion(searchSuggestion: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.findObject(UiSelector().textContains(searchSuggestion)).also {
                it.waitForExists(waitingTime)
                it.clickAndWaitForNewWindow(waitingTimeShort)
            }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun searchScreen(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
    SearchRobot().interact()
    return SearchRobot.Transition()
}

private fun browserToolbarEditView() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))

private val dismissPermissionButton =
    mDevice.findObject(UiSelector().text("DISMISS"))

private val goToPermissionsSettingsButton =
    mDevice.findObject(UiSelector().text("GO TO SETTINGS"))

private val scanButton = itemWithDescription("Scan")

private fun clearButton() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_clear_view"))

private fun searchWrapper() = mDevice.findObject(UiSelector().resourceId("$packageName:id/search_wrapper"))

private val searchSelectorButton = itemWithResId("$packageName:id/search_selector")

private val searchShortcutList =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_menu_recyclerView"))

private val voiceSearchButton = mDevice.findObject(UiSelector().description("Voice search"))
