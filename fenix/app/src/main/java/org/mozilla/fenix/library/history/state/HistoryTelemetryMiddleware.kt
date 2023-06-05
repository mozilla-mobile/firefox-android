package org.mozilla.fenix.library.history.state

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentState
import org.mozilla.fenix.library.history.RemoveTimeFrame
import org.mozilla.fenix.GleanMetrics.History as GleanHistory

class HistoryTelemetryMiddleware(
    private val isInPrivateMode: () -> Boolean, // this should potentially be a browser store read
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction
    ) {
        when (action) {
            is HistoryFragmentAction.HistoryItemClicked -> {
                if (context.state.mode.selectedItems.isEmpty()) {
                    when (val item = action.item) {
                        is History.Regular -> {
                            GleanHistory.openedItem.record(
                                GleanHistory.OpenedItemExtra(
                                    isRemote = item.isRemote,
                                    timeGroup = item.historyTimeGroup.toString(),
                                    isPrivate = isInPrivateMode(),
                                ),
                            )
                        }

                        is History.Group -> GleanHistory.searchTermGroupTapped.record(NoExtras())
                        else -> {}
                    }
                }
            }
            is HistoryFragmentAction.SearchClicked -> GleanHistory.searchIconTapped.record(NoExtras())
            is HistoryFragmentAction.EnterRecentlyClosed -> Events.recentlyClosedTabsOpened.record(NoExtras())
            is HistoryFragmentAction.DeleteItems -> {
                for (item in action.items) {
                    GleanHistory.removed.record(NoExtras())
                }
            }
            is HistoryFragmentAction.DeleteTimeRange -> when (action.timeFrame) {
                    RemoveTimeFrame.LastHour -> GleanHistory.removedLastHour.record(NoExtras())
                    RemoveTimeFrame.TodayAndYesterday -> GleanHistory.removedTodayAndYesterday.record(NoExtras())
                    null -> GleanHistory.removedAll.record(NoExtras())
                }
            else -> {}
        }
        next(action)
    }
}
