/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.engine.middleware

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.SessionPriority.DEFAULT
import mozilla.components.concept.engine.EngineSession.SessionPriority.HIGH
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.coroutines.Dispatchers as MozillaDispatchers

/**
 * [Middleware] implementation responsible for updating the priority of the selected [EngineSession]
 * to [HIGH] and the rest to [DEFAULT].
 */
class SessionPrioritizationMiddleware(
    private val clearAfterMillis: Long = 15000,
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val waitScope: CoroutineScope = CoroutineScope(MozillaDispatchers.Cached),
) : Middleware<BrowserState, BrowserAction> {
    private val logger = Logger("SessionPrioritizationMiddleware")
    private var clearHighPriorityJobs = mutableMapOf<String, Job>()

    @VisibleForTesting
    internal var previousHighestPriorityTabId = ""

    override fun invoke(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction,
    ) {
        when (action) {
            is EngineAction.UnlinkEngineSessionAction -> {
                val activeTab = context.state.findTab(action.tabId)
                activeTab?.engineState?.engineSession?.updateSessionPriority(DEFAULT)
                logger.info("Update the tab ${activeTab?.id} priority to ${DEFAULT.name}")
            }
            is ContentAction.CheckForFormDataAction -> {
                val tab = context.state.findTab(action.tabId)
                if (action.containsFormData) {
                    tab?.engineState?.engineSession?.updateSessionPriority(HIGH)
                    logger.info("Update the tab ${tab?.id} priority to ${HIGH.name}")
                    tab?.let {
                        clearHighPriority(context, it.id)
                    }
                } else {
                    tab?.engineState?.engineSession?.updateSessionPriority(DEFAULT)
                    logger.info("Update the tab ${tab?.id} priority to ${DEFAULT.name}")
                }
                return // Do not let the action continue through to the reducer
            }
            is ContentAction.ClearHighPrioritySessionAction -> {
                // remove finished job from map
                val tab = context.state.findTab(action.tabId)
                tab?.engineState?.engineSession?.updateSessionPriority(DEFAULT)
                logger.info("Update the tab ${tab?.id} priority back to ${DEFAULT.name}")
                clearHighPriorityJobs.remove(action.tabId)
                return // Do not let the action continue through to the reducer
            }
            else -> {
                // no-op
            }
        }

        next(action)

        when (action) {
            is TabListAction,
            is EngineAction.LinkEngineSessionAction,
            -> {
                // if it exists in the map of high priority tabs to be cleared, cancel the job and remove it
                val state = context.state
                clearHighPriorityJobs[state.selectedTabId]?.cancel()
                clearHighPriorityJobs.remove(state.selectedTabId)

                if (previousHighestPriorityTabId != state.selectedTabId) {
                    updatePriorityIfNeeded(state)
                }
            }
            else -> {
                // no-op
            }
        }
    }

    private fun updatePriorityIfNeeded(state: BrowserState) = mainScope.launch {
        val currentSelectedTab = state.selectedTabId?.let { state.findTab(it) }
        val previousSelectedTab = state.findTab(previousHighestPriorityTabId)
        val currentEngineSession: EngineSession? = currentSelectedTab?.engineState?.engineSession

        // We need to make sure we alter the previousHighestPriorityTabId, after the session is linked.
        // So we update the priority on the engine session, as we could get actions where the tab
        // is selected but not linked yet, causing out sync issues,
        // when previousHighestPriorityTabId didn't call updateSessionPriority()
        if (currentEngineSession != null) {
            mainScope.launch {
                // check for existing form data here and only set DEFAULT if there is
                previousSelectedTab?.engineState?.engineSession?.checkForFormData()
            }

            currentEngineSession.updateSessionPriority(HIGH)
            logger.info("Update the currentSelectedTab ${currentSelectedTab.id} priority to ${HIGH.name}")
            previousHighestPriorityTabId = currentSelectedTab.id
        }
    }

    private fun clearHighPriority(context: MiddlewareContext<BrowserState, BrowserAction>, tabId: String) {
        // store and launch the new job related to the tabId
        var clearJob: Job = waitScope.launch {
            delay(clearAfterMillis)
            context.store.dispatch(ContentAction.ClearHighPrioritySessionAction(tabId))
        }
        clearHighPriorityJobs[tabId] = clearJob
        logger.info("Tab $tabId will return to ${DEFAULT.name} priority after $clearAfterMillis ms")
    }
}
