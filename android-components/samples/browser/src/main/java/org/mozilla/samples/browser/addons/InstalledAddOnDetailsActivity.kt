/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.addons

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import mozilla.components.feature.addons.AddOn
import org.mozilla.samples.browser.R

/**
 * An activity to show the details of a installed add-on.
 */
class InstalledAddOnDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_installed_add_on_details)
        val addOn = requireNotNull(intent.getParcelableExtra<AddOn>("add_on"))
        bind(addOn)
    }

    private fun bind(addOn: AddOn) {
        title = addOn.translatableName.translate()

        bindEnableSwitch(addOn.enabled)

        bindSettings()

        bindDetails(addOn)

        bindPermissions(addOn)
    }

    private fun bindVersion(addOn: AddOn) {
        val versionView = findViewById<TextView>(R.id.version_text)
        versionView.text = addOn.version
    }

    private fun bindEnableSwitch(enabled: Boolean) {
        val switch = findViewById<Switch>(R.id.enable_switch)
        switch.setState(enabled)
        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.setState(isChecked)
        }
    }

    private fun bindSettings() {
        findViewById<View>(R.id.settings).setOnClickListener {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindDetails(addOn: AddOn) {
        findViewById<View>(R.id.details).setOnClickListener {
            val intent = Intent(this, AddOnDetailsActivity::class.java)
            intent.putExtra("add_on", addOn)
            this.startActivity(intent)
        }
    }

    private fun bindPermissions(addOn: AddOn) {
        findViewById<View>(R.id.permissions).setOnClickListener {
            val intent = Intent(this, PermissionsDetailsActivity::class.java)
            intent.putExtra("add_on", addOn)
            this.startActivity(intent)
        }
    }

    private fun bindRemoveButton() {
        findViewById<View>(R.id.remove_add_on).setOnClickListener {
            Toast.makeText(this, "Removed Add-on", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Switch.setState(checked: Boolean) {
        val text = if (checked) {
            R.string.addon_settings_on
        } else {
            R.string.addon_settings_off
        }
        setText(text)
        isChecked = checked
    }
}
