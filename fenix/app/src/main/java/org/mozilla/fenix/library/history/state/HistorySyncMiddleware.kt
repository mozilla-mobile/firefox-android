package org.mozilla.fenix.library.history.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.service.fxa.AccountManagerException
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.SyncReason
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentState


class HistorySyncMiddleware(
    private val accountManager: FxaAccountManager,
    private val refreshView: () -> Unit,
    private val scope: CoroutineScope,
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction,
    ) {
        when (action) {
            is HistoryFragmentAction.StartSync -> {
                scope.launch {
                    accountManager.syncNow(
                        reason = SyncReason.User,
                        debounce = true,
                        customEngineSubset = listOf(SyncEngine.History),
                    )
                    refreshView()
                    context.store.dispatch(HistoryFragmentAction.FinishSync)
                }
            }
            else -> Unit
        }
        next(action)
    }
}
