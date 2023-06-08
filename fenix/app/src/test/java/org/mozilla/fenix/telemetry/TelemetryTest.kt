/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.telemetry

import android.content.Context
import android.content.DialogInterface
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.test.core.app.ApplicationProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.coroutines.test.advanceUntilIdle
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.NewCreditCardFields
import mozilla.components.concept.storage.UpdatableCreditCardFields
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.pocket.PocketStory
import mozilla.components.service.pocket.ext.getCurrentFlightImpressions
import mozilla.components.service.sync.autofill.AutofillCreditCardsAddressesStorage
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import mozilla.components.support.utils.CreditCardNetworkType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.BookmarksManagement
import org.mozilla.fenix.GleanMetrics.Collections
import org.mozilla.fenix.GleanMetrics.CreditCards
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.History
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.Pocket
import org.mozilla.fenix.GleanMetrics.SearchShortcuts
import org.mozilla.fenix.GleanMetrics.SyncAuth
import org.mozilla.fenix.GleanMetrics.TabsTray
import org.mozilla.fenix.GleanMetrics.UnifiedSearch
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.collections.DefaultCollectionCreationController
import org.mozilla.fenix.collections.Tab
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.Core
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.TelemetryAccountObserver
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.pocket.DefaultPocketStoriesController
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesCategory
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesSelectedCategory
import org.mozilla.fenix.home.toolbar.DefaultToolbarController
import org.mozilla.fenix.library.bookmarks.BookmarkFragmentInteractor
import org.mozilla.fenix.library.bookmarks.DefaultBookmarkController
import org.mozilla.fenix.library.history.DefaultHistoryInteractor
import org.mozilla.fenix.library.history.HistoryController
import org.mozilla.fenix.search.SearchDialogController
import org.mozilla.fenix.search.SearchDialogFragmentStore
import org.mozilla.fenix.search.SearchFragmentAction
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.settings.creditcards.controller.DefaultCreditCardEditorController
import org.mozilla.fenix.tabstray.DefaultNavigationInteractor
import org.mozilla.fenix.tabstray.TabsTrayAction
import org.mozilla.fenix.tabstray.TabsTrayFragmentDirections
import org.mozilla.fenix.tabstray.TabsTrayMiddleware
import org.mozilla.fenix.tabstray.TabsTrayState
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.utils.Settings

@RunWith(FenixRobolectricTestRunner::class)
class TelemetryTest {
    private lateinit var store: TabsTrayStore

    private lateinit var browserStore: BrowserStore
    private lateinit var bookmarkFragmentInteractor: BookmarkFragmentInteractor
    private lateinit var registry: ObserverRegistry<AccountObserver>
    private lateinit var defaultCollectionCreationController: DefaultCollectionCreationController
    private lateinit var defaultToolbarController: DefaultToolbarController
    private lateinit var defaultCreditCardEditorController: DefaultCreditCardEditorController
    private lateinit var searchDialogController: SearchDialogController

    private val tabCollectionStorage: TabCollectionStorage = mockk(relaxed = true)
    private val bookmarkController: DefaultBookmarkController = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val accountManager: FxaAccountManager = mockk(relaxed = true)
    private val homeActivity: HomeActivity = mockk(relaxed = true)
    private val context: Context = mockk()
    private val settings: Settings = mockk()
    private val mockComponents: Components = mockk()
    private val nimbus: NimbusApi = mockk()
    private val storage: AutofillCreditCardsAddressesStorage = mockk(relaxed = true)
    private val showDeleteDialog = mockk<(DialogInterface.OnClickListener) -> Unit>()
    private val searchDialogFragmentStore: SearchDialogFragmentStore = mockk(relaxed = true)
    private val tabsTrayMiddleware: TabsTrayMiddleware = TabsTrayMiddleware()
    private val testTab = createTab(url = "https://mozilla.org")
    private val bookmarkItem = BookmarkNode(BookmarkNodeType.ITEM, "456", "123", 0u, "Mozilla", "http://mozilla.org", 0, null)
    private val searchEngine = SearchEngine(
        id = "test",
        name = "Test Engine",
        icon = mockk(relaxed = true),
        type = SearchEngine.Type.BUNDLED,
        resultUrls = listOf("https://example.org/?q={searchTerms}"),
    )

    private var dismissed = false
    var focusToolbarInvoked = false

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @get:Rule
    val gleanRule = GleanTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        every { homeActivity.settings() } returns settings
        every { context.components } returns mockComponents
        every { mockComponents.settings } returns settings
        every { mockComponents.analytics } returns mockk {
            every { experiments } returns nimbus
        }
        every { nimbus.recordEvent(any()) } returns Unit
        every { settings.toolbarPosition } returns ToolbarPosition.BOTTOM
        every { settings.signedInFxaAccount = any() } just Runs

        every { searchDialogFragmentStore.state.tabId } returns "test-tab-id"
        every { searchDialogFragmentStore.state.searchEngineSource.searchEngine } returns searchEngine

        val observer = TelemetryAccountObserver(context)
        registry = ObserverRegistry<AccountObserver>().apply { register(observer) }

