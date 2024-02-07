/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.fenix.compose.LongPressIconButton
import org.mozilla.fenix.compose.TabCounter
import org.mozilla.fenix.compose.annotation.LightDarkPreview
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

/**
 * Top-level UI for displaying the navigation bar.
 *
 * @param buttons A list of [NavBarButton] used to populate the bar.
 * @param tabCount The number of opened tabs.
 */
@Composable
fun NavigationBar(
    buttons: List<NavBarButton>,
    tabCount: Int,
) {
    Box(
        modifier = Modifier
            .background(FirefoxTheme.colors.layer1)
            .height(48.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            buttons.forEach { button ->
                when (button) {
                    is NavBarButton.ActionButton -> {
                        LongPressIconButton(
                            onClick = { button.onClick() },
                            onLongClick = { button.onLongPress() },
                        ) {
                            Icon(
                                painter = painterResource(button.iconId),
                                stringResource(id = button.descriptionResourceId),
                                tint = FirefoxTheme.colors.iconPrimary,
                            )
                        }
                    }
                    is NavBarButton.ThreeDotMenuButton -> {
                        AndroidView(
                            modifier = Modifier.size(48.dp),
                            factory = { _ -> button.menuButton },
                        )
                    }
                    is NavBarButton.TabCounterButton -> {
                        CompositionLocalProvider(LocalContentColor provides FirefoxTheme.colors.iconPrimary) {
                            IconButton(onClick = { button.onClick() }) {
                                TabCounter(tabCount = tabCount)
                            }
                        }
                    }
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun NavigationBarPreview() {
    FirefoxTheme {
        NavigationBar(
            buttons = DefaultButtons.defaultItems,
            tabCount = 0,
        )
    }
}

@Preview
@Composable
private fun NavigationBarPrivatePreview() {
    FirefoxTheme(theme = Theme.Private) {
        NavigationBar(
            buttons = DefaultButtons.defaultItems,
            tabCount = 0,
        )
    }
}
