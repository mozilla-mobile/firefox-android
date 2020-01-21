/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.R

/**
 * An adapter for displaying unsupported add-on items.
 *
 * @property addonManager Manager of installed and recommended [Addon]s and manages their states.
 * @property unsupportedAddonsAdapterDelegate Delegate that will provides callbacks for handling
 * any interactions with the unsupported add-ons to the app to handle.
 * @property unsupportedAddons The list of unsupported add-ons based on the AMO store.
 */
class UnsupportedAddonsAdapter(
    private val addonManager: AddonManager,
    private val unsupportedAddonsAdapterDelegate: UnsupportedAddonsAdapterDelegate,
    private val unsupportedAddons: List<Addon>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as UnsupportedAddonViewHolder
        val addon = unsupportedAddons[position]

        holder.titleView.text =
            if (addon.translatableName.isNotEmpty()) {
                addon.translatableName.translate()
            } else {
                addon.id
            }

        holder.removeButton.setOnClickListener {
            addonManager.uninstallAddon(addon,
                onSuccess = {
                    unsupportedAddonsAdapterDelegate.onUninstallSuccess()
                },
                onError = { addonId, throwable ->
                    unsupportedAddonsAdapterDelegate.onUninstallError(addonId, throwable)
                })
        }
    }

    override fun getItemCount(): Int {
        return unsupportedAddons.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.mozac_feature_addons_unsupported_item, parent, false)

        val iconView = view.findViewById<ImageView>(R.id.add_on_icon)
        val titleView = view.findViewById<TextView>(R.id.add_on_name)
        val removeButton = view.findViewById<ImageButton>(R.id.add_on_remove_button)

        return UnsupportedAddonViewHolder(view, iconView, titleView, removeButton)
    }

    /**
     * A view holder for displaying unsupported add-on items.
     */
    class UnsupportedAddonViewHolder(
        view: View,
        val iconView: ImageView,
        val titleView: TextView,
        val removeButton: ImageButton
    ) : RecyclerView.ViewHolder(view)
}
