/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.state.search.SearchEngine
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.databinding.FragmentAddSearchEngineBinding
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SupportUtils

/**
 * Fragment to enter a custom search engine name and URL template.
 */
class EditCustomSearchEngineFragment : Fragment(R.layout.fragment_add_search_engine) {

    private val args by navArgs<EditCustomSearchEngineFragmentArgs>()
    private lateinit var searchEngine: SearchEngine

    private var _binding: FragmentAddSearchEngineBinding? = null
    private val binding get() = _binding!!

    private val inputListener = object : TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            val bothFieldsHaveInput = binding.editEngineName.text?.isNotBlank() == true &&
                    binding.editSearchString.text?.isNotBlank() == true
            binding.saveButton.isEnabled = bothFieldsHaveInput
        }

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
            Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchEngine = requireNotNull(
            requireComponents.core.store.state.search.customSearchEngines.find { engine ->
                engine.id == args.searchEngineIdentifier
            },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = searchEngine.resultUrls[0]

        _binding = FragmentAddSearchEngineBinding.bind(view)

        binding.saveButton.apply {
            setOnClickListener { saveCustomEngine() }
        }

        binding.editEngineName.addTextChangedListener(inputListener)
        binding.editSearchString.addTextChangedListener(inputListener)

        binding.editEngineName.setText(searchEngine.name)
        binding.editSearchString.setText(url.toEditableUrl())

        binding.editSearchString.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    saveCustomEngine()
                    true
                }
                else -> false
            }
        }

        binding.customSearchEnginesLearnMore.setOnClickListener {
            (activity as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getSumoURLForTopic(
                    requireContext(),
                    SupportUtils.SumoTopic.CUSTOM_SEARCH_ENGINES,
                ),
                newTab = true,
                from = BrowserDirection.FromEditCustomSearchEngineFragment,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.search_engine_edit_custom_search_engine_title))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("LongMethod")
    private fun saveCustomEngine() {
        binding.customSearchEngineNameField.error = ""
        binding.customSearchEngineSearchStringField.error = ""

        val name = binding.editEngineName.text?.toString()?.trim() ?: ""
        val searchString = binding.editSearchString.text?.toString() ?: ""

        if (checkForErrors(name, searchString)) {
            return
        }

        lifecycleScope.launch(Main) {
            val result = withContext(IO) {
                SearchStringValidator.isSearchStringValid(
                    requireComponents.core.client,
                    searchString,
                )
            }

            when (result) {
                SearchStringValidator.Result.CannotReach -> {
                    binding.customSearchEngineSearchStringField.error = resources
                        .getString(R.string.search_add_custom_engine_error_cannot_reach, name)
                }

                SearchStringValidator.Result.Success -> {
                    val update = searchEngine.copy(
                        name = name,
                        resultUrls = listOf(searchString.toSearchUrl()),
                        icon = requireComponents.core.icons.loadIcon(IconRequest(searchString))
                            .await().bitmap,
                    )

                    requireComponents.useCases.searchUseCases.addSearchEngine(update)

                    val successMessage = resources
                        .getString(R.string.search_edit_custom_engine_success_message, name)

                    view?.also {
                        FenixSnackbar.make(
                            view = it,
                            duration = FenixSnackbar.LENGTH_SHORT,
                            isDisplayedWithBrowserToolbar = false,
                        )
                            .setText(successMessage)
                            .show()
                    }

                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun checkForErrors(name: String, searchString: String): Boolean {
        return when {
            name.isEmpty() -> {
                binding.customSearchEngineNameField.error = resources
                    .getString(R.string.search_add_custom_engine_error_empty_name)
                true
            }

            searchString.isEmpty() -> {
                binding.customSearchEngineSearchStringField.error =
                    resources.getString(R.string.search_add_custom_engine_error_empty_search_string)
                true
            }

            !searchString.contains("%s") -> {
                binding.customSearchEngineSearchStringField.error =
                    resources.getString(R.string.search_add_custom_engine_error_missing_template)
                true
            }
            else -> false
        }
    }
}

private fun String.toEditableUrl(): String {
    return replace("{searchTerms}", "%s")
}

private fun String.toSearchUrl(): String {
    return replace("%s", "{searchTerms}")
}
