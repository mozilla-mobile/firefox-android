package org.mozilla.fenix.library.history.state

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.R
import org.mozilla.fenix.library.history.History

import org.mozilla.fenix.library.history.HistoryFragmentAction
import org.mozilla.fenix.library.history.HistoryFragmentDirections
import org.mozilla.fenix.library.history.HistoryFragmentState
import kotlin.coroutines.CoroutineContext

class HistoryNavigationMiddleware(
    private val navController: NavController,
    private val onBackPressed: () -> Unit,
    private val openToBrowser: (item: History.Regular) -> Unit,
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction
    ) {
        CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val state = context.store.state
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

                else -> Unit
            }
            next(action)
        }
    }
}
