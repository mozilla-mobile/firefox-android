package org.mozilla.fenix.browser.readermode

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.ext.alreadyOnDestination
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.utils.Settings

class BrowserNavigator(
    private val addTabUseCase: TabsUseCases.AddNewTabUseCase,
    private val sessionUseCases: SessionUseCases,
    private val searchUseCases: SearchUseCases,
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val profiler: Profiler?,
    private val settings: Settings,
    private val getNavDirections: (BrowserDirection, String?) -> NavDirections?,
    private val getNavController: () -> NavController,
) {

    constructor(
        components: Components,
        settings: Settings,
        getNavDirections: (BrowserDirection, String?) -> NavDirections?,
        getNavController: () -> NavController,
    ) : this(
        components.useCases.tabsUseCases.addTab,
        components.useCases.sessionUseCases,
        components.useCases.searchUseCases,
        components.appStore,
        components.core.store,
        components.core.engine.profiler,
        settings,
        getNavDirections,
        getNavController,
    )

    fun openToBrowserAndLoad(
        searchTermOrURL: String,
        newTab: Boolean,
        from: BrowserDirection,
        customTabSessionId: String? = null,
        engine: SearchEngine? = null,
        forceSearch: Boolean = false,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
        requestDesktopMode: Boolean = false,
        historyMetadata: HistoryMetadataKey? = null,
        additionalHeaders: Map<String, String>? = null,
    ) {
        openToBrowser(from, customTabSessionId)
        load(
            searchTermOrURL = searchTermOrURL,
            newTab = newTab,
            engine = engine,
            forceSearch = forceSearch,
            flags = flags,
            requestDesktopMode = requestDesktopMode,
            historyMetadata = historyMetadata,
            additionalHeaders = additionalHeaders,
        )
    }

    fun openToBrowser(from: BrowserDirection, customTabSessionId: String? = null) {
        if (getNavController().alreadyOnDestination(R.id.browserFragment)) return
        @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
        val directions = getNavDirections(from, customTabSessionId)
        if (directions != null) {
            getNavController().nav(fragmentId, directions)
        }
    }

    private fun load(
        searchTermOrURL: String,
        newTab: Boolean,
        engine: SearchEngine?,
        forceSearch: Boolean,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
        requestDesktopMode: Boolean = false,
        historyMetadata: HistoryMetadataKey? = null,
        additionalHeaders: Map<String, String>? = null,
        mode: BrowsingMode? = null,
    ) {
        val startTime = profiler?.getProfilerTime()

        val private = when (mode ?: appStore.state.mode) {
            BrowsingMode.Private -> true
            BrowsingMode.Normal -> false
        }

        // In situations where we want to perform a search but have no search engine (e.g. the user
        // has removed all of them, or we couldn't load any) we will pass searchTermOrURL to Gecko
        // and let it try to load whatever was entered.
        if ((!forceSearch && searchTermOrURL.isUrl()) || engine == null) {
            val tabId = if (newTab) {
                addTabUseCase(
                    url = searchTermOrURL.toNormalizedUrl(),
                    flags = flags,
                    private = private,
                    historyMetadata = historyMetadata,
                )
            } else {
                sessionUseCases.loadUrl(
                    url = searchTermOrURL.toNormalizedUrl(),
                    flags = flags,
                )
                browserStore.state.selectedTabId
            }

            if (requestDesktopMode && tabId != null) {
                handleRequestDesktopMode(tabId)
            }
        } else {
            if (newTab) {
                val searchUseCase = if (private) {
                    searchUseCases.newPrivateTabSearch
                } else {
                    searchUseCases.newTabSearch
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
                searchUseCases.defaultSearch.invoke(
                    searchTerms = searchTermOrURL,
                    searchEngine = engine,
                    flags = flags,
                    additionalHeaders = additionalHeaders,
                )
            }
        }

        if (profiler?.isProfilerActive() == true) {
            // Wrapping the `addMarker` method with `isProfilerActive` even though it's no-op when
            // profiler is not active. That way, `text` argument will not create a string builder all the time.
            profiler.addMarker(
                "HomeActivity.load",
                startTime,
                "newTab: $newTab",
            )
        }
    }

    private fun handleRequestDesktopMode(tabId: String) {
        sessionUseCases.requestDesktopSite(true, tabId)
        browserStore.dispatch(ContentAction.UpdateDesktopModeAction(tabId, true))

        // Reset preference value after opening the tab in desktop mode
        settings.openNextTabInDesktopMode = false
    }
}
