/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.cookiebanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.focus.GleanMetrics.CookieBanner
import org.mozilla.focus.R
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.settings
import org.mozilla.focus.ui.theme.FocusTheme
import org.mozilla.focus.utils.ViewUtils

/**
 * Displays a cookie banner dialog fragment that contains the dialog compose and his logic.
 */
class CookieBannerDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            FocusTheme {
                val cookieBannerDialogSelectedVariant =
                    CookieBannerDialogUtils.getCookieBannerSelectedVariant(requireContext())
                CookieBannerDialogCompose(
                    dialogTitle = cookieBannerDialogSelectedVariant.title,
                    dialogText = cookieBannerDialogSelectedVariant.message,
                    allowButtonText = requireContext().getString(R.string.cookie_banner_dialog_positive_button),
                    declineButtonText = getString(R.string.cookie_banner_dialog_negative_button),
                    onAllowButtonClicked = {
                        onConfirmDialogClicked()
                        dismiss()
                        ViewUtils.showBrandedSnackbar(
                            requireActivity().findViewById(android.R.id.content),
                            R.string.cookie_banner_snack_bar_text,
                            requireActivity().resources.getInteger(R.integer.erase_snackbar_delay),
                        )
                        CookieBanner.dialogAllowButton.record(NoExtras())
                    },
                    onDeclineButtonClicked = {
                        CookieBannerDialogUtils.setCookieBannerDialogDismissedTime(context)
                        dismiss()
                        CookieBanner.dialogDeclineButton.record(NoExtras())
                    },
                )
            }
        }
    }

    private fun onConfirmDialogClicked() {
        requireContext().settings.saveCurrentCookieBannerOptionInSharePref(
            CookieBannerOption.CookieBannerRejectOrAccept(),
        )
        requireContext().components.engine.settings.cookieBannerHandlingModePrivateBrowsing =
            CookieBannerOption.CookieBannerRejectOrAccept().mode
        requireContext().components.sessionUseCases.reload()
        CookieBannerDialogUtils.setCookieBannerDialogDisabled(requireContext())
    }

    companion object {
        const val FRAGMENT_TAG = "cookie-banner-dialog-fragment"
    }
}
