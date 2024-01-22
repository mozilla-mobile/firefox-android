/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode

/**
 * Navigates to the browser fragment and loads a URL or performs a search (depending on the
 * value of [searchTermOrURL]).
 *
 * @param navController The [NavController] to use for navigation.
 * @param searchTermOrURL The entered search term to search or URL to be loaded.
 * @param newTab Whether or not to load the URL in a new tab.
 * @param from The [BrowserDirection] to indicate which fragment the browser is being opened from.
 * @param browsingMode The tab's current [BrowsingMode].
 * @param customTabSessionId Optional custom tab session ID if navigating from a custom tab.
 * @param engine Optional [SearchEngine] to use when performing a search.
 * @param forceSearch Whether or not to force performing a search.
 * @param flags Flags that will be used when loading the URL (not applied to searches).
 * @param requestDesktopMode Whether or not to request the desktop mode for the session.
 * @param historyMetadata The [HistoryMetadataKey] of the new tab in case this tab was opened from history.
 * @param additionalHeaders The extra headers to use when loading the URL.
 */
fun FragmentActivity.openToBrowserAndLoad(
    navController: NavController,
    searchTermOrURL: String,
    newTab: Boolean,
    from: BrowserDirection,
    browsingMode: BrowsingMode = BrowsingMode.Normal,
    customTabSessionId: String? = null,
    engine: SearchEngine? = null,
    forceSearch: Boolean = false,
    flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
    requestDesktopMode: Boolean = false,
    historyMetadata: HistoryMetadataKey? = null,
    additionalHeaders: Map<String, String>? = null,
) {
    openToBrowser(navController = navController, from = from, customTabSessionId = customTabSessionId)
    load(
        searchTermOrURL = searchTermOrURL,
        newTab = newTab,
        engine = engine,
        forceSearch = forceSearch,
        flags = flags,
        browsingMode = browsingMode,
        requestDesktopMode = requestDesktopMode,
        historyMetadata = historyMetadata,
        additionalHeaders = additionalHeaders,
    )
}

/**
 * Navigates to the browser fragment.
 *
 * @param navController The [NavController] to use for navigation.
 * @param from The [BrowserDirection] to indicate which fragment the browser is being opened from.
 * @param customTabSessionId Optional custom tab session ID if navigating from a custom tab.
 */
fun FragmentActivity.openToBrowser(
    navController: NavController,
    from: BrowserDirection,
    customTabSessionId: String? = null,
) {
    if (navController.alreadyOnDestination(R.id.browserFragment)) return
    @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
    val directions = getNavDirections(from, customTabSessionId)
    if (directions != null) {
        navController.nav(fragmentId, directions)
    }
}

/**
 * Loads a URL or performs a search (depending on the value of [searchTermOrURL]).
 *
 * @param searchTermOrURL The entered search term to search or URL to be loaded.
 * @param newTab Whether or not to load the URL in a new tab.
 * @param engine Optional [SearchEngine] to use when performing a search.
 * @param forceSearch Whether or not to force performing a search.
 * @param flags Flags that will be used when loading the URL (not applied to searches).
 * @param browsingMode The tab's current [BrowsingMode].
 * @param requestDesktopMode Whether or not to request the desktop mode for the session.
 * @param historyMetadata The [HistoryMetadataKey] of the new tab in case this tab was opened from history.
 * @param additionalHeaders The extra headers to use when loading the URL.
 */
private fun FragmentActivity.load(
    searchTermOrURL: String,
    newTab: Boolean,
    engine: SearchEngine?,
    forceSearch: Boolean,
    flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
    browsingMode: BrowsingMode,
    requestDesktopMode: Boolean = false,
    historyMetadata: HistoryMetadataKey? = null,
    additionalHeaders: Map<String, String>? = null,
) {
    val startTime = components.core.engine.profiler?.getProfilerTime()

    val private = when (browsingMode) {
        BrowsingMode.Private -> true
        BrowsingMode.Normal -> false
    }

    // In situations where we want to perform a search but have no search engine (e.g. the user
    // has removed all of them, or we couldn't load any) we will pass searchTermOrURL to Gecko
    // and let it try to load whatever was entered.
    if ((!forceSearch && searchTermOrURL.isUrl()) || engine == null) {
        val tabId = if (newTab) {
            components.useCases.tabsUseCases.addTab(
                url = searchTermOrURL.toNormalizedUrl(),
                flags = flags,
                private = private,
                historyMetadata = historyMetadata,
            )
        } else {
            components.useCases.sessionUseCases.loadUrl(
                url = searchTermOrURL.toNormalizedUrl(),
                flags = flags,
            )
            components.core.store.state.selectedTabId
        }

        if (requestDesktopMode && tabId != null) {
            handleRequestDesktopMode(tabId)
        }
    } else {
        if (newTab) {
            val searchUseCase = if (browsingMode.isPrivate) {
                components.useCases.searchUseCases.newPrivateTabSearch
            } else {
                components.useCases.searchUseCases.newTabSearch
            }
            searchUseCase.invoke(
                searchTerms = searchTermOrURL,
                source = SessionState.Source.Internal.UserEntered,
                selected = true,
                searchEngine = engine,
                flags = flags,
                additionalHeaders = additionalHeaders,
            )
        } else {
            components.useCases.searchUseCases.defaultSearch.invoke(
                searchTerms = searchTermOrURL,
                searchEngine = engine,
                flags = flags,
                additionalHeaders = additionalHeaders,
            )
        }
    }

    if (components.core.engine.profiler?.isProfilerActive() == true) {
        // Wrapping the `addMarker` method with `isProfilerActive` even though it's no-op when
        // profiler is not active. That way, `text` argument will not create a string builder all the time.
        components.core.engine.profiler?.addMarker(
            "HomeActivity.load",
            startTime,
            "newTab: $newTab",
        )
    }
}

internal fun FragmentActivity.handleRequestDesktopMode(tabId: String) {
    components.useCases.sessionUseCases.requestDesktopSite(true, tabId)
    components.core.store.dispatch(ContentAction.UpdateDesktopModeAction(tabId, true))

    // Reset preference value after opening the tab in desktop mode
    settings().openNextTabInDesktopMode = false
}
