package org.mozilla.fenix.library.history.state

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.navigateSafe
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentDirections
import org.mozilla.fenix.library.history.HistoryFragmentState
import org.mozilla.fenix.utils.Settings

/**
 * Middleware to initiate navigation side-effects in response to HistoryFragmentActions.
 */
class HistoryNavigationMiddleware(
    private val navController: NavController,
    private val settings: Settings,
    private val onBackPressed: () -> Unit,
    private val openToBrowser: (item: History.Regular) -> Unit,
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction,
    ) {
        val state = context.store.state
        // Seems a safe assumption that any navigation should be handled on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            when (action) {
                is HistoryFragmentAction.BackPressed -> {
                    if (state.mode is HistoryFragmentState.Mode.Editing) {
                        next(HistoryFragmentAction.ExitEditMode)
                    } else {
                        onBackPressed()
                    }
                }

                is HistoryFragmentAction.HistoryItemClicked -> {
                    if (state.mode.selectedItems.isEmpty()) {
                        when (val item = action.item) {
                            is History.Regular -> openToBrowser(item)
                            is History.Group -> {
                                navController.navigate(
                                    HistoryFragmentDirections.actionGlobalHistoryMetadataGroup(
                                        title = item.title,
                                        historyMetadataItems = item.items.toTypedArray(),
                                    ),
                                    NavOptions.Builder()
                                        .setPopUpTo(R.id.historyMetadataGroupFragment, true)
                                        .build(),
                                )
                            }

                            else -> Unit
                        }
                    }
                }
                is HistoryFragmentAction.SearchClicked -> {
                    val directions = if (settings.showUnifiedSearchFeature) {
                        HistoryFragmentDirections.actionGlobalSearchDialog(null)
                    } else {
                        HistoryFragmentDirections.actionGlobalHistorySearchDialog()
                    }

                    navController.navigateSafe(R.id.historyFragment, directions)
                }
                is HistoryFragmentAction.EnterRecentlyClosed -> {
                    navController.navigate(
                        HistoryFragmentDirections.actionGlobalRecentlyClosed(),
                        NavOptions.Builder().setPopUpTo(R.id.recentlyClosedFragment, true).build(),
                    )
                }
                else -> Unit
            }
            next(action)
        }
    }
}
