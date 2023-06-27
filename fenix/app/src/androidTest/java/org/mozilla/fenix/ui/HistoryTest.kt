/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.Context
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RecyclerViewIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.longTapSelectItem
import org.mozilla.fenix.helpers.TestHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.historyMenu
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.multipleSelectionToolbar
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.searchScreen

/**
 *  Tests for verifying basic functionality of history
 *
 */
class HistoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.settings()
            .shouldShowJumpBackInCFR = false

        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // Clearing all history data after each test to avoid overlapping data
        val applicationContext: Context = activityTestRule.activity.applicationContext
        val historyStorage = PlacesHistoryStorage(applicationContext)

        runBlocking {
            historyStorage.deleteEverything()
        }
    }

    @Test
    fun noHistoryItemsInCacheTest() {
        homeScreen {
        }.openThreeDotMenu {
            verifyHistoryButton()
        }.openHistory {
            verifyHistoryMenuView()
            verifyEmptyHistoryView()
        }
    }

    // Test running on beta/release builds in CI:
    // caution when making changes to it, so they don't block the builds
    @Test
    fun visitedUrlHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                verifyHistoryMenuView()
                verifyVisitedTimeTitle()
                verifyFirstTestPageTitle("Test_Page_1")
                verifyTestPageUrl(firstWebPage.url)
            }
        }
    }

    @Test
    fun deleteHistoryItemTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                clickDeleteHistoryButton(firstWebPage.url.toString())
            }
            verifyDeleteSnackbarText("Deleted")
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun undoDeleteHistoryItemTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                clickDeleteHistoryButton(firstWebPage.url.toString())
            }
            verifyUndoDeleteSnackBarButton()
            clickUndoDeleteButton()
            verifyHistoryItemExists(true, firstWebPage.url.toString())
        }
    }

    @SmokeTest
    @Test
    fun cancelDeleteAllHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                clickDeleteAllHistoryButton()
            }
            verifyDeleteConfirmationMessage()
            selectEverythingOption()
            cancelDeleteHistory()
            verifyHistoryItemExists(true, firstWebPage.url.toString())
        }
    }

    @SmokeTest
    @Test
    fun deleteAllHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                clickDeleteAllHistoryButton()
            }
            verifyDeleteConfirmationMessage()
            selectEverythingOption()
            confirmDeleteAllHistory()
            verifyDeleteSnackbarText("Browsing data deleted")
            verifyEmptyHistoryView()
        }
    }

    @SmokeTest
    @Test
    fun historyMultiSelectionToolbarItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
            }
        }

        multipleSelectionToolbar {
            verifyMultiSelectionCheckmark()
            verifyMultiSelectionCounter()
            verifyShareHistoryButton()
            verifyCloseToolbarButton()
        }.closeToolbarReturnToHistory {
            verifyHistoryMenuView()
        }
    }

    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1807268")
    @Test
    fun openHistoryInNewTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
            closeTab()
        }

        homeScreen { }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
                openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
            }
        }

        multipleSelectionToolbar {
        }.clickOpenNewTab {
            verifyExistingTabList()
            verifyNormalModeSelected()
        }
    }

    @Test
    fun openHistoryInPrivateTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
                openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
            }
        }

        multipleSelectionToolbar {
        }.clickOpenPrivateTab {
            verifyPrivateModeSelected()
            verifyExistingTabList()
        }
    }

    @Test
    fun deleteMultipleSelectionTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            mDevice.waitForIdle()
            verifyUrl(secondWebPage.url.toString())
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 2),
            ) {
                verifyHistoryItemExists(true, firstWebPage.url.toString())
                verifyHistoryItemExists(true, secondWebPage.url.toString())
                longTapSelectItem(firstWebPage.url)
                longTapSelectItem(secondWebPage.url)
                openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
            }
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        historyMenu {
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun shareButtonTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
            }
        }

        multipleSelectionToolbar {
            clickShareHistoryButton()
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    @Test
    fun verifySearchHistoryViewTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openHistory {
            clickSearchButton()
            verifyHistorySearchBar(true)
            verifyHistorySearchBarPosition(true)
            tapOutsideToDismissSearchBar()
            verifyHistorySearchBar(false)
        }.goBack {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            clickTopToolbarToggle()
        }

        exitMenu()

        browserScreen {
        }.openThreeDotMenu {
        }.openHistory {
            clickSearchButton()
            verifyHistorySearchBar(true)
            verifyHistorySearchBarPosition(false)
            dismissHistorySearchBarUsingBackButton()
            verifyHistorySearchBar(false)
        }
    }

    @Test
    fun verifyVoiceSearchInHistoryTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
            clickSearchButton()
            verifyHistorySearchBar(true)
        }
        searchScreen {
            startVoiceSearch()
        }
    }

    @Test
    fun verifySearchForHistoryItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
        }.openThreeDotMenu {
        }.openHistory {
            clickSearchButton()
            // Search for a valid term
            searchForHistoryItem(firstWebPage.title)
            verifySearchedHistoryItemExists(firstWebPage.url.toString(), true)
            verifySearchedHistoryItemExists(secondWebPage.url.toString(), false)
            // Search for invalid term
            searchForHistoryItem("Android")
            verifySearchedHistoryItemExists(firstWebPage.url.toString(), false)
            verifySearchedHistoryItemExists(secondWebPage.url.toString(), false)
        }
    }

    @Test
    fun verifyDeletedHistoryItemsCanNotBeSearchedTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)
        val thirdWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(thirdWebPage.url) {
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            clickDeleteHistoryButton(firstWebPage.title)
            verifyHistoryItemExists(false, firstWebPage.title)
            clickDeleteHistoryButton(secondWebPage.title)
            verifyHistoryItemExists(false, secondWebPage.title)
            clickSearchButton()
            searchForHistoryItem("generic")
            verifySearchedHistoryItemExists(firstWebPage.url.toString(), false)
            verifySearchedHistoryItemExists(secondWebPage.url.toString(), false)
            verifySearchedHistoryItemExists(thirdWebPage.url.toString(), true)
            dismissHistorySearchBarUsingBackButton()
            clickDeleteHistoryButton(thirdWebPage.title)
            verifyHistoryItemExists(false, firstWebPage.title)
            clickSearchButton()
            searchForHistoryItem("generic")
            verifySearchedHistoryItemExists(thirdWebPage.url.toString(), false)
        }
    }
}
