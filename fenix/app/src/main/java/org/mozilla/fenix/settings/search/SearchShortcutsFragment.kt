/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import kotlinx.coroutines.MainScope
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentSearchShortcutsBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme
import org.mozilla.fenix.utils.allowUndo

/**
 * A [Fragment] that allows user to select what search engine shortcuts will be visible in the quick
 * search menu.
 */
class SearchShortcutsFragment : Fragment(R.layout.fragment_search_shortcuts) {

    lateinit var tabsTrayDialogBinding: FragmentSearchShortcutsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        tabsTrayDialogBinding = FragmentSearchShortcutsBinding.inflate(
            inflater,
            container,
            false,
        )

        tabsTrayDialogBinding.root.setContent {
            FirefoxTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                SearchEngineShortcuts(
                    getString(R.string.preferences_category_engines_in_search_menu),
                    requireComponents.core.store,
                    requireContext(),
                    onEditEngineClicked = {
                        val directions = SearchShortcutsFragmentDirections
                            .actionSearchEngineFragmentToEditCustomSearchEngineFragment(it.id)

                        Navigation.findNavController(requireView()).navigate(directions)
                    },
                    onDeleteEngineClicked = {
                        deleteSearchEngine(requireContext(), it)
                    },
                    onAddEngineClicked = {
                        val directions = SearchShortcutsFragmentDirections
                            .actionDefaultEngineFragmentToAddSearchEngineFragment()

                        Navigation.findNavController(requireView()).navigate(directions)
                    },
                )
            }
        }

        return tabsTrayDialogBinding.root
    }

    private fun deleteSearchEngine(
        context: Context,
        engine: SearchEngine,
    ) {
        val selectedOrDefaultSearchEngine = context.components.core.store.state.search.selectedOrDefaultSearchEngine
        if (selectedOrDefaultSearchEngine == engine) {
            val nextSearchEngine =
                if (context.settings().showUnifiedSearchFeature) {
                    context.components.core.store.state.search.searchEngines.firstOrNull {
                        it.id != engine.id && (it.isGeneral || it.type == SearchEngine.Type.CUSTOM)
                    }
                        ?: context.components.core.store.state.search.searchEngines.firstOrNull {
                            it.id != engine.id
                        }
                } else {
                    context.components.core.store.state.search.searchEngines.firstOrNull {
                        it.id != engine.id
                    }
                }

            nextSearchEngine?.let {
                context.components.useCases.searchUseCases.selectSearchEngine(
                    nextSearchEngine,
                )
            }
        }
        context.components.useCases.searchUseCases.removeSearchEngine(engine)

        MainScope().allowUndo(
            view = context.getRootView()!!,
            message = context
                .getString(R.string.search_delete_search_engine_success_message, engine.name),
            undoActionTitle = context.getString(R.string.snackbar_deleted_undo),
            onCancel = {
                context.components.useCases.searchUseCases.addSearchEngine(engine)
                if (selectedOrDefaultSearchEngine == engine) {
                    context.components.useCases.searchUseCases.selectSearchEngine(engine)
                }
            },
            operation = {},
        )
    }

    override fun onResume() {
        super.onResume()
        view?.hideKeyboard()
        showToolbar(getString(R.string.preferences_manage_search_shortcuts))
    }
}
