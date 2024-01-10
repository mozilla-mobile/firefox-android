/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import androidx.annotation.VisibleForTesting
import mozilla.components.service.pocket.PocketStory.PocketRecommendedStory
import mozilla.components.service.pocket.PocketStory.PocketSponsoredStory
import mozilla.components.service.pocket.ext.recordNewImpression
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.home.TabsTrayReducer
import org.mozilla.fenix.components.appstate.home.ToolbarReducer
import org.mozilla.fenix.components.appstate.home.WallpapersReducer
import org.mozilla.fenix.components.appstate.shopping.ShoppingStateReducer
import org.mozilla.fenix.ext.filterOutTab
import org.mozilla.fenix.ext.getFilteredStories
import org.mozilla.fenix.home.intent.IntentReducer
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesSelectedCategory
import org.mozilla.fenix.home.recentsyncedtabs.RecentSyncedTabState
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.messaging.state.MessagingReducer

/**
 * Reducer for [AppStore].
 */
internal object AppStoreReducer {
    @Suppress("LongMethod")
    fun reduce(state: AppState, action: AppAction): AppState = when (action) {
        is AppAction.Init -> state
        is AppAction.OpenTabInBrowser -> state.copy(mode = action.mode)
        is AppAction.BrowsingModeLoaded -> state.copy(mode = action.mode)
        is AppAction.UpdateInactiveExpanded ->
            state.copy(inactiveTabsExpanded = action.expanded)
        is AppAction.UpdateFirstFrameDrawn -> {
            state.copy(firstFrameDrawn = action.drawn)
        }
        is AppAction.AddNonFatalCrash ->
            state.copy(nonFatalCrashes = state.nonFatalCrashes + action.crash)
        is AppAction.RemoveNonFatalCrash ->
            state.copy(nonFatalCrashes = state.nonFatalCrashes - action.crash)
        is AppAction.RemoveAllNonFatalCrashes ->
            state.copy(nonFatalCrashes = emptyList())

        is AppAction.MessagingAction -> MessagingReducer.reduce(state, action)

        is AppAction.Change -> state.copy(
            collections = action.collections,
            mode = action.mode,
            topSites = action.topSites,
            recentBookmarks = action.recentBookmarks,
            recentTabs = action.recentTabs,
            recentHistory = action.recentHistory,
            recentSyncedTabState = action.recentSyncedTabState,
        )
        is AppAction.CollectionExpanded -> {
            val newExpandedCollection = state.expandedCollections.toMutableSet()

            if (action.expand) {
                newExpandedCollection.add(action.collection.id)
            } else {
                newExpandedCollection.remove(action.collection.id)
            }

            state.copy(expandedCollections = newExpandedCollection)
        }
        is AppAction.CollectionsChange -> state.copy(collections = action.collections)
        is AppAction.ModeChange -> state.copy(mode = action.mode)
        is AppAction.TopSitesChange -> state.copy(topSites = action.topSites)
        is AppAction.RemoveCollectionsPlaceholder -> {
            state.copy(showCollectionPlaceholder = false)
        }
        is AppAction.RecentTabsChange -> {
            state.copy(
                recentTabs = action.recentTabs,
                recentHistory = state.recentHistory,
            )
        }
        is AppAction.RemoveRecentTab -> {
            state.copy(
                recentTabs = state.recentTabs.filterOutTab(action.recentTab),
            )
        }
        is AppAction.RecentSyncedTabStateChange -> {
            state.copy(
                recentSyncedTabState = action.state,
            )
        }
        is AppAction.RecentBookmarksChange -> state.copy(recentBookmarks = action.recentBookmarks)
        is AppAction.RemoveRecentBookmark -> {
            state.copy(recentBookmarks = state.recentBookmarks.filterNot { it.url == action.recentBookmark.url })
        }
        is AppAction.RecentHistoryChange -> state.copy(
            recentHistory = action.recentHistory,
        )
        is AppAction.RemoveRecentHistoryHighlight -> state.copy(
            recentHistory = state.recentHistory.filterNot {
                it is RecentlyVisitedItem.RecentHistoryHighlight && it.url == action.highlightUrl
            },
        )
        is AppAction.RemoveRecentSyncedTab -> state.copy(
            recentSyncedTabState = when (state.recentSyncedTabState) {
                is RecentSyncedTabState.Success -> RecentSyncedTabState.Success(
                    state.recentSyncedTabState.tabs - action.syncedTab,
                )
                else -> state.recentSyncedTabState
            },
        )
        is AppAction.SelectedTabChanged -> state.copy(
            selectedTabId = action.tab.id,
            mode = BrowsingMode.fromBoolean(action.tab.content.private),
        )
        is AppAction.DisbandSearchGroupAction -> state.copy(
            recentHistory = state.recentHistory.filterNot {
                it is RecentHistoryGroup && it.title.equals(action.searchTerm, true)
            },
        )
        is AppAction.SelectPocketStoriesCategory -> {
            val updatedCategoriesState = state.copy(
                pocketStoriesCategoriesSelections =
                state.pocketStoriesCategoriesSelections + PocketRecommendedStoriesSelectedCategory(
                    name = action.categoryName,
                ),
            )

            // Selecting a category means the stories to be displayed needs to also be changed.
            updatedCategoriesState.copy(
                pocketStories = updatedCategoriesState.getFilteredStories(),
            )
        }
        is AppAction.DeselectPocketStoriesCategory -> {
            val updatedCategoriesState = state.copy(
                pocketStoriesCategoriesSelections = state.pocketStoriesCategoriesSelections.filterNot {
                    it.name == action.categoryName
                },
            )

            // Deselecting a category means the stories to be displayed needs to also be changed.
            updatedCategoriesState.copy(
                pocketStories = updatedCategoriesState.getFilteredStories(),
            )
        }
        is AppAction.PocketStoriesCategoriesChange -> {
            val updatedCategoriesState =
                state.copy(pocketStoriesCategories = action.storiesCategories)
            // Whenever categories change stories to be displayed needs to also be changed.
            updatedCategoriesState.copy(
                pocketStories = updatedCategoriesState.getFilteredStories(),
            )
        }
        is AppAction.PocketStoriesCategoriesSelectionsChange -> {
            val updatedCategoriesState = state.copy(
                pocketStoriesCategories = action.storiesCategories,
                pocketStoriesCategoriesSelections = action.categoriesSelected,
            )
            // Whenever categories change stories to be displayed needs to also be changed.
            updatedCategoriesState.copy(
                pocketStories = updatedCategoriesState.getFilteredStories(),
            )
        }
        is AppAction.PocketStoriesClean -> state.copy(
            pocketStoriesCategories = emptyList(),
            pocketStoriesCategoriesSelections = emptyList(),
            pocketStories = emptyList(),
            pocketSponsoredStories = emptyList(),
        )
        is AppAction.PocketSponsoredStoriesChange -> {
            val updatedStoriesState = state.copy(
                pocketSponsoredStories = action.sponsoredStories,
            )

            updatedStoriesState.copy(
                pocketStories = updatedStoriesState.getFilteredStories(),
            )
        }
        is AppAction.PocketStoriesShown -> {
            var updatedCategories = state.pocketStoriesCategories
            action.storiesShown.filterIsInstance<PocketRecommendedStory>().forEach { shownStory ->
                updatedCategories = updatedCategories.map { category ->
                    when (category.name == shownStory.category) {
                        true -> {
                            category.copy(
                                stories = category.stories.map { story ->
                                    when (story.title == shownStory.title) {
                                        true -> story.copy(timesShown = story.timesShown.inc())
                                        false -> story
                                    }
                                },
                            )
                        }
                        false -> category
                    }
                }
            }

            var updatedSponsoredStories = state.pocketSponsoredStories
            action.storiesShown.filterIsInstance<PocketSponsoredStory>().forEach { shownStory ->
                updatedSponsoredStories = updatedSponsoredStories.map { story ->
                    when (story.id == shownStory.id) {
                        true -> story.recordNewImpression()
                        false -> story
                    }
                }
            }

            state.copy(
                pocketStoriesCategories = updatedCategories,
                pocketSponsoredStories = updatedSponsoredStories,
            )
        }
        is AppAction.AddPendingDeletionSet ->
            state.copy(pendingDeletionHistoryItems = state.pendingDeletionHistoryItems + action.historyItems)

        is AppAction.UndoPendingDeletionSet ->
            state.copy(pendingDeletionHistoryItems = state.pendingDeletionHistoryItems - action.historyItems)
        is AppAction.AppLifecycleAction.ResumeAction -> {
            state.copy(isForeground = true)
        }
        is AppAction.AppLifecycleAction.PauseAction -> {
            state.copy(isForeground = false)
        }

        is AppAction.UpdateStandardSnackbarErrorAction -> state.copy(
            standardSnackbarError = action.standardSnackbarError,
        )

        is AppAction.ShoppingAction -> ShoppingStateReducer.reduce(state, action)
        is AppAction.WallpaperAction -> WallpapersReducer.reduce(state, action)
        is AppAction.IntentAction -> IntentReducer.reduce(state, action)
        is AppAction.ToolbarAction -> ToolbarReducer.reduce(state, action)
        is AppAction.TabsTrayAction -> TabsTrayReducer.reduce(state, action)
    }
}

/**
 * Removes a [RecentHistoryGroup] identified by [groupTitle] if it exists in the current list.
 *
 * @param groupTitle [RecentHistoryGroup.title] of the item that should be removed.
 */
@VisibleForTesting
internal fun List<RecentlyVisitedItem>.filterOut(groupTitle: String?): List<RecentlyVisitedItem> {
    return when (groupTitle != null) {
        true -> filterNot { it is RecentHistoryGroup && it.title.equals(groupTitle, true) }
        false -> this
    }
}
