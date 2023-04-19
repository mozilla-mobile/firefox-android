/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import mozilla.components.compose.cfr.CFRPopup
import mozilla.components.compose.cfr.CFRPopupProperties
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.components
import org.mozilla.fenix.compose.ComposeViewHolder
import org.mozilla.fenix.tabstray.NavigationInteractor
import org.mozilla.fenix.tabstray.TabsTrayFragment
import org.mozilla.fenix.tabstray.TabsTrayState
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.tabstray.TrayPagerAdapter
import org.mozilla.fenix.tabstray.inactivetabs.InactiveTabsList
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.TabsTray as TabsTrayMetrics

/**
 * The [ComposeViewHolder] for displaying the section of inactive tabs in [TrayPagerAdapter].
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param lifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param tabsTrayStore [TabsTrayStore] used to listen for changes to [TabsTrayState.inactiveTabs].
 * @param interactor [InactiveTabsInteractor] used to respond to interactions with the inactive tabs header
 * and the auto close dialog.
 */
@Suppress("LongParameterList")
class InactiveTabViewHolder(
    composeView: ComposeView,
    lifecycleOwner: LifecycleOwner,
    private val tabsTrayStore: TabsTrayStore,
    private val interactor: InactiveTabsInteractor,
    private val navigationInteractor: NavigationInteractor,
) : ComposeViewHolder(composeView, lifecycleOwner) {

    @Composable
    override fun Content() {
        val expanded = components.appStore
            .observeAsComposableState { state -> state.inactiveTabsExpanded }.value ?: false
        val inactiveTabs = tabsTrayStore
            .observeAsComposableState { state -> state.inactiveTabs }.value ?: emptyList()
        val showInactiveTabsAutoCloseDialog =
            components.settings.shouldShowInactiveTabsAutoCloseDialog(inactiveTabs.size)
        var showAutoClosePrompt by remember { mutableStateOf(showInactiveTabsAutoCloseDialog) }

        if (showInactiveTabsAutoCloseDialog) {
            TabsTrayMetrics.autoCloseSeen.record(NoExtras())
        }

        if (inactiveTabs.isNotEmpty()) {
            InactiveTabsList(
                inactiveTabs = inactiveTabs,
                expanded = expanded,
                showAutoCloseDialog = showAutoClosePrompt,
                onHeaderClick = { interactor.onInactiveTabsHeaderClicked(!expanded) },
                onDeleteAllButtonClick = interactor::onDeleteAllInactiveTabsClicked,
                onAutoCloseDismissClick = {
                    interactor.onAutoCloseDialogCloseButtonClicked()
                    showAutoClosePrompt = !showAutoClosePrompt
                },
                onEnableAutoCloseClick = {
                    interactor.onEnableAutoCloseClicked()
                    showAutoClosePrompt = !showAutoClosePrompt
                    showConfirmationSnackbar()
                },
                onTabClick = interactor::onInactiveTabClicked,
                onTabCloseClick = interactor::onInactiveTabClosed,
            )
            val settings = components.settings
            if (settings.shouldShowInactiveTabsOnboardingPopup &&
                settings.canShowCfr
            ) {
                CreateInactiveTabCFR(settings)
            }
        }
    }

    /**
     * Create the CFR needed to inform the user about inactive tabs.
     */
    @Composable
    @Suppress("MagicNumber")
    fun CreateInactiveTabCFR(settings: Settings) {
        CFRPopup(
            anchor = composeView,
            properties = CFRPopupProperties(
                popupBodyColors = listOf(
                    FirefoxTheme.colors.gradientEnd.toArgb(),
                    FirefoxTheme.colors.gradientStart.toArgb(),
                ),
                dismissButtonColor = FirefoxTheme.colors.iconOnColor.toArgb(),
                indicatorDirection = CFRPopup.IndicatorDirection.UP,
                popupVerticalOffset = (-25).dp, // Offset to point the arrow at the header of the list.
            ),
            onDismiss = {
                settings.shouldShowInactiveTabsOnboardingPopup = false
                org.mozilla.fenix.GleanMetrics.TabsTray.inactiveTabsCfrDismissed.record(
                    NoExtras(),
                )
            },
            text = {
                FirefoxTheme {
                    Text(
                        text = stringResource(R.string.tab_tray_inactive_onboarding_message),
                        color = FirefoxTheme.colors.textOnColorPrimary,
                        style = FirefoxTheme.typography.body1,
                    )
                }
            },
            action = {
                FirefoxTheme {
                    Text(
                        text = stringResource(R.string.tab_tray_inactive_onboarding_button_text),
                        color = FirefoxTheme.colors.textOnColorPrimary,
                        modifier = Modifier.clickable {
                            settings.shouldShowInactiveTabsOnboardingPopup = false
                            navigationInteractor.onTabSettingsClicked()
                            org.mozilla.fenix.GleanMetrics.TabsTray.inactiveTabsCfrSettings.record(
                                NoExtras(),
                            )
                        },
                        style = FirefoxTheme.typography.body1.copy(
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                }
            },
        ).show()
    }

    override val allowPrivateTheme: Boolean
        get() = false

    private fun showConfirmationSnackbar() {
        val context = composeView.context
        val text = context.getString(R.string.inactive_tabs_auto_close_message_snackbar)
        val snackbar = FenixSnackbar.make(
            view = composeView,
            duration = FenixSnackbar.LENGTH_SHORT,
            isDisplayedWithBrowserToolbar = true,
        ).setText(text)
        snackbar.view.elevation = TabsTrayFragment.ELEVATION
        snackbar.show()
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
