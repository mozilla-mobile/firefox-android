/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.extension

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.fenix.R
import org.mozilla.fenix.android.FenixDialogFragment
import org.mozilla.fenix.databinding.OverlayAddOnProgressBinding

/**
 * A [FenixDialogFragment] that shows an overlay while an extension file is been downloaded.
 */
class AddonDownloadingDialogFragment : FenixDialogFragment() {
    override val gravity: Int = Gravity.BOTTOM
    override val layoutId: Int = R.layout.overlay_add_on_progress
    private var _binding: OverlayAddOnProgressBinding? = null

    /**
     * A lambda called when the dialog is dismissed.
     */
    var onDismissed: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflateRootView(container)

        _binding = OverlayAddOnProgressBinding.bind(rootView)

        _binding?.cancelButton?.setOnClickListener { dismiss() }
        return rootView
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onDismissed?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
