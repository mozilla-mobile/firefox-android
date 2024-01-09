/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.FaviconListItem
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Bottom sheet, compose view for tab history
 * @property tabHistoryListItems The list of elements that will appear on the bottom sheet.
 * @property selectedTabHistoryItem The item that is currently selected.
 * @property onClick When the user presses on the item from the list.
 * @property onDismiss When the bottom sheet gets dismissed.
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun TabHistoryBottomSheetView(
    tabHistoryListItems: List<TabHistoryItem>,
    selectedTabHistoryItem: TabHistoryItem? = null,
    onClick: (TabHistoryItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
        confirmValueChange = {
            if (it == ModalBottomSheetValue.Hidden) {
                onDismiss()
            }
            true
        },
    )

    SideEffect {
        scope.launch {
            modalBottomSheetState.show()
        }
    }

    TabHistoryBottomSheetLayout(
        tabHistoryListItems,
        selectedTabHistoryItem,
        onClick,
        modalBottomSheetState,
        onDismiss,
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun TabHistoryBottomSheetLayout(
    tabHistoryListItems: List<TabHistoryItem>,
    selectedTabHistoryItem: TabHistoryItem? = null,
    onClick: (TabHistoryItem) -> Unit,
    sheetState: ModalBottomSheetState,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    selectedTabHistoryItem?.let {
        SideEffect {
            coroutineScope.launch {
                listState.scrollToItem(it.index)
            }
        }
    }
    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetState = sheetState,
        sheetBackgroundColor = colorResource(id = R.color.fx_mobile_layer_color_1),
        scrimColor = Color.Transparent,
        sheetContent = {
            FirefoxTheme {
                Divider(
                    color = Color.Gray,
                    thickness = 3.dp,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(50.dp),
                )
                LazyColumn(
                    state = listState,
                ) {
                    items(tabHistoryListItems) { item ->
                        var rowModifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.home_item_horizontal_margin),
                            )

                        if (selectedTabHistoryItem?.index == item.index) {
                            rowModifier =
                                rowModifier.then(Modifier.background(FirefoxTheme.colors.layerAccentNonOpaque))
                        }
                        Column(rowModifier) {
                            FaviconListItem(
                                label = item.title,
                                description = item.url,
                                url = item.url,
                                onClick = {
                                    onClick(item)
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    ) {
    }
}
