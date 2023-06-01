/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.HistoryMetadataAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.history.DefaultPagedHistoryProvider
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.navigateSafe
import org.mozilla.fenix.library.history.HistoryFragment.DeleteConfirmationDialogFragment
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.History as GleanHistory

@Suppress("TooManyFunctions")
interface HistoryController {
    /**
     * Displays a [DeleteConfirmationDialogFragment].
     */
    fun handleDeleteTimeRange()
    fun handleDeleteSome(items: Set<History>)

    /**
     * Deletes history items inside the time frame.
     *
     * @param timeFrame Selected time frame by the user. If `null`, removes all history.
     */
    fun handleDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?)
}

@Suppress("TooManyFunctions", "LongParameterList")
class DefaultHistoryController(
    private val store: HistoryFragmentStore,
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val historyStorage: PlacesHistoryStorage,
    private var historyProvider: DefaultPagedHistoryProvider,
    private val scope: CoroutineScope,
    private val displayDeleteTimeRange: () -> Unit,
    private val onTimeFrameDeleted: () -> Unit,
    private val deleteSnackbar: (
        items: Set<History>,
        undo: suspend (Set<History>) -> Unit,
        delete: (Set<History>) -> suspend (context: Context) -> Unit,
    ) -> Unit,
) : HistoryController {

    override fun handleDeleteTimeRange() {
        displayDeleteTimeRange.invoke()
    }

    override fun handleDeleteSome(items: Set<History>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.AddPendingDeletionSet(pendingDeletionItems))
        deleteSnackbar.invoke(items, ::undo, ::delete)
    }

    override fun handleDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?) {
        scope.launch {
            store.dispatch(HistoryFragmentAction.EnterDeletionMode)
            if (timeFrame == null) {
                historyStorage.deleteEverything()
            } else {
                val longRange = timeFrame.toLongRange()
                historyStorage.deleteVisitsBetween(
                    startTime = longRange.first,
                    endTime = longRange.last,
                )
            }
            when (timeFrame) {
                RemoveTimeFrame.LastHour -> GleanHistory.removedLastHour.record(NoExtras())
                RemoveTimeFrame.TodayAndYesterday -> GleanHistory.removedTodayAndYesterday.record(NoExtras())
                null -> GleanHistory.removedAll.record(NoExtras())
            }
            // We introduced more deleting options, but are keeping these actions for all options.
            // The approach could be improved: https://github.com/mozilla-mobile/fenix/issues/26102
            browserStore.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction)
            browserStore.dispatch(EngineAction.PurgeHistoryAction).join()

            store.dispatch(HistoryFragmentAction.ExitDeletionMode)

            launch(Dispatchers.Main) {
                onTimeFrameDeleted.invoke()
            }
        }
    }

    private fun undo(items: Set<History>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.UndoPendingDeletionSet(pendingDeletionItems))
    }

    private fun delete(items: Set<History>): suspend (context: Context) -> Unit {
        return { context ->
            CoroutineScope(Dispatchers.IO).launch {
                store.dispatch(HistoryFragmentAction.EnterDeletionMode)
                for (item in items) {
                    GleanHistory.removed.record(NoExtras())

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