        browserStore = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(testTab),
                search = SearchState(
                    regionSearchEngines = listOf(searchEngine),
                ),
            ),
        )

        store = TabsTrayStore(
            middlewares = listOf(tabsTrayMiddleware),
            initialState = TabsTrayState(),
        )

        bookmarkFragmentInteractor = BookmarkFragmentInteractor(bookmarksController = bookmarkController)

        defaultCollectionCreationController = DefaultCollectionCreationController(
            store = mockk(),
            browserStore,
            dismiss = {
                dismissed = true
            },
            tabCollectionStorage,
            scope = coroutinesTestRule.scope,
        )

        defaultToolbarController = DefaultToolbarController(
            activity = homeActivity,
            store = browserStore,
            navController = navController,
        )

        defaultCreditCardEditorController = spyk(
            DefaultCreditCardEditorController(
                storage = storage,
                lifecycleScope = coroutinesTestRule.scope,
                navController = navController,
                ioDispatcher = coroutinesTestRule.testDispatcher,
                showDeleteDialog = showDeleteDialog,
            ),
        )

        every { showDeleteDialog(any()) } answers {
            firstArg<DialogInterface.OnClickListener>().onClick(
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        }

        searchDialogController = SearchDialogController(
            activity = homeActivity,
            store = BrowserStore(),
            tabsUseCases = TabsUseCases(browserStore),
            fragmentStore = searchDialogFragmentStore,
            navController = navController,
            settings = settings,
            dismissDialog = { },
            clearToolbarFocus = { },
            focusToolbar = {
                focusToolbarInvoked = true
            },
            clearToolbar = { },
            dismissDialogAndGoBack = { },
        )
    }

    @After
    fun tearDown() {
    }

    // TabsTrayMiddlewareTest
    @Test
    fun `WHEN inactive tabs are updated THEN report the count of inactive tabs`() {
        assertNull(TabsTray.hasInactiveTabs.testGetValue())
        assertNull(Metrics.inactiveTabsCount.testGetValue())

        store.dispatch(TabsTrayAction.UpdateInactiveTabs(emptyList()))
        store.waitUntilIdle()

        assertNotNull(TabsTray.hasInactiveTabs.testGetValue())
        assertNotNull(Metrics.inactiveTabsCount.testGetValue())
        assertEquals(0L, Metrics.inactiveTabsCount.testGetValue())
    }

    @Test
    fun `WHEN multi select mode from menu is entered THEN relevant metrics are collected`() {
        assertNull(TabsTray.enterMultiselectMode.testGetValue())

        store.dispatch(TabsTrayAction.EnterSelectMode)
        store.waitUntilIdle()

        assertNotNull(TabsTray.enterMultiselectMode.testGetValue())
        val snapshot = TabsTray.enterMultiselectMode.testGetValue()!!
        assertEquals(1, snapshot.size)
        assertEquals("false", snapshot.single().extra?.getValue("tab_selected"))
    }

    @Test
    fun `WHEN multi select mode by long press is entered THEN relevant metrics are collected`() {
        assertNull(TabsTray.enterMultiselectMode.testGetValue())

        store.dispatch(TabsTrayAction.AddSelectTab(mockk()))
        store.waitUntilIdle()

        assertNotNull(TabsTray.enterMultiselectMode.testGetValue())
        val snapshot = TabsTray.enterMultiselectMode.testGetValue()!!
        assertEquals(1, snapshot.size)
        assertEquals("true", snapshot.single().extra?.getValue("tab_selected"))
    }

    // TabsTray NavigationInteractor tests
    @Test
    fun `onTabTrayDismissed calls dismissTabTray on DefaultNavigationInteractor`() {
        var dismissTabTrayInvoked = false

        assertNull(TabsTray.closed.testGetValue())

        val defaultNavigationInteractor = DefaultNavigationInteractor(
            browserStore,
            navController,
            dismissTabTray = {
                dismissTabTrayInvoked = true
            },
            dismissTabTrayAndNavigateHome = { },
            showCancelledDownloadWarning = { _, _, _ -> },
            accountManager,
        )

        defaultNavigationInteractor.onTabTrayDismissed()

        assertTrue(dismissTabTrayInvoked)
        assertNotNull(TabsTray.closed.testGetValue())
    }

    @Test
    fun `onOpenRecentlyClosedClicked calls navigation on DefaultNavigationInteractor`() {
        assertNull(Events.recentlyClosedTabsOpened.testGetValue())

        val defaultNavigationInteractor = DefaultNavigationInteractor(
            browserStore,
            navController,
            dismissTabTray = {},
            dismissTabTrayAndNavigateHome = { },
            showCancelledDownloadWarning = { _, _, _ -> },
            accountManager,
        )

        defaultNavigationInteractor.onOpenRecentlyClosedClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalRecentlyClosed()) }
        assertNotNull(Events.recentlyClosedTabsOpened.testGetValue())
    }

    // HistoryInteractorTest
    @Test
    fun onSearch() {
        val controller: HistoryController = mockk(relaxed = true)
        val interactor = DefaultHistoryInteractor(controller)

        assertNull(History.searchIconTapped.testGetValue())
        interactor.onSearch()

        verifyAll {
            controller.handleSearch()
        }
        assertNotNull(History.searchIconTapped.testGetValue())
    }

    // BookmarkFragmentInteractorTest
    @Test
    fun `open a bookmark item`() {
        bookmarkFragmentInteractor.open(bookmarkItem)

        verify { bookmarkController.handleBookmarkTapped(bookmarkItem) }
        assertNotNull(BookmarksManagement.open.testGetValue())
        assertEquals(1, BookmarksManagement.open.testGetValue()!!.size)
        assertNull(BookmarksManagement.open.testGetValue()!!.single().extra)
    }

    @Test
    fun `copy a bookmark item`() {
        bookmarkFragmentInteractor.onCopyPressed(bookmarkItem)

        verify { bookmarkController.handleCopyUrl(bookmarkItem) }
        assertNotNull(BookmarksManagement.copied.testGetValue())
        assertEquals(1, BookmarksManagement.copied.testGetValue()!!.size)
        assertNull(BookmarksManagement.copied.testGetValue()!!.single().extra)
    }

    @Test
    fun `share a bookmark item`() {
        bookmarkFragmentInteractor.onSharePressed(bookmarkItem)

        verify { bookmarkController.handleBookmarkSharing(bookmarkItem) }
        assertNotNull(BookmarksManagement.shared.testGetValue())
        assertEquals(1, BookmarksManagement.shared.testGetValue()!!.size)
        assertNull(BookmarksManagement.shared.testGetValue()!!.single().extra)
    }

    @Test
    fun `open a bookmark item in a new tab`() {
        bookmarkFragmentInteractor.onOpenInNormalTab(bookmarkItem)

        verify { bookmarkController.handleOpeningBookmark(bookmarkItem, BrowsingMode.Normal) }
        assertNotNull(BookmarksManagement.openInNewTab.testGetValue())
        assertEquals(1, BookmarksManagement.openInNewTab.testGetValue()!!.size)
        assertNull(BookmarksManagement.openInNewTab.testGetValue()!!.single().extra)
    }

    @Test
    fun `open a bookmark item in a private tab`() {
        bookmarkFragmentInteractor.onOpenInPrivateTab(bookmarkItem)

        verify { bookmarkController.handleOpeningBookmark(bookmarkItem, BrowsingMode.Private) }
        assertNotNull(BookmarksManagement.openInPrivateTab.testGetValue())
        assertEquals(1, BookmarksManagement.openInPrivateTab.testGetValue()!!.size)
        assertNull(BookmarksManagement.openInPrivateTab.testGetValue()!!.single().extra)
    }

    @Test
    fun `WHEN onSearch is called THEN call controller handleSearch`() {
        assertNull(BookmarksManagement.searchIconTapped.testGetValue())
        bookmarkFragmentInteractor.onSearch()

        verify {
            bookmarkController.handleSearch()
        }
        assertNotNull(BookmarksManagement.searchIconTapped.testGetValue())
    }

    // DefaultPocketStoriesControllerTest
    @Test
    fun `GIVEN a category is selected WHEN that same category is clicked THEN deselect it and record telemetry`() {
        val category1 = PocketRecommendedStoriesCategory("cat1", emptyList())
        val category2 = PocketRecommendedStoriesCategory("cat2", emptyList())
        val selections = listOf(PocketRecommendedStoriesSelectedCategory(category2.name))
        val store = spyk(
            AppStore(
                AppState(
                    pocketStoriesCategories = listOf(category1, category2),
                    pocketStoriesCategoriesSelections = selections,
                ),
            ),
        )

        val controller = DefaultPocketStoriesController(mockk(), store)
        assertNull(Pocket.homeRecsCategoryClicked.testGetValue())

        controller.handleCategoryClick(category2)
        verify(exactly = 0) { store.dispatch(AppAction.SelectPocketStoriesCategory(category2.name)) }
        verify { store.dispatch(AppAction.DeselectPocketStoriesCategory(category2.name)) }

        assertNotNull(Pocket.homeRecsCategoryClicked.testGetValue())
        val event = Pocket.homeRecsCategoryClicked.testGetValue()!!
        assertEquals(1, event.size)
        assertTrue(event.single().extra!!.containsKey("category_name"))
        assertEquals(category2.name, event.single().extra!!["category_name"])
        assertTrue(event.single().extra!!.containsKey("new_state"))
        assertEquals("deselected", event.single().extra!!["new_state"])
        assertTrue(event.single().extra!!.containsKey("selected_total"))
        assertEquals("1", event.single().extra!!["selected_total"])
    }

    @Test
    fun `GIVEN 8 categories are selected WHEN when a new one is clicked THEN the oldest selected is deselected before selecting the new one and record telemetry`() {
        val category1 = PocketRecommendedStoriesSelectedCategory(name = "cat1", selectionTimestamp = 111)
        val category2 = PocketRecommendedStoriesSelectedCategory(name = "cat2", selectionTimestamp = 222)
        val category3 = PocketRecommendedStoriesSelectedCategory(name = "cat3", selectionTimestamp = 333)
        val oldestSelectedCategory = PocketRecommendedStoriesSelectedCategory(name = "oldestSelectedCategory", selectionTimestamp = 0)
        val category4 = PocketRecommendedStoriesSelectedCategory(name = "cat4", selectionTimestamp = 444)
        val category5 = PocketRecommendedStoriesSelectedCategory(name = "cat5", selectionTimestamp = 555)
        val category6 = PocketRecommendedStoriesSelectedCategory(name = "cat6", selectionTimestamp = 678)
        val category7 = PocketRecommendedStoriesSelectedCategory(name = "cat7", selectionTimestamp = 890)
        val newSelectedCategory = PocketRecommendedStoriesSelectedCategory(name = "newSelectedCategory", selectionTimestamp = 654321)
        val store = spyk(
            AppStore(
                AppState(
                    pocketStoriesCategoriesSelections = listOf(
                        category1,
                        category2,
                        category3,
                        category4,
                        category5,
                        category6,
                        category7,
                        oldestSelectedCategory,
                    ),
                ),
            ),
        )

        val controller = DefaultPocketStoriesController(mockk(), store)
        assertNull(Pocket.homeRecsCategoryClicked.testGetValue())

        controller.handleCategoryClick(PocketRecommendedStoriesCategory(newSelectedCategory.name))

        verify { store.dispatch(AppAction.DeselectPocketStoriesCategory(oldestSelectedCategory.name)) }
        verify { store.dispatch(AppAction.SelectPocketStoriesCategory(newSelectedCategory.name)) }

        assertNotNull(Pocket.homeRecsCategoryClicked.testGetValue())
        val event = Pocket.homeRecsCategoryClicked.testGetValue()!!
        assertEquals(1, event.size)
        assertTrue(event.single().extra!!.containsKey("category_name"))
        assertEquals(newSelectedCategory.name, event.single().extra!!["category_name"])
        assertTrue(event.single().extra!!.containsKey("new_state"))
        assertEquals("selected", event.single().extra!!["new_state"])
        assertTrue(event.single().extra!!.containsKey("selected_total"))
        assertEquals("8", event.single().extra!!["selected_total"])
    }

    @Test
    fun `GIVEN fewer than 8 categories are selected WHEN when a new one is clicked THEN don't deselect anything but select the newly clicked category and record telemetry`() {
        val category1 = PocketRecommendedStoriesSelectedCategory(name = "cat1", selectionTimestamp = 111)
        val category2 = PocketRecommendedStoriesSelectedCategory(name = "cat2", selectionTimestamp = 222)
        val category3 = PocketRecommendedStoriesSelectedCategory(name = "cat3", selectionTimestamp = 333)
        val oldestSelectedCategory = PocketRecommendedStoriesSelectedCategory(name = "oldestSelectedCategory", selectionTimestamp = 0)
        val category4 = PocketRecommendedStoriesSelectedCategory(name = "cat4", selectionTimestamp = 444)
        val category5 = PocketRecommendedStoriesSelectedCategory(name = "cat5", selectionTimestamp = 555)
        val category6 = PocketRecommendedStoriesSelectedCategory(name = "cat6", selectionTimestamp = 678)
        val store = spyk(
            AppStore(
                AppState(
                    pocketStoriesCategoriesSelections = listOf(
                        category1,
                        category2,
                        category3,
                        category4,
                        category5,
                        category6,
                        oldestSelectedCategory,
                    ),
                ),
            ),
        )
        val newSelectedCategoryName = "newSelectedCategory"
        val controller = DefaultPocketStoriesController(mockk(), store)

        controller.handleCategoryClick(PocketRecommendedStoriesCategory(newSelectedCategoryName))

        verify(exactly = 0) { store.dispatch(AppAction.DeselectPocketStoriesCategory(oldestSelectedCategory.name)) }
        verify { store.dispatch(AppAction.SelectPocketStoriesCategory(newSelectedCategoryName)) }

        assertNotNull(Pocket.homeRecsCategoryClicked.testGetValue())
        val event = Pocket.homeRecsCategoryClicked.testGetValue()!!
        assertEquals(1, event.size)
        assertTrue(event.single().extra!!.containsKey("category_name"))
        assertEquals(newSelectedCategoryName, event.single().extra!!["category_name"])
        assertTrue(event.single().extra!!.containsKey("new_state"))
        assertEquals("selected", event.single().extra!!["new_state"])
        assertTrue(event.single().extra!!.containsKey("selected_total"))
        assertEquals("7", event.single().extra!!["selected_total"])
    }

    @Test
    fun `WHEN a new sponsored story is shown THEN update the State and record telemetry`() {
        val store = spyk(AppStore())
        val controller = DefaultPocketStoriesController(mockk(), store)
        val storyShown: PocketStory.PocketSponsoredStory = mockk {
            every { shim.click } returns "testClickShim"
            every { shim.impression } returns "testImpressionShim"
            every { id } returns 123
        }
        var wasPingSent = false
        mockkStatic("mozilla.components.service.pocket.ext.PocketStoryKt") {
            // Simulate that the story was already shown 3 times.
            every { storyShown.getCurrentFlightImpressions() } returns listOf(2L, 3L, 7L)
            // Test that the spoc ping is immediately sent with the needed data.
            Pings.spoc.testBeforeNextSubmit { reason ->
                assertEquals(storyShown.shim.impression, Pocket.spocShim.testGetValue())
                assertEquals(Pings.spocReasonCodes.impression.name, reason?.name)
                wasPingSent = true
            }

            controller.handleStoryShown(storyShown, 1 to 2)

            verify { store.dispatch(AppAction.PocketStoriesShown(listOf(storyShown))) }
            assertNotNull(Pocket.homeRecsSpocShown.testGetValue())
            assertEquals(1, Pocket.homeRecsSpocShown.testGetValue()!!.size)
            val data = Pocket.homeRecsSpocShown.testGetValue()!!.single().extra
            assertEquals("123", data?.entries?.first { it.key == "spoc_id" }?.value)
            assertEquals("1x2", data?.entries?.first { it.key == "position" }?.value)
            assertEquals("4", data?.entries?.first { it.key == "times_shown" }?.value)
            assertTrue(wasPingSent)
        }
    }

    @Test
    fun `WHEN new stories are shown THEN update the State and record telemetry`() {
        val store = spyk(AppStore())
        val controller = DefaultPocketStoriesController(mockk(), store)
        val storiesShown: List<PocketStory> = mockk()
        assertNull(Pocket.homeRecsShown.testGetValue())

        controller.handleStoriesShown(storiesShown)

        verify { store.dispatch(AppAction.PocketStoriesShown(storiesShown)) }
        assertNotNull(Pocket.homeRecsShown.testGetValue())
        assertEquals(1, Pocket.homeRecsShown.testGetValue()!!.size)
        assertNull(Pocket.homeRecsShown.testGetValue()!!.single().extra)
    }

    @Test
    fun `WHEN a recommended story is clicked THEN open that story's url using HomeActivity and record telemetry`() {
        val story = PocketStory.PocketRecommendedStory(
            title = "",
            url = "testLink",
            imageUrl = "",
            publisher = "",
            category = "",
            timeToRead = 0,
            timesShown = 123,
        )
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())
        assertNull(Pocket.homeRecsStoryClicked.testGetValue())

        controller.handleStoryClicked(story, 1 to 2)

        verify { homeActivity.openToBrowserAndLoad(story.url, true, BrowserDirection.FromHome) }

        assertNotNull(Pocket.homeRecsStoryClicked.testGetValue())
        val event = Pocket.homeRecsStoryClicked.testGetValue()!!
        assertEquals(1, event.size)
        assertTrue(event.single().extra!!.containsKey("position"))
        assertEquals("1x2", event.single().extra!!["position"])
        assertTrue(event.single().extra!!.containsKey("times_shown"))
        assertEquals(story.timesShown.inc().toString(), event.single().extra!!["times_shown"])
    }

    @Test
    fun `WHEN a sponsored story is clicked THEN open that story's url using HomeActivity and record telemetry`() {
        val storyClicked = PocketStory.PocketSponsoredStory(
            id = 7,
            title = "",
            url = "testLink",
            imageUrl = "",
            sponsor = "",
            shim = mockk {
                every { click } returns "testClickShim"
                every { impression } returns "testImpressionShim"
            },
            priority = 3,
            caps = mockk(relaxed = true),
        )
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())
        var wasPingSent = false
        assertNull(Pocket.homeRecsSpocClicked.testGetValue())
        mockkStatic("mozilla.components.service.pocket.ext.PocketStoryKt") {
            // Simulate that the story was already shown 2 times.
            every { storyClicked.getCurrentFlightImpressions() } returns listOf(2L, 3L)
            // Test that the spoc ping is immediately sent with the needed data.
            Pings.spoc.testBeforeNextSubmit { reason ->
                assertEquals(storyClicked.shim.click, Pocket.spocShim.testGetValue())
                assertEquals(Pings.spocReasonCodes.click.name, reason?.name)
                wasPingSent = true
            }

            controller.handleStoryClicked(storyClicked, 2 to 3)

            verify { homeActivity.openToBrowserAndLoad(storyClicked.url, true, BrowserDirection.FromHome) }
            assertNotNull(Pocket.homeRecsSpocClicked.testGetValue())
            assertEquals(1, Pocket.homeRecsSpocClicked.testGetValue()!!.size)
            val data = Pocket.homeRecsSpocClicked.testGetValue()!!.single().extra
            assertEquals("7", data?.entries?.first { it.key == "spoc_id" }?.value)
            assertEquals("2x3", data?.entries?.first { it.key == "position" }?.value)
            assertEquals("3", data?.entries?.first { it.key == "times_shown" }?.value)
            assertTrue(wasPingSent)
        }
    }

    @Test
    fun `WHEN discover more is clicked then open that using HomeActivity and record telemetry`() {
        val link = "http://getpocket.com/explore"
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())
        assertNull(Pocket.homeRecsDiscoverClicked.testGetValue())

        controller.handleDiscoverMoreClicked(link)

        verify { homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome) }
        assertNotNull(Pocket.homeRecsDiscoverClicked.testGetValue())
        assertEquals(1, Pocket.homeRecsDiscoverClicked.testGetValue()!!.size)
        assertNull(Pocket.homeRecsDiscoverClicked.testGetValue()!!.single().extra)
    }

    @Test
    fun `WHEN learn more is clicked then open that using HomeActivity and record telemetry`() {
        val link = "https://www.mozilla.org/en-US/firefox/pocket/"
        val homeActivity: HomeActivity = mockk(relaxed = true)
        val controller = DefaultPocketStoriesController(homeActivity, mockk())
        assertNull(Pocket.homeRecsLearnMoreClicked.testGetValue())

        controller.handleLearnMoreClicked(link)

        verify { homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome) }
        assertNotNull(Pocket.homeRecsLearnMoreClicked.testGetValue())
        assertNull(Pocket.homeRecsLearnMoreClicked.testGetValue()!!.single().extra)
    }

    // BackgroundServicesTest
    @Test
    fun `telemetry account observer tracks sign in event`() {
        val account = mockk<OAuthAccount>()

        registry.notifyObservers { onAuthenticated(account, AuthType.Signin) }
        assertEquals(1, SyncAuth.signIn.testGetValue()!!.size)
        assertEquals(null, SyncAuth.signIn.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `telemetry account observer tracks sign up event`() {
        val account = mockk<OAuthAccount>()

        registry.notifyObservers { onAuthenticated(account, AuthType.Signup) }
        assertEquals(1, SyncAuth.signUp.testGetValue()!!.size)
        assertEquals(null, SyncAuth.signUp.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `telemetry account observer tracks pairing event`() {
        val account = mockk<OAuthAccount>()

        registry.notifyObservers { onAuthenticated(account, AuthType.Pairing) }
        assertEquals(1, SyncAuth.paired.testGetValue()!!.size)
        assertEquals(null, SyncAuth.paired.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `telemetry account observer tracks recovered event`() {
        val account = mockk<OAuthAccount>()

        registry.notifyObservers { onAuthenticated(account, AuthType.Recovered) }
        assertEquals(1, SyncAuth.recovered.testGetValue()!!.size)
        assertEquals(null, SyncAuth.recovered.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `telemetry account observer tracks external creation event with null action`() {
        val account = mockk<OAuthAccount>()

        registry.notifyObservers { onAuthenticated(account, AuthType.OtherExternal(null)) }
        assertEquals(1, SyncAuth.otherExternal.testGetValue()!!.size)
        assertEquals(null, SyncAuth.otherExternal.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `telemetry account observer tracks external creation event with some action`() {
        val account = mockk<OAuthAccount>()

        registry.notifyObservers { onAuthenticated(account, AuthType.OtherExternal("someAction")) }
        assertEquals(1, SyncAuth.otherExternal.testGetValue()!!.size)
        assertEquals(null, SyncAuth.otherExternal.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `telemetry account observer tracks sign out event`() {
        registry.notifyObservers { onLoggedOut() }

        assertEquals(1, SyncAuth.signOut.testGetValue()!!.size)
        assertEquals(null, SyncAuth.signOut.testGetValue()!!.single().extra)
        verify { settings.signedInFxaAccount = false }
        confirmVerified(settings)
    }

    // DefaultCollectionCreationControllerTest
    @Test
    fun `GIVEN tab list WHEN saveCollectionName is called THEN collection should be created`() {
        val tab1 = createTab("https://www.mozilla.org", id = "session-1")
        val tab2 = createTab("https://www.mozilla.org", id = "session-2")

        // clear tabs from other tests
        browserStore.dispatch(
            TabListAction.RemoveAllTabsAction(),
        )

        browserStore.dispatch(
            TabListAction.AddMultipleTabsAction(listOf(tab1, tab2)),
        ).joinBlocking()

        coEvery { tabCollectionStorage.addTabsToCollection(any(), any()) } returns 1L
        coEvery { tabCollectionStorage.createCollection(any(), any()) } returns 1L

        val tabs = listOf(
            Tab("session-1", "", "", ""),
            Tab("null-session", "", "", ""),
        )

        defaultCollectionCreationController.saveCollectionName(tabs, "name")

        assertTrue(dismissed)
        coVerify { tabCollectionStorage.createCollection("name", listOf(tab1)) }

        assertNotNull(Collections.saved.testGetValue())
        val recordedEvents = Collections.saved.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        val eventExtra = recordedEvents.single().extra
        assertNotNull(eventExtra)
        assertTrue(eventExtra!!.containsKey("tabs_open"))
        assertEquals("2", eventExtra["tabs_open"])
        assertTrue(eventExtra.containsKey("tabs_selected"))
        assertEquals("1", eventExtra["tabs_selected"])
    }

    @Test
    fun `GIVEN collection WHEN renameCollection is called THEN collection should be renamed`() = runTestOnMain {
        val collection = mockk<TabCollection>()

        defaultCollectionCreationController.renameCollection(collection, "name")
        advanceUntilIdle()

        assertTrue(dismissed)

        assertNotNull(Collections.renamed.testGetValue())
        val recordedEvents = Collections.renamed.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        assertNull(recordedEvents.single().extra)

        coVerify { tabCollectionStorage.renameCollection(collection, "name") }
    }

    @Test
    fun `WHEN selectCollection is called THEN add tabs should be added to collection`() {
        val tab1 = createTab("https://www.mozilla.org", id = "session-1")
        val tab2 = createTab("https://www.mozilla.org", id = "session-2")

        // clear tabs from other tests
        browserStore.dispatch(
            TabListAction.RemoveAllTabsAction(),
        )

        browserStore.dispatch(
            TabListAction.AddMultipleTabsAction(listOf(tab1, tab2)),
        ).joinBlocking()

        val tabs = listOf(
            Tab("session-1", "", "", ""),
        )
        val collection = mockk<TabCollection>()
        coEvery { tabCollectionStorage.addTabsToCollection(any(), any()) } returns 1L
        coEvery { tabCollectionStorage.createCollection(any(), any()) } returns 1L

        defaultCollectionCreationController.selectCollection(collection, tabs)

        assertTrue(dismissed)
        coVerify { tabCollectionStorage.addTabsToCollection(collection, listOf(tab1)) }

        assertNotNull(Collections.tabsAdded.testGetValue())
        val recordedEvents = Collections.tabsAdded.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        val eventExtra = recordedEvents.single().extra
        assertNotNull(eventExtra)
        assertTrue(eventExtra!!.containsKey("tabs_open"))
        assertEquals("2", eventExtra["tabs_open"])
        assertTrue(eventExtra.containsKey("tabs_selected"))
        assertEquals("1", eventExtra["tabs_selected"])
    }

    // DefaultToolbarControllerTest
    @Test
    fun `WHEN Paste & Go toolbar menu is clicked with text in clipboard THEN record proper metrics`() {
        assertNull(Events.enteredUrl.testGetValue())
        assertNull(Events.performedSearch.testGetValue())

        var clipboardText = "text"
        defaultToolbarController.handlePasteAndGo(clipboardText)

        verify {
            homeActivity.openToBrowserAndLoad(
                searchTermOrURL = clipboardText,
                newTab = true,
                from = BrowserDirection.FromHome,
                engine = searchEngine,
            )
        }

        assertNotNull(Events.performedSearch.testGetValue())

        clipboardText = "https://mozilla.org"
        defaultToolbarController.handlePasteAndGo(clipboardText)

        verify {
            homeActivity.openToBrowserAndLoad(
                searchTermOrURL = clipboardText,
                newTab = true,
                from = BrowserDirection.FromHome,
                engine = searchEngine,
            )
        }

        assertNotNull(Events.enteredUrl.testGetValue())
    }

    @Test
    fun `WHEN the toolbar is tapped THEN navigate to the search dialog`() {
        assertNull(Events.searchBarTapped.testGetValue())

        defaultToolbarController.handleNavigateSearch()

        assertNotNull(Events.searchBarTapped.testGetValue())

        val recordedEvents = Events.searchBarTapped.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        assertEquals("HOME", recordedEvents.single().extra?.getValue("source"))

        verify {
            navController.navigate(
                match<NavDirections> { it.actionId == R.id.action_global_search_dialog },
                any<NavOptions>(),
            )
        }
    }

    // DefaultCreditCardEditorControllerTest
    @Test
    fun handleDeleteCreditCard() = runTestOnMain {
        val creditCardId = "id"
        assertNull(CreditCards.deleted.testGetValue())

        defaultCreditCardEditorController.handleDeleteCreditCard(creditCardId)

        coVerify {
            storage.deleteCreditCard(creditCardId)
            navController.popBackStack()
        }
        assertNotNull(CreditCards.deleted.testGetValue())
    }

    @Test
    fun handleSaveCreditCard() = runTestOnMain {
        val creditCardFields = NewCreditCardFields(
            billingName = "Banana Apple",
            plaintextCardNumber = CreditCardNumber.Plaintext("4111111111111112"),
            cardNumberLast4 = "1112",
            expiryMonth = 1,
            expiryYear = 2030,
            cardType = CreditCardNetworkType.DISCOVER.cardName,
        )
        assertNull(CreditCards.saved.testGetValue())

        defaultCreditCardEditorController.handleSaveCreditCard(creditCardFields)

        coVerify {
            storage.addCreditCard(creditCardFields)
            navController.popBackStack()
        }
        assertNotNull(CreditCards.saved.testGetValue())
    }

    @Test
    fun handleUpdateCreditCard() = runTestOnMain {
        val creditCardId = "id"
        val creditCardFields = UpdatableCreditCardFields(
            billingName = "Banana Apple",
            cardNumber = CreditCardNumber.Plaintext("4111111111111112"),
            cardNumberLast4 = "1112",
            expiryMonth = 1,
            expiryYear = 2034,
            cardType = CreditCardNetworkType.DISCOVER.cardName,
        )
        assertNull(CreditCards.modified.testGetValue())

        defaultCreditCardEditorController.handleUpdateCreditCard(creditCardId, creditCardFields)

        coVerify {
            storage.updateCreditCard(creditCardId, creditCardFields)
            navController.popBackStack()
        }
        assertNotNull(CreditCards.modified.testGetValue())
    }

    // SearchDialogControllerTest
    @Test
    fun `GIVEN default search engine is selected WHEN url is committed THEN load the url`() {
        val url = "https://www.google.com/"
        assertNull(Events.enteredUrl.testGetValue())

        every { searchDialogFragmentStore.state.defaultEngine } returns searchEngine

        searchDialogController.handleUrlCommitted(url)

        verify {
            homeActivity.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine,
                forceSearch = false,
            )
        }

        assertNotNull(Events.enteredUrl.testGetValue())
        val snapshot = Events.enteredUrl.testGetValue()!!
        assertEquals(1, snapshot.size)
        assertEquals("false", snapshot.single().extra?.getValue("autocomplete"))
    }

    @Test
    fun `GIVEN a general search engine is selected WHEN url is committed THEN perform search`() {
        val url = "https://www.google.com/"
        assertNull(Events.enteredUrl.testGetValue())

        every { searchDialogFragmentStore.state.defaultEngine } returns mockk(relaxed = true)

        searchDialogController.handleUrlCommitted(url)

        verify {
            homeActivity.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine,
                forceSearch = true,
            )
        }

        assertNotNull(Events.enteredUrl.testGetValue())
        val snapshot = Events.enteredUrl.testGetValue()!!
        assertEquals(1, snapshot.size)
        assertEquals("false", snapshot.single().extra?.getValue("autocomplete"))
    }

    @Test
    fun handleMozillaUrlCommitted() {
        val url = "moz://a"
        assertNull(Events.enteredUrl.testGetValue())

        every { searchDialogFragmentStore.state.defaultEngine } returns searchEngine

        searchDialogController.handleUrlCommitted(url)

        verify {
            homeActivity.openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.MANIFESTO),
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                engine = searchEngine,
            )
        }

        assertNotNull(Events.enteredUrl.testGetValue())
        val snapshot = Events.enteredUrl.testGetValue()!!
        assertEquals(1, snapshot.size)
        assertEquals("false", snapshot.single().extra?.getValue("autocomplete"))
    }

    @Test
    fun handleUrlTapped() {
        val url = "https://www.google.com/"
        val flags = EngineSession.LoadUrlFlags.all()
        assertNull(Events.enteredUrl.testGetValue())

        searchDialogController.handleUrlTapped(url, flags)

        verify {
            homeActivity.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = false,
                from = BrowserDirection.FromSearchDialog,
                flags = flags,
            )
        }

        assertNotNull(Events.enteredUrl.testGetValue())

        searchDialogController.handleUrlTapped(url)

        assertNotNull(Events.enteredUrl.testGetValue())
        val snapshot = Events.enteredUrl.testGetValue()!!
        assertEquals(2, snapshot.size)
        assertEquals("false", snapshot.first().extra?.getValue("autocomplete"))
        assertEquals("false", snapshot[1].extra?.getValue("autocomplete"))
    }

    @Test
    fun handleSearchShortcutEngineSelected() {
        val browsingMode = BrowsingMode.Private
        every { homeActivity.browsingModeManager.mode } returns browsingMode
        every { settings.showUnifiedSearchFeature } returns false

        searchDialogController.handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)

        verify {
            searchDialogFragmentStore.dispatch(
                SearchFragmentAction.SearchShortcutEngineSelected(
                    searchEngine,
                    browsingMode,
                    settings,
                ),
            )
        }

        assertNotNull(SearchShortcuts.selected.testGetValue())
        val recordedEvents = SearchShortcuts.selected.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        val eventExtra = recordedEvents.single().extra
        assertNotNull(eventExtra)
        assertTrue(eventExtra!!.containsKey("engine"))
        assertEquals(searchEngine.name, eventExtra["engine"])
    }

    @Test
    fun `WHEN history search engine is selected THEN dispatch correct action`() {
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION
        every { searchEngine.id } returns Core.HISTORY_SEARCH_ENGINE_ID
        every { settings.showUnifiedSearchFeature } returns true

        assertNull(UnifiedSearch.engineSelected.testGetValue())

        searchDialogController.handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)

        verify {
            searchDialogFragmentStore.dispatch(
                SearchFragmentAction.SearchHistoryEngineSelected(
                    searchEngine,
                ),
            )
        }

        assertNotNull(UnifiedSearch.engineSelected.testGetValue())
        val recordedEvents = UnifiedSearch.engineSelected.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        val eventExtra = recordedEvents.single().extra
        assertNotNull(eventExtra)
        assertTrue(eventExtra!!.containsKey("engine"))
        assertEquals("history", eventExtra["engine"])
    }

    @Test
    fun `WHEN bookmarks search engine is selected THEN dispatch correct action`() {
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION
        every { searchEngine.id } returns Core.BOOKMARKS_SEARCH_ENGINE_ID
        every { settings.showUnifiedSearchFeature } returns true

        assertNull(UnifiedSearch.engineSelected.testGetValue())

        searchDialogController.handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)

        verify {
            searchDialogFragmentStore.dispatch(
                SearchFragmentAction.SearchBookmarksEngineSelected(
                    searchEngine,
                ),
            )
        }

        assertNotNull(UnifiedSearch.engineSelected.testGetValue())
        val recordedEvents = UnifiedSearch.engineSelected.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        val eventExtra = recordedEvents.single().extra
        assertNotNull(eventExtra)
        assertTrue(eventExtra!!.containsKey("engine"))
        assertEquals("bookmarks", eventExtra["engine"])
    }

    @Test
    fun `WHEN tabs search engine is selected THEN dispatch correct action`() {
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.type } returns SearchEngine.Type.APPLICATION
        every { searchEngine.id } returns Core.TABS_SEARCH_ENGINE_ID
        every { settings.showUnifiedSearchFeature } returns true

        assertNull(UnifiedSearch.engineSelected.testGetValue())

        searchDialogController.handleSearchShortcutEngineSelected(searchEngine)

        assertTrue(focusToolbarInvoked)

        verify {
            searchDialogFragmentStore.dispatch(
                SearchFragmentAction.SearchTabsEngineSelected(
                    searchEngine,
                ),
            )
        }

        assertNotNull(UnifiedSearch.engineSelected.testGetValue())
        val recordedEvents = UnifiedSearch.engineSelected.testGetValue()!!
        assertEquals(1, recordedEvents.size)
        val eventExtra = recordedEvents.single().extra
        assertNotNull(eventExtra)
        assertTrue(eventExtra!!.containsKey("engine"))
        assertEquals("tabs", eventExtra["engine"])
    }
}
