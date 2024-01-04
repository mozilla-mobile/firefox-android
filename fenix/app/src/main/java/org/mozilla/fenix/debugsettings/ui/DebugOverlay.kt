/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import mozilla.components.lib.state.ext.observeAsState
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.annotation.LightDarkPreview
import org.mozilla.fenix.compose.button.FloatingActionButton
import org.mozilla.fenix.debugsettings.store.DebugDrawerAction
import org.mozilla.fenix.debugsettings.store.DebugDrawerState
import org.mozilla.fenix.debugsettings.store.DebugDrawerState.DrawerStatus
import org.mozilla.fenix.debugsettings.store.DebugDrawerStore
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Overlay for presenting Fenix-wide debugging content.
 *
 * @param debugDrawerStore [DebugDrawerStore] used to listen for changes to [DebugDrawerState] and
 * dispatch any [DebugDrawerAction]s.
 */
@Composable
fun DebugOverlay(
    debugDrawerStore: DebugDrawerStore,
) {
    val drawerStatus by debugDrawerStore.observeAsState(initialValue = DrawerStatus.Closed) { state ->
        state.drawerStatus
    }
    val snackbarState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(drawerStatus) {
        if (drawerStatus == DrawerStatus.Open) {
            drawerState.open()
        }
    }

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .distinctUntilChanged()
            .filter { it == DrawerValue.Closed }
            .collect {
                debugDrawerStore.dispatch(DebugDrawerAction.DrawerClosed)
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FloatingActionButton(
            icon = painterResource(R.drawable.ic_debug_transparent_fire_24),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            onClick = {
                debugDrawerStore.dispatch(DebugDrawerAction.DrawerOpened)
            },
        )

        // ModalDrawer utilizes a Surface, which blocks ALL clicks behind it, preventing the app
        // from being interactable. This cannot be overridden in the Surface API, so we must hide
        // the entire drawer when it is closed.
        if (drawerStatus == DrawerStatus.Open) {
            val currentLayoutDirection = LocalLayoutDirection.current
            val sheetLayoutDirection = when (currentLayoutDirection) {
                LayoutDirection.Rtl -> LayoutDirection.Ltr
                LayoutDirection.Ltr -> LayoutDirection.Rtl
            }

            // Force the drawer to always open from the opposite side of the screen. We need to reset
            // this below with `drawerContent` to ensure the content follows the correct direction.
            CompositionLocalProvider(LocalLayoutDirection provides sheetLayoutDirection) {
                ModalDrawer(
                    drawerContent = {
                        CompositionLocalProvider(LocalLayoutDirection provides currentLayoutDirection) {
                            DebugDrawer()
                        }
                    },
                    drawerBackgroundColor = FirefoxTheme.colors.layer1,
                    scrimColor = FirefoxTheme.colors.scrim,
                    drawerState = drawerState,
                    content = {},
                )
            }
        }

        // This must be the last element in the Box
        SnackbarHost(
            hostState = snackbarState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
            )
        }
    }
}

@Composable
@LightDarkPreview
private fun DebugOverlayPreview() {
    FirefoxTheme {
        DebugOverlay(
            debugDrawerStore = DebugDrawerStore(),
        )
    }
}
