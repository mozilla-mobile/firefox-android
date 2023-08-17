/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.metrics.MetricsUtils
import org.mozilla.fenix.utils.Settings

/**
 * The [Store] for holding the [SearchFragmentState] and applying [SearchFragmentAction]s.
 */
class SearchFragmentStore(
    initialState: SearchFragmentState,
) : Store<SearchFragmentState, SearchFragmentAction>(
    initialState,
    ::searchStateReducer,
)

/**
 * Wraps a `SearchEngine` to give consumers the context that it was selected as a shortcut
 */
sealed class SearchEngineSource {
    abstract val searchEngine: SearchEngine?

    /**
     * No search engine
     */
    object None : SearchEngineSource() {
        override val searchEngine: SearchEngine? = null
    }

    /**
     * Search engine set as default
     */
    data class Default(override val searchEngine: SearchEngine) : SearchEngineSource()

    /**
     * Search engine for quick search
     * This is for any search engine that is not the user selected default.
     */
    data class Shortcut(override val searchEngine: SearchEngine) : SearchEngineSource()

    /**
     * Search engine for history
     */
    data class History(override val searchEngine: SearchEngine) : SearchEngineSource()

    /**
     * Search engine for bookmarks
     */
    data class Bookmarks(override val searchEngine: SearchEngine) : SearchEngineSource()

    /**
     * Search engine for tabs
     */
    data class Tabs(override val searchEngine: SearchEngine) : SearchEngineSource()
}

/**
 * The state for the Search Screen
 *
 * @property query The current search query string
 * @property url The current URL of the tab (if this fragment is shown for an already existing tab)
 * @property searchTerms The search terms used to search previously in this tab (if this fragment is shown
 * for an already existing tab)
 * @property searchEngineSource The current selected search engine with the context of how it was selected
 * @property defaultEngine The current default search engine (or null if none is available yet)
 * @property showSearchSuggestions Whether or not to show search suggestions from the search engine in the AwesomeBar
 * @property showSearchSuggestionsHint Whether or not to show search suggestions in private hint panel
 * @property showClipboardSuggestions Whether or not to show clipboard suggestion in the AwesomeBar
 * @property showSearchTermHistory Whether or not to show suggestions based on the previously used search terms
 * with the currently selected search engine.
 * @property showHistorySuggestionsForCurrentEngine Whether or not to show history suggestions for only
 * the current search engine.
 * @property showAllHistorySuggestions Whether or not to show history suggestions in the AwesomeBar
 * @property showBookmarksSuggestionsForCurrentEngine Whether or not to show bookmarks suggestions for only
 * the current search engine.
 * @property showAllBookmarkSuggestions Whether or not to show the bookmark suggestion in the AwesomeBar
 * @property showSyncedTabsSuggestionsForCurrentEngine Whether or not to show synced tabs suggestions for only
 * the current search engine.
 * @property showAllSyncedTabsSuggestions Whether or not to show the synced tabs suggestion in the AwesomeBar
 * @property showSessionSuggestionsForCurrentEngine Whether or not to show local tabs suggestions for only
 * the current search engine.
 * @property showAllSessionSuggestions Whether or not to show the session suggestion in the AwesomeBar
 * @property pastedText The text pasted from the long press toolbar menu
 * @property clipboardHasUrl Indicates if the clipboard contains an URL.
 */
data class SearchFragmentState(
    val query: String,
    val url: String,
    val searchTerms: String,
    val searchEngineSource: SearchEngineSource,
    val defaultEngine: SearchEngine?,
    val showSearchSuggestions: Boolean,
    val showSearchSuggestionsHint: Boolean,
    val showClipboardSuggestions: Boolean,
    val showSearchTermHistory: Boolean,
    val showHistorySuggestionsForCurrentEngine: Boolean,
    val showAllHistorySuggestions: Boolean,
    val showBookmarksSuggestionsForCurrentEngine: Boolean,
    val showAllBookmarkSuggestions: Boolean,
    val showSyncedTabsSuggestionsForCurrentEngine: Boolean,
    val showAllSyncedTabsSuggestions: Boolean,
    val showSessionSuggestionsForCurrentEngine: Boolean,
    val showAllSessionSuggestions: Boolean,
    val tabId: String?,
    val pastedText: String? = null,
    val searchAccessPoint: MetricsUtils.Source,
    val clipboardHasUrl: Boolean = false,
) : State

/**
 * Creates the initial state for the search fragment.
 */
