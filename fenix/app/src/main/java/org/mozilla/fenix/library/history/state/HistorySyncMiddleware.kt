package org.mozilla.fenix.library.history.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentState

class HistorySyncMiddleware(
    private val syncHistory: suspend () -> Unit,
    private val scope: CoroutineScope
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction
    ) {
        when (action) {
            is HistoryFragmentAction.StartSync -> {
                scope.launch {
                    syncHistory()
                    context.store.dispatch(HistoryFragmentAction.FinishSync)
                }
            }
            else -> Unit
        }
        next(action)
    }
}

