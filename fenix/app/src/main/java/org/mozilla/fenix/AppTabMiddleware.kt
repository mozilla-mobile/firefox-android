package org.mozilla.fenix

import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.appstate.Screen
import org.mozilla.fenix.utils.Settings

class AppTabMiddleware(
    private val browserStore: BrowserStore,
    private val settings: Settings,
    private val tabsUseCases: TabsUseCases,
    private val sessionUseCases: SessionUseCases,
    private val searchUseCases: SearchUseCases,
    private val profiler: Profiler? = null,
) : Middleware<AppState, AppAction> {
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction
    ) {
        next(action)
        when (action) {
            is AppAction.ChangeScreen -> {
                if (action.screen is Screen.Browser) loadBrowserTab(action.screen)
            }
            else -> Unit
        }
    }

    private fun loadBrowserTab(screen: Screen.Browser) {
        val startTime = profiler?.getProfilerTime()
        val mode = BrowsingMode.Normal
//        val mode = browsingModeManager.mode


//        val private = when (mode) {
//            BrowsingMode.Private -> true
//            BrowsingMode.Normal -> false
//        }

        // In situations where we want to perform a search but have no search engine (e.g. the user
        // has removed all of them, or we couldn't load any) we will pass searchTermOrURL to Gecko
        // and let it try to load whatever was entered.
        if ((!screen.forceSearch && screen.searchTermOrURL.isUrl()) || screen.engine == null) {
            val tabId = if (screen.newTab) {
                tabsUseCases.addTab(
                    url = screen.searchTermOrURL.toNormalizedUrl(),
                    flags = screen.flags,
                    private = mode.isPrivate,
                    historyMetadata = screen.historyMetadata,
                )
            } else {
               sessionUseCases.loadUrl(
                    url = screen.searchTermOrURL.toNormalizedUrl(),
                    flags = screen.flags,
                )
                browserStore.state.selectedTabId
            }

            if (screen.requestDesktopMode && tabId != null) {
                handleRequestDesktopMode(tabId)
            }
        } else {
            if (screen.newTab) {
                val searchUseCase = if (mode.isPrivate) {
                    searchUseCases.newPrivateTabSearch
                } else {
                    searchUseCases.newTabSearch
                }
                searchUseCase.invoke(
                    searchTerms = screen.searchTermOrURL,
                    source = SessionState.Source.Internal.UserEntered,
                    selected = true,
                    searchEngine = screen.engine,
                    flags = screen.flags,
                    additionalHeaders = screen.additionalHeaders,
                )
            } else {
                searchUseCases.defaultSearch.invoke(
                    searchTerms = screen.searchTermOrURL,
                    searchEngine = screen.engine,
                    flags = screen.flags,
                    additionalHeaders = screen.additionalHeaders,
                )
            }
        }

        if (profiler?.isProfilerActive() == true) {
            // Wrapping the `addMarker` method with `isProfilerActive` even though it's no-op when
            // profiler is not active. That way, `text` argument will not create a string builder all the time.
            profiler.addMarker(
                "HomeActivity.load",
                startTime,
                "newTab: $screen.newTab",
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