@Suppress("LongParameterList")
fun createInitialSearchFragmentState(
    activity: HomeActivity,
    components: Components,
    tabId: String?,
    pastedText: String?,
    searchAccessPoint: MetricsUtils.Source,
    searchEngine: SearchEngine? = null,
): SearchFragmentState {
    val settings = components.settings
    val tab = tabId?.let { components.core.store.state.findTab(it) }
    val url = tab?.content?.url.orEmpty()

    val searchEngineSource = if (searchEngine != null) {
        SearchEngineSource.Shortcut(searchEngine)
    } else {
        SearchEngineSource.None
    }

    return SearchFragmentState(
        query = url,
        url = url,
        searchTerms = tab?.content?.searchTerms.orEmpty(),
        searchEngineSource = searchEngineSource,
        defaultEngine = null,
        showSearchSuggestions = shouldShowSearchSuggestions(
            browsingMode = activity.browsingModeManager.mode,
            settings = settings,
        ),
        showSearchSuggestionsHint = false,
        showClipboardSuggestions = settings.shouldShowClipboardSuggestions,
        showSearchTermHistory = settings.shouldShowHistorySuggestions,
        showHistorySuggestionsForCurrentEngine = false,
        showAllHistorySuggestions = settings.shouldShowHistorySuggestions,
        showBookmarksSuggestionsForCurrentEngine = false,
        showAllBookmarkSuggestions = settings.shouldShowBookmarkSuggestions,
        showSyncedTabsSuggestionsForCurrentEngine = false,
        showAllSyncedTabsSuggestions = settings.shouldShowSyncedTabsSuggestions,
        showSessionSuggestionsForCurrentEngine = false,
        showAllSessionSuggestions = true,
        tabId = tabId,
        pastedText = pastedText,
        searchAccessPoint = searchAccessPoint,
    )
}

/**
 * Actions to dispatch through the `SearchStore` to modify `SearchState` through the reducer.
 */
sealed class SearchFragmentAction : Action {
    /**
     * Action to enable or disable search suggestions.
     */
    data class SetShowSearchSuggestions(val show: Boolean) : SearchFragmentAction()

    /**
     * Action when default search engine is selected.
     */
    data class SearchDefaultEngineSelected(
        val engine: SearchEngine,
        val browsingMode: BrowsingMode,
        val settings: Settings,
    ) : SearchFragmentAction()

    /**
     * Action when shortcut search engine is selected.
     */
    data class SearchShortcutEngineSelected(
        val engine: SearchEngine,
        val browsingMode: BrowsingMode,
        val settings: Settings,
    ) : SearchFragmentAction()

    /**
     * Action when history search engine is selected.
     */
    data class SearchHistoryEngineSelected(val engine: SearchEngine) : SearchFragmentAction()

    /**
     * Action when bookmarks search engine is selected.
     */
    data class SearchBookmarksEngineSelected(val engine: SearchEngine) : SearchFragmentAction()

    /**
     * Action when tabs search engine is selected.
     */
    data class SearchTabsEngineSelected(val engine: SearchEngine) : SearchFragmentAction()

    /**
     * Action when allow search suggestion in private mode hint is tapped.
     */
    data class AllowSearchSuggestionsInPrivateModePrompt(val show: Boolean) : SearchFragmentAction()

    /**
     * Action when query is updated.
     */
    data class UpdateQuery(val query: String) : SearchFragmentAction()

    /**
     * Action when updating clipboard URL.
     */
    data class UpdateClipboardHasUrl(val hasUrl: Boolean) : SearchFragmentAction()

    /**
     * Updates the local `SearchFragmentState` from the global `SearchState` in `BrowserStore`.
     */
    data class UpdateSearchState(val search: SearchState) : SearchFragmentAction()
}

/**
 * The SearchState Reducer.
 */
