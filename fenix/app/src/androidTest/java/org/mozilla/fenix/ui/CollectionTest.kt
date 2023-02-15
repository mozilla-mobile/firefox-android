/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.util.Log
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.collectionRobot
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.tabDrawer

/**
 *  Tests for verifying basic functionality of tab collections
 *
 */

class CollectionTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val firstCollectionName = "testcollection_1"
    private val secondCollectionName = "testcollection_2"
    private val collectionName = "First Collection"

    @get:Rule
    val composeTestRule =
        AndroidComposeTestRule(
            HomeActivityIntentTestRule(
                isHomeOnboardingDialogEnabled = false,
                isJumpBackInCFREnabled = false,
                isRecentTabsFeatureEnabled = false,
                isRecentlyVisitedFeatureEnabled = false,
                isPocketEnabled = false,
                isWallpaperOnboardingEnabled = false,
                isTCPCFREnabled = false,
            ),
        ) { it.activity }

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
    }

    @SmokeTest
    @Test
    fun createFirstCollectionTest() {
        val firstWebPage = getGenericAsset(mockWebServer, 1)
        val secondWebPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
            mDevice.waitForIdle()
        }.goToHomescreen {
        }.clickSaveTabsToCollectionButton {
            longClickTab(firstWebPage.title)
            selectTab(secondWebPage.title, numOfTabs = 2)
        }.clickSaveCollection {
            typeCollectionNameAndSave(collectionName)
        }

        tabDrawer {
            verifySnackBarText("Collection saved!")
            snackBarButtonClick("VIEW")
        }

        homeScreen {
            verifyCollectionIsDisplayed(collectionName)
        }
    }

    @SmokeTest
    @Test
    fun verifyExpandedCollectionItemsTest() {
        val webPage = getGenericAsset(mockWebServer, 1)
        val webPageUrl = webPage.url.host.toString()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
            Log.i("Andi", "Loaded ${webPage.title}")
        }.openTabDrawer {
            Log.i("Andi", "Opened tab drawer")
            createCollection(webPage.title, collectionName = collectionName)
            snackBarButtonClick("VIEW")
            Log.i("Andi", "Clicked snack bar VIEW button")
        }

        homeScreen {
            verifyCollectionIsDisplayed(collectionName)
            Log.i("Andi", "Verified collection is displayed on the home screen")
        }.expandCollection(collectionName, composeTestRule) {
            Log.i("Andi", "Expanded collection")
            verifyTabSavedInCollection(webPage.title)
            Log.i("Andi", "Verified tab: ${webPage.title} is displayed in collection")
            verifyCollectionTabUrl(true, webPageUrl)
            Log.i("Andi", "Verified tab url: $webPageUrl is displayed in collection")
            verifyShareCollectionButtonIsVisible(true)
            Log.i("Andi", "Verified collection share button")
            verifyCollectionMenuIsVisible(true, composeTestRule)
            Log.i("Andi", "Verified collection menu is visible")
            verifyCollectionItemRemoveButtonIsVisible(webPage.title, true)
            Log.i("Andi", "Verified collection remove button is visible")
        }.collapseCollection(collectionName) {
            Log.i("Andi", "Collapsed collection")
        }

        collectionRobot {
            verifyTabSavedInCollection(webPage.title, false)
            Log.i("Andi", "Verified tab: ${webPage.title} is NOT displayed in collection")
            verifyShareCollectionButtonIsVisible(false)
            Log.i("Andi", "Verified collection share button is NOT displayed")
            verifyCollectionMenuIsVisible(false, composeTestRule)
            Log.i("Andi", "Verified collection menu is NOT displayed")
            verifyCollectionTabUrl(false, webPageUrl)
            Log.i("Andi", "Verified tab url: $webPageUrl is NOT displayed in collection")
            verifyCollectionItemRemoveButtonIsVisible(webPage.title, false)
            Log.i("Andi", "Verified collection remove button is NOT visible")
        }

        homeScreen {
            verifyCollectionIsDisplayed(collectionName)
            Log.i("Andi", "Verified collection is displayed on the home screen")
        }.expandCollection(collectionName, composeTestRule) {
            Log.i("Andi", "Expanded again the collection")
            verifyTabSavedInCollection(webPage.title)
            Log.i("Andi", "Verified tab: ${webPage.title} is displayed in collection")
            verifyCollectionTabUrl(true, webPageUrl)
            Log.i("Andi", "Verified tab url: $webPageUrl is displayed in collection")
            verifyShareCollectionButtonIsVisible(true)
            Log.i("Andi", "Verified collection share button")
            verifyCollectionMenuIsVisible(true, composeTestRule)
            Log.i("Andi", "Verified collection menu is visible")
            verifyCollectionItemRemoveButtonIsVisible(webPage.title, true)
            Log.i("Andi", "Verified collection remove button is visible")
        }.collapseCollection(collectionName) {
            Log.i("Andi", "Collapsed again the collection")
        }

        collectionRobot {
            verifyTabSavedInCollection(webPage.title, false)
            Log.i("Andi", "Verified tab: ${webPage.title} is NOT displayed in collection")
            verifyShareCollectionButtonIsVisible(false)
            Log.i("Andi", "Verified collection share button is NOT displayed")
            verifyCollectionMenuIsVisible(false, composeTestRule)
            Log.i("Andi", "Verified collection menu is NOT displayed")
            verifyCollectionTabUrl(false, webPageUrl)
            Log.i("Andi", "Verified tab url: $webPageUrl is NOT displayed in collection")
            verifyCollectionItemRemoveButtonIsVisible(webPage.title, false)
            Log.i("Andi", "Verified collection remove button is NOT visible")
        }
    }

    @SmokeTest
    @Test
    fun openAllTabsInCollectionTest() {
        val firstTestPage = getGenericAsset(mockWebServer, 1)
        val secondTestPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstTestPage.url) {
            waitForPageToLoad()
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondTestPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer {
            createCollection(
                firstTestPage.title,
                secondTestPage.title,
                collectionName = collectionName,
            )
            closeTab()
        }

        homeScreen {
        }.expandCollection(collectionName, composeTestRule) {
            clickCollectionThreeDotButton(composeTestRule)
            selectOpenTabs(composeTestRule)
        }
        tabDrawer {
            verifyExistingOpenTabs(firstTestPage.title, secondTestPage.title)
        }
    }

    @SmokeTest
    @Test
    fun shareCollectionTest() {
        val firstWebsite = getGenericAsset(mockWebServer, 1)
        val secondWebsite = getGenericAsset(mockWebServer, 2)
        val sharingApp = "Gmail"
        val urlString = "${secondWebsite.url}\n\n${firstWebsite.url}"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebsite.url) {
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebsite.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer {
            createCollection(firstWebsite.title, secondWebsite.title, collectionName = collectionName)
            verifySnackBarText("Collection saved!")
        }.openTabsListThreeDotMenu {
        }.closeAllTabs {
        }.expandCollection(collectionName, composeTestRule) {
        }.clickShareCollectionButton {
            verifyShareTabsOverlay(firstWebsite.title, secondWebsite.title)
            verifySharingWithSelectedApp(sharingApp, urlString, collectionName)
        }
    }

    // Test running on beta/release builds in CI:
    // caution when making changes to it, so they don't block the builds
    @SmokeTest
    @Test
    fun deleteCollectionTest() {
        val webPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
        }.openTabDrawer {
            createCollection(webPage.title, collectionName = collectionName)
            snackBarButtonClick("VIEW")
        }

        homeScreen {
        }.expandCollection(collectionName, composeTestRule) {
            clickCollectionThreeDotButton(composeTestRule)
            selectDeleteCollection(composeTestRule)
        }

        homeScreen {
            verifySnackBarText("Collection deleted")
            verifyNoCollectionsText()
        }
    }

    // open a webpage, and add currently opened tab to existing collection
    @Test
    fun mainMenuSaveToExistingCollection() {
        val firstWebPage = getGenericAsset(mockWebServer, 1)
        val secondWebPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openTabDrawer {
            createCollection(firstWebPage.title, collectionName = collectionName)
            verifySnackBarText("Collection saved!")
        }.closeTabDrawer {}

        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            verifyPageContent(secondWebPage.content)
        }.openThreeDotMenu {
        }.openSaveToCollection {
        }.selectExistingCollection(collectionName) {
            verifySnackBarText("Tab saved!")
        }.goToHomescreen {
        }.expandCollection(collectionName, composeTestRule) {
            verifyTabSavedInCollection(firstWebPage.title)
            verifyTabSavedInCollection(secondWebPage.title)
        }
    }

    @Test
    fun verifyAddTabButtonOfCollectionMenu() {
        val firstWebPage = getGenericAsset(mockWebServer, 1)
        val secondWebPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openTabDrawer {
            createCollection(firstWebPage.title, collectionName = collectionName)
            verifySnackBarText("Collection saved!")
            closeTab()
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
        }.goToHomescreen {
        }.expandCollection(collectionName, composeTestRule) {
            clickCollectionThreeDotButton(composeTestRule)
            selectAddTabToCollection(composeTestRule)
            verifyTabsSelectedCounterText(1)
            saveTabsSelectedForCollection()
            verifySnackBarText("Tab saved!")
            verifyTabSavedInCollection(secondWebPage.title)
        }
    }

    @Test
    fun renameCollectionTest() {
        val webPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
            Log.i("Andi", "Loaded ${webPage.title}")
        }.openTabDrawer {
            Log.i("Andi", "Opened tab drawer")
            createCollection(webPage.title, collectionName = firstCollectionName)
            verifySnackBarText("Collection saved!")
            Log.i("Andi", "Verified Collection saved! snackbar")
        }.closeTabDrawer {
            Log.i("Andi", "Closed tabs tray")
        }.goToHomescreen {
            Log.i("Andi", "Clicked go to home toolbar button")
            verifyCollectionIsDisplayed(firstCollectionName)
            Log.i("Andi", "Verified collection: $firstCollectionName is displayed on the home screen")
        }.expandCollection(firstCollectionName, composeTestRule) {
            Log.i("Andi", "Expanded collection")
            clickCollectionThreeDotButton(composeTestRule)
            Log.i("Andi", "Clicked collection 3 dot button")
            selectRenameCollection(composeTestRule)
            Log.i("Andi", "Clicked rename button")
        }.typeCollectionNameAndSave(secondCollectionName) {
            Log.i("Andi", "Typed collection name: $secondCollectionName, and save")
        }
        homeScreen {
            verifyCollectionIsDisplayed(secondCollectionName)
            Log.i("Andi", "Verified collection: $secondCollectionName is displayed on the home screen")
        }
    }

    @Test
    fun createSecondCollectionTest() {
        val webPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
        }.openTabDrawer {
            createCollection(webPage.title, collectionName = firstCollectionName)
            verifySnackBarText("Collection saved!")
            createCollection(
                webPage.title,
                collectionName = secondCollectionName,
                firstCollection = false,
            )
            verifySnackBarText("Collection saved!")
        }.closeTabDrawer {
        }.goToHomescreen {
            verifyCollectionIsDisplayed(firstCollectionName)
            verifyCollectionIsDisplayed(secondCollectionName)
        }
    }

    @Test
    fun removeTabFromCollectionTest() {
        val webPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
        }.openTabDrawer {
            createCollection(webPage.title, collectionName = collectionName)
            closeTab()
        }

        homeScreen {
        }.expandCollection(collectionName, composeTestRule) {
            verifyTabSavedInCollection(webPage.title, true)
            removeTabFromCollection(webPage.title)
            verifyTabSavedInCollection(webPage.title, false)
        }
        homeScreen {
            verifyCollectionIsDisplayed(collectionName, false)
        }
    }

    @Test
    fun swipeLeftToRemoveTabFromCollectionTest() {
        val testPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.url) {
            waitForPageToLoad()
        }.openTabDrawer {
            createCollection(
                testPage.title,
                collectionName = collectionName,
            )
            closeTab()
        }

        homeScreen {
        }.expandCollection(collectionName, composeTestRule) {
            swipeToBottom()
            swipeTabLeft(testPage.title, composeTestRule)
            verifyTabSavedInCollection(testPage.title, false)
        }
        homeScreen {
            verifyCollectionIsDisplayed(collectionName, false)
        }
    }

    @Test
    fun swipeRightToRemoveTabFromCollectionTest() {
        val testPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.url) {
            waitForPageToLoad()
        }.openTabDrawer {
            createCollection(
                testPage.title,
                collectionName = collectionName,
            )
            closeTab()
        }

        homeScreen {
        }.expandCollection(collectionName, composeTestRule) {
            swipeToBottom()
            swipeTabRight(testPage.title, composeTestRule)
            verifyTabSavedInCollection(testPage.title, false)
        }
        homeScreen {
            verifyCollectionIsDisplayed(collectionName, false)
        }
    }

    @Test
    @Ignore("Failing after compose migration. See: https://github.com/mozilla-mobile/fenix/issues/26087")
    fun selectTabOnLongTapTest() {
        val firstWebPage = getGenericAsset(mockWebServer, 1)
        val secondWebPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            waitForPageToLoad()
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer {
            verifyExistingOpenTabs(firstWebPage.title, secondWebPage.title)
            longClickTab(firstWebPage.title)
            verifyTabsMultiSelectionCounter(1)
            selectTab(secondWebPage.title, numOfTabs = 2)
        }.clickSaveCollection {
            typeCollectionNameAndSave(collectionName)
            verifySnackBarText("Tabs saved!")
        }

        tabDrawer {
        }.closeTabDrawer {
        }.goToHomescreen {
        }.expandCollection(collectionName, composeTestRule) {
            verifyTabSavedInCollection(firstWebPage.title)
            verifyTabSavedInCollection(secondWebPage.title)
        }
    }

    @Test
    fun navigateBackInCollectionFlowTest() {
        val webPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
        }.openTabDrawer {
            createCollection(webPage.title, collectionName = collectionName)
            verifySnackBarText("Collection saved!")
        }.closeTabDrawer {
        }.openThreeDotMenu {
        }.openSaveToCollection {
            verifySelectCollectionScreen()
            goBackInCollectionFlow()
        }

        browserScreen {
        }.openThreeDotMenu {
        }.openSaveToCollection {
            verifySelectCollectionScreen()
            clickAddNewCollection()
            verifyCollectionNameTextField()
            goBackInCollectionFlow()
            verifySelectCollectionScreen()
            goBackInCollectionFlow()
        }
        // verify the browser layout is visible
        browserScreen {
            verifyMenuButton()
        }
    }

    @SmokeTest
    @Test
    fun undoDeleteCollectionTest() {
        val webPage = getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
        }.openTabDrawer {
            createCollection(webPage.title, collectionName = collectionName)
            snackBarButtonClick("VIEW")
        }

        homeScreen {
        }.expandCollection(collectionName, composeTestRule) {
            clickCollectionThreeDotButton(composeTestRule)
            selectDeleteCollection(composeTestRule)
        }

        homeScreen {
            verifySnackBarText("Collection deleted")
            clickUndoSnackBarButton()
            verifyCollectionIsDisplayed(collectionName, true)
        }
    }
}
