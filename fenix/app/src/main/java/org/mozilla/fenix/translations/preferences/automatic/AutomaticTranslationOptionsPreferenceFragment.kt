/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.translations.preferences.automatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A fragment displaying the Firefox Preference Automatic Translation Options screen.
 */
class AutomaticTranslationOptionsPreferenceFragment : Fragment() {
    private val args by navArgs<AutomaticTranslationOptionsPreferenceFragmentArgs>()

    override fun onResume() {
        super.onResume()
        showToolbar(args.selectedTranslationOptionPreference.displayName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            FirefoxTheme {
                AutomaticTranslationOptionsPreference(
                    selectedOption = args.selectedTranslationOptionPreference.automaticTranslationOptionPreference,
                )
            }
        }
    }
}