@Suppress("LongMethod")
private fun searchStateReducer(state: SearchFragmentState, action: SearchFragmentAction): SearchFragmentState {
    return when (action) {
        is SearchFragmentAction.SearchDefaultEngineSelected ->
            state.copy(
                searchEngineSource = SearchEngineSource.Default(action.engine),
                showSearchSuggestions = shouldShowSearchSuggestions(action.browsingMode, action.settings),
                showClipboardSuggestions = action.settings.shouldShowClipboardSuggestions,
                showSearchTermHistory = action.settings.shouldShowHistorySuggestions,
                showHistorySuggestionsForCurrentEngine = false, // we'll show all history
                showAllHistorySuggestions = action.settings.shouldShowHistorySuggestions,
                showBookmarksSuggestionsForCurrentEngine = false, // we'll show all bookmarks
                showAllBookmarkSuggestions = action.settings.shouldShowBookmarkSuggestions,
                showSyncedTabsSuggestionsForCurrentEngine = false, // we'll show all synced tabs
                showAllSyncedTabsSuggestions = action.settings.shouldShowSyncedTabsSuggestions,
                showSessionSuggestionsForCurrentEngine = false, // we'll show all local tabs
                showAllSessionSuggestions = true,
            )
        is SearchFragmentAction.SearchShortcutEngineSelected ->
            state.copy(
                searchEngineSource = SearchEngineSource.Shortcut(action.engine),
                showSearchSuggestions = shouldShowSearchSuggestions(action.browsingMode, action.settings),
                showClipboardSuggestions = action.settings.shouldShowClipboardSuggestions,
                showSearchTermHistory = action.settings.shouldShowHistorySuggestions,
                showHistorySuggestionsForCurrentEngine = action.settings.shouldShowHistorySuggestions &&
                    !action.engine.isGeneral,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = action.settings.shouldShowBookmarkSuggestions &&
                    !action.engine.isGeneral,
                showAllBookmarkSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = action.settings.shouldShowSyncedTabsSuggestions &&
                    !action.engine.isGeneral,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = !action.engine.isGeneral,
                showAllSessionSuggestions = false,
            )
        is SearchFragmentAction.SearchHistoryEngineSelected ->
            state.copy(
                searchEngineSource = SearchEngineSource.History(action.engine),
                showSearchSuggestions = false,
                showClipboardSuggestions = false,
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = true,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
            )
        is SearchFragmentAction.SearchBookmarksEngineSelected ->
            state.copy(
                searchEngineSource = SearchEngineSource.Bookmarks(action.engine),
                showSearchSuggestions = false,
                showClipboardSuggestions = false,
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = true,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = false,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = false,
            )
        is SearchFragmentAction.SearchTabsEngineSelected ->
            state.copy(
                searchEngineSource = SearchEngineSource.Tabs(action.engine),
                showSearchSuggestions = false,
                showClipboardSuggestions = false,
                showSearchTermHistory = false,
                showHistorySuggestionsForCurrentEngine = false,
                showAllHistorySuggestions = false,
                showBookmarksSuggestionsForCurrentEngine = false,
                showAllBookmarkSuggestions = false,
                showSyncedTabsSuggestionsForCurrentEngine = false,
                showAllSyncedTabsSuggestions = true,
                showSessionSuggestionsForCurrentEngine = false,
                showAllSessionSuggestions = true,
            )
        is SearchFragmentAction.UpdateQuery ->
            state.copy(query = action.query)
        is SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt ->
            state.copy(showSearchSuggestionsHint = action.show)
        is SearchFragmentAction.SetShowSearchSuggestions ->
            state.copy(showSearchSuggestions = action.show)
        is SearchFragmentAction.UpdateSearchState -> {
            state.copy(
                defaultEngine = action.search.selectedOrDefaultSearchEngine,
                searchEngineSource = when (state.searchEngineSource) {
                    is SearchEngineSource.Default, is SearchEngineSource.None -> {
                        action.search.selectedOrDefaultSearchEngine?.let { SearchEngineSource.Default(it) }
                            ?: SearchEngineSource.None
                    }
                    else -> {
                        state.searchEngineSource
                    }
                },
            )
        }
        is SearchFragmentAction.UpdateClipboardHasUrl -> {
            state.copy(
                clipboardHasUrl = action.hasUrl,
            )
        }
    }
}

/**
 * Check whether search suggestions should be shown in the AwesomeBar.
 *
 * @param browsingMode Current browsing mode: normal or private.
 * @param settings Persistence layer containing user option's for showing search suggestions.
 *
 * @return `true` if search suggestions should be shown `false` otherwise.
 */
@VisibleForTesting
internal fun shouldShowSearchSuggestions(
    browsingMode: BrowsingMode,
    settings: Settings,
) = when (browsingMode) {
    BrowsingMode.Normal -> settings.shouldShowSearchSuggestions
    BrowsingMode.Private ->
        settings.shouldShowSearchSuggestions && settings.shouldShowSearchSuggestionsInPrivate
}
