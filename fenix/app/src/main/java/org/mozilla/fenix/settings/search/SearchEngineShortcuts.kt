/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.availableSearchEngines
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.FirefoxTheme

@Composable
fun SearchEngineShortcuts(
    categoryTitle: String,
    store: BrowserStore,
    context: Context,
    onEditEngineClicked: (SearchEngine) -> Unit,
    onDeleteEngineClicked: (SearchEngine) -> Unit,
    onAddEngineClicked: () -> Unit,
) {
    val searchState = store.observeAsComposableState { it.search }.value ?: SearchState()
    var searchEngines = with(searchState) {
        regionSearchEngines + additionalSearchEngines + availableSearchEngines + customSearchEngines
    }
    val disabledShortcuts = searchState.disabledSearchEngineShortcutIds

    Column(
        modifier = Modifier.background(color = FirefoxTheme.colors.layer1),
    ) {
        Title(title = categoryTitle)
        Spacer(modifier = Modifier.height(12.dp))
        SearchItems(
            searchEngines,
            disabledShortcuts,
            context,
            onEditEngineClicked,
            onDeleteEngineClicked,
        )
        AddEngineButton(onAddEngineClicked)
    }
}

@Composable
fun Title(title: String) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = FirefoxTheme.colors.textAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = FirefoxTheme.typography.headline8,
        )
    }
}

@Composable
fun SearchItems(
    engines: List<SearchEngine>,
    disabledShortcuts: List<String>,
    context: Context,
    onEditEngineClicked: (SearchEngine) -> Unit,
    onDeleteEngineClicked: (SearchEngine) -> Unit,
) {
    LazyColumn {
        items(items = engines, key = { engine -> engine.id }) {
            SearchItem(
                it,
                it.name,
                context,
                !disabledShortcuts.contains(it.id),
                onEditEngineClicked,
                onDeleteEngineClicked,
            )
        }
    }
}

@Composable
fun AddEngineButton(
    onAddEngineClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = 4.dp)
            .clickable { onAddEngineClicked() },
    ) {
        Spacer(modifier = Modifier.width(68.dp))
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = R.drawable.ic_new),
            contentDescription = "",
            tint = FirefoxTheme.colors.iconPrimary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(top = 8.dp, bottom = 8.dp)
                .weight(1f),
            text = stringResource(id = R.string.search_engine_add_custom_search_engine_title),
            color = FirefoxTheme.colors.textPrimary,
            style = FirefoxTheme.typography.subtitle1,
        )
    }
}

@Composable
fun SearchItem(
    engine: SearchEngine,
    name: String,
    context: Context,
    isChecked: Boolean,
    onEditEngineClicked: (SearchEngine) -> Unit,
    onDeleteEngineClicked: (SearchEngine) -> Unit,
) {
    val isMenuExpanded: MutableState<Boolean> = remember { mutableStateOf(false) }


    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = 4.dp),
    ) {
        Checkbox(
            modifier = Modifier.align(Alignment.CenterVertically),
            checked = isChecked,
            onCheckedChange = {
                context.components.useCases.searchUseCases.updateSearchEngineShortcutUseCase(
                    engine,
                    it,
                )
            },
            colors = CheckboxDefaults.colors(
                checkedColor = FirefoxTheme.colors.formSelected,
            ),
        )
        Spacer(modifier = Modifier.width(20.dp))
        Image(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically),
            bitmap = engine.icon.asImageBitmap(), contentDescription = "",
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(top = 8.dp, bottom = 8.dp)
                .weight(1f),
            text = name,
            style = FirefoxTheme.typography.subtitle1,
            color = FirefoxTheme.colors.textPrimary,
        )
        if (engine.type == SearchEngine.Type.CUSTOM) {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                IconButton(onClick = { isMenuExpanded.value = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "",
                        tint = FirefoxTheme.colors.iconPrimary,
                    )
                    DropdownMenu(
                        expanded = isMenuExpanded.value,
                        onDismissRequest = { isMenuExpanded.value = false },
                        offset = DpOffset(x = 0.dp, y = -24.dp),
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                onEditEngineClicked(engine)
                                isMenuExpanded.value = false
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.search_engine_edit),
                                color = FirefoxTheme.colors.textWarning,
                                fontSize = 14.sp,
                                style = FirefoxTheme.typography.headline8,
                            )
                        }

                        DropdownMenuItem(
                            onClick = {
                                onDeleteEngineClicked(engine)
                                isMenuExpanded.value = false
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.search_engine_delete),
                                color = FirefoxTheme.colors.textWarning,
                                fontSize = 14.sp,
                                style = FirefoxTheme.typography.headline8,
                            )
                        }

                    }
                }
            }
        }
    }
}


