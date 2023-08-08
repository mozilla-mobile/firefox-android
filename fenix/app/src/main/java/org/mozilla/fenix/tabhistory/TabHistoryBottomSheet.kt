/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabhistory

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.gms.common.util.VisibleForTesting
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.state.content.HistoryState
import mozilla.components.lib.state.ext.flowScoped
import org.mozilla.fenix.ext.components

/**
 * Is the bottom sheet that will appear when the user presses the long-click back button.
 */
class TabHistoryBottomSheet(
    private val customTabSessionId: String?,
    private val tabHistoryBottomSheetComposeView: ComposeView,
) {
    private var currentSelectedIndex: Int? = null

    /**
     * Create and display [TabHistoryBottomSheet] using compose.
     */
    fun show() {
        if (tabHistoryBottomSheetComposeView.visibility == View.GONE) {
            tabHistoryBottomSheetComposeView.visibility = View.VISIBLE
            initTabHistoryBottomSheet()
        }
    }

    @VisibleForTesting
    internal fun initTabHistoryBottomSheet() {
        val controller = DefaultTabHistoryController(
            goToHistoryIndexUseCase =
            tabHistoryBottomSheetComposeView.context.components.useCases.sessionUseCases.goToHistoryIndex,
            customTabId = customTabSessionId,
        )
        tabHistoryBottomSheetComposeView.context.components.core.store.flowScoped(
            tabHistoryBottomSheetComposeView.findViewTreeLifecycleOwner(),
        ) { flow ->
            flow.mapNotNull { state -> state.findCustomTabOrSelectedTab(customTabSessionId)?.content?.history }
                .distinctUntilChanged()
                .collect { historyState ->
                    updateState(controller, historyState, tabHistoryBottomSheetComposeView)
                }
        }
    }

    private fun updateState(
        controller: DefaultTabHistoryController,
        historyState: HistoryState,
        tabHistoryBottomSheetComposeView: ComposeView,
    ) {
        currentSelectedIndex = historyState.currentIndex
        var selectedHistoryItem: TabHistoryItem? = null
        val items = historyState.items.mapIndexed { index, historyItem ->
            if (index == currentSelectedIndex) {
                selectedHistoryItem = TabHistoryItem(
                    title = historyItem.title,
                    url = historyItem.uri,
                    index = index,
                )
            }
            TabHistoryItem(
                title = historyItem.title,
                url = historyItem.uri,
                index = index,
            )
        }

        tabHistoryBottomSheetComposeView.apply {
            setContent {
                TabHistoryBottomSheetView(
                    tabHistoryListItems = items.sortedByDescending { it.index },
                    selectedTabHistoryItem = selectedHistoryItem,
                    onClick = {
                        TabHistoryInteractor(controller).goToHistoryItem(it)
                    },
                    onDismiss = {
                        tabHistoryBottomSheetComposeView.visibility = View.GONE
                    },
                )
            }
        }
    }
}

/**
 * The item that will be on the bottom sheet.
 */
data class TabHistoryItem(
    val title: String,
    val url: String,
    val index: Int,
)
