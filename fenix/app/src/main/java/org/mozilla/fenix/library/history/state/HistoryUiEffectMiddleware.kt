package org.mozilla.fenix.library.history.state

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentState

class HistoryUiEffectMiddleware(
    private val refreshView: () -> Unit,
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction,
    ) {
        when (action) {
            is HistoryFragmentAction.FinishSync -> refreshView()
            is HistoryFragmentAction.AddItemForRemoval, 
            HistoryFragmentAction.BackPressed, 
            is HistoryFragmentAction.ChangeEmptyState, 
            is HistoryFragmentAction.DeleteItems, 
            is HistoryFragmentAction.DeleteTimeRange, 
            HistoryFragmentAction.EnterDeletionMode, 
            HistoryFragmentAction.EnterRecentlyClosed, 
            HistoryFragmentAction.ExitDeletionMode, 
            HistoryFragmentAction.ExitEditMode, 
            is HistoryFragmentAction.HistoryItemClicked, 
            is HistoryFragmentAction.HistoryItemLongClicked, 
            is HistoryFragmentAction.RemoveItemForRemoval, 
            HistoryFragmentAction.SearchClicked, 
            HistoryFragmentAction.StartSync, 
            is HistoryFragmentAction.UpdatePendingDeletionItems -> Unit
        }
    }
}
