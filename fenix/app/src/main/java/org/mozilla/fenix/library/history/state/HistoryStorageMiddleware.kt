package org.mozilla.fenix.library.history.state

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.HistoryMetadataAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.history.DefaultPagedHistoryProvider
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentState
import org.mozilla.fenix.library.history.HistoryFragmentStore
import org.mozilla.fenix.library.history.toPendingDeletionHistory

class HistoryStorageMiddleware(
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val historyProvider: DefaultPagedHistoryProvider,
    private val historyStorage: PlacesHistoryStorage,
    private val deleteSnackbar: (
        items: Set<History>,
        undo: suspend (Set<History>) -> Unit,
        delete: (HistoryFragmentStore, Set<History>) -> suspend (context: Context) -> Unit,
    ) -> Unit,
    private val onTimeFrameDeleted: () -> Unit
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction
    ) {
        when (action) {
            is HistoryFragmentAction.DeleteItems -> {
                appStore.dispatch(AppAction.AddPendingDeletionSet(action.items.toPendingDeletionHistory()))
                deleteSnackbar(action.items, ::undo, ::delete)
            }
            is HistoryFragmentAction.DeleteTimeRange -> {
                context.store.dispatch(HistoryFragmentAction.EnterDeletionMode)
                CoroutineScope(Dispatchers.IO).launch {
                    if (action.timeFrame == null) {
                        historyStorage.deleteEverything()
                    } else {
                        val longRange = action.timeFrame.toLongRange()
                        historyStorage.deleteVisitsBetween(
                            startTime = longRange.first,
                            endTime = longRange.last,
                        )
                    }
                    browserStore.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction)
                    browserStore.dispatch(EngineAction.PurgeHistoryAction).join()

                    context.store.dispatch(HistoryFragmentAction.ExitDeletionMode)
                    launch(Dispatchers.Main) {
                        onTimeFrameDeleted()
                    }
                }
            }
            else -> Unit
        }
        next(action)
    }

    private fun undo(items: Set<History>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.UndoPendingDeletionSet(pendingDeletionItems))
    }

    private fun delete(store: HistoryFragmentStore, items: Set<History>): suspend (context: Context) -> Unit {
        return { context ->
            CoroutineScope(Dispatchers.IO).launch {
                store.dispatch(HistoryFragmentAction.EnterDeletionMode)
                for (item in items) {
                    when (item) {
                        is History.Regular -> context.components.core.historyStorage.deleteVisitsFor(item.url)
                        is History.Group -> {
                            // NB: If we have non-search groups, this logic needs to be updated.
                            historyProvider.deleteMetadataSearchGroup(item)
                            context.components.core.store.dispatch(
                                HistoryMetadataAction.DisbandSearchGroupAction(searchTerm = item.title),
                            )
                        }
                        // We won't encounter individual metadata entries outside of groups.
                        is History.Metadata -> {}
                    }
                }
                store.dispatch(HistoryFragmentAction.ExitDeletionMode)
            }
        }
    }
}
