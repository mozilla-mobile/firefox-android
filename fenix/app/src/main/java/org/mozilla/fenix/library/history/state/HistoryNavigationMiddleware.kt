package org.mozilla.fenix.library.history.state

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext

import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentState

class HistoryNavigationMiddleware(private val onBackPressed: () -> Unit) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction
    ) {
        val state = context.store.state
        when (action) {
            is HistoryFragmentAction.BackPressed -> {
                if (state.mode is HistoryFragmentState.Mode.Editing) {
                    next(HistoryFragmentAction.ExitEditMode)
                } else {
                    onBackPressed()
                }
            }
            else -> next(action)
        }
    }
}
