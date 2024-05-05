package org.mozilla.samples.browser.summarize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.samples.browser.databinding.FragmentSummaryBinding

const val ARG_SUMMARIZATION_TEXT = "summarization_text"

class SummaryFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSummaryBinding? = null

    private val binding get() = _binding!!

    private val viewModel: SummarizeViewModel by lazy {
        SummarizeViewModel()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val text = arguments?.getString(ARG_SUMMARIZATION_TEXT) ?: ""
        summarizeAndUpdate(text)
    }

    private fun summarizeAndUpdate(text: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.summarizedContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val summarizedText = viewModel.summarizeText(text)

            withContext(Dispatchers.Main) {
                binding.summarizedContent.text = summarizedText
                binding.progressBar.visibility = View.GONE
                binding.summarizedContent.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun newInstance(text: String): SummaryFragment =
            SummaryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUMMARIZATION_TEXT, text)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}