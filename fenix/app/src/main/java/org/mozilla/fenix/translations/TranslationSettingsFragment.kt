/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.translations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A fragment displaying the Firefox Translation settings screen.
 */
class TranslationSettingsFragment : Fragment(), UserInteractionHandler {
    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.translation_settings_toolbar_title))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            FirefoxTheme {
                TranslationSettings(
                    translationSwitchList = getTranslationSettingsSwitchList(),
                    onAutomaticTranslationClicked = {},
                    onDownloadLanguageClicked = {},
                    onNeverTranslationClicked = {},
                )
            }
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().popBackStack()
        findNavController().navigate(
            TranslationsDialogFragmentDirections.actionGlobalToTranslationOptionsDialogFragment(),
        )
        return true
    }
}
