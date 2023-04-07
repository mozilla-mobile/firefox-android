package org.mozilla.fenix.summarize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.asLiveData
import mozilla.components.browser.state.selector.selectedTab
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentSummarizeBinding
import org.mozilla.fenix.ext.requireComponents

class SummarizeFragment : DialogFragment() {

    private var _binding: FragmentSummarizeBinding? = null
    private val binding get() = _binding!!

    private var summary: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CreateShortcutDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSummarizeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.doneButton.setOnClickListener { dismiss() }


        requireComponents.core.store.state.selectedTab?.content?.let { tabContent ->

            binding.dialogTitle.text = tabContent.title

            SummarizeUtil.getSummaryForUrl(tabContent.url).asLiveData()
                .observe(viewLifecycleOwner) { sum ->
                    summary += sum
                    binding.summaryTextView.text = summary
                }
        }


    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()

    }

}
