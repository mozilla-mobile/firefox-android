/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.net.toFileUri
import java.util.UUID

/**
 * Allows to launch a file picker to select, an add-on file to be installed.
 * @param context the application context
 * @param onInstallationFailed (optional) callback invoked if there was an error installing the add-on.
 *
 */
class AddonFilePicker(
    val context: Context,
    private val addonManager: AddonManager,
    private val onInstallationFailed: (() -> Unit) = { },
) {
    internal lateinit var activityLauncher: ActivityResultLauncher<Array<String>>
    private val logger = Logger("AddonFilePicker")

    /**
     * Launch an Android file picker, where the user can select an add-on file.
     * @returns a [Boolean] indicating if the file picker was launched successfully or not.
     */
    fun launch(): Boolean {
        return try {
            activityLauncher.launch(emptyArray())
            true
        } catch (e: ActivityNotFoundException) {
            logger.error("Unable to find an app to select an XPI file", e)
            false
        }
    }

    /**
     * Registers a lister for file picker results.
     * @param resultCaller The [ActivityResultCaller] on which results will be observed.
     */
    fun registerForResults(resultCaller: ActivityResultCaller) {
        activityLauncher =
            resultCaller.registerForActivityResult(AddonOpenDocument(), ::handleUriSelected)
    }

    internal fun handleUriSelected(uri: Uri?) {
        uri?.let {
            val fileUri = convertToFileUri(uri)
            val onSuccess: ((Addon) -> Unit) = {
                logger.info("Add-on from $fileUri installed successfully")
            }
            val onError: ((String, Throwable) -> Unit) = { _, exception ->
                onInstallationFailed()
                logger.info("Unable to install add-on from $fileUri")
            }
            addonManager.installAddon(
                Addon(id = UUID.randomUUID().toString(), downloadUrl = fileUri),
                onSuccess,
                onError,
            )
        }
    }

    internal fun convertToFileUri(uri: Uri): String = uri.toFileUri(context, "XPIs").toString()
}

internal open class AddonOpenDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        val intent = super.createIntent(context, input)
        // The default implementation of OpenDocument() adds,
        // for our use case it's not need.
        intent.removeExtra(Intent.EXTRA_MIME_TYPES)
        return intent
    }
}
