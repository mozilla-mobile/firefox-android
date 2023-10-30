/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.extension

import android.content.Context
import android.view.Gravity
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.state.extension.WebExtensionPromptRequest
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.webextension.WebExtensionInstallException
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.ui.AddonDialogFragment
import mozilla.components.feature.addons.ui.AddonInstallationDialogFragment
import mozilla.components.feature.addons.ui.PermissionsDialogFragment
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.android.content.appVersionName
import mozilla.components.ui.widgets.withCenterAlignedButtons
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.ThemeManager
import java.lang.ref.WeakReference

/**
 * Feature implementation for handling [WebExtensionPromptRequest] and showing the respective UI.
 */
class WebExtensionPromptFeature(
    private val store: BrowserStore,
    private val context: Context,
    private val addonManager: AddonManager = context.components.addonManager,
    private val fragmentManager: FragmentManager,
) : LifecycleAwareFeature {

    /**
     * (optional) callback invoked when an add-on was updated due to an interaction with a
     * [WebExtensionPromptRequest].
     * Won't be needed after https://bugzilla.mozilla.org/show_bug.cgi?id=1858484.
     */
    var onAddonChanged: (Addon) -> Unit = {}

    /**
     * Whether or not an add-on installation is in progress.
     */
    private var isInstallationInProgress = false
    private var scope: CoroutineScope? = null

    /**
     * Starts observing the selected session to listen for window requests
     * and opens / closes tabs as needed.
     */
    override fun start() {
        scope = store.flowScoped { flow ->
            flow.mapNotNull { state ->
                state.webExtensionPromptRequest
            }.distinctUntilChanged().collect { promptRequest ->

                when (promptRequest) {
                    is WebExtensionPromptRequest.AfterInstallation -> {
                        handleAfterInstallationRequest(promptRequest)
                    }

                    is WebExtensionPromptRequest.BeforeInstallation.InstallationFailed -> {
                        handleBeforeInstallationRequest(promptRequest)
                        consumePromptRequest()
                    }
                }
            }
        }
        tryToReAttachButtonHandlersToPreviousDialog()
    }

    @VisibleForTesting
    internal fun handleAfterInstallationRequest(promptRequest: WebExtensionPromptRequest.AfterInstallation) {
        val installedState = addonManager.toInstalledState(promptRequest.extension)
        val addon = Addon.newFromWebExtension(promptRequest.extension, installedState)
        when (promptRequest) {
            is WebExtensionPromptRequest.AfterInstallation.Permissions.Required -> handleRequiredPermissionRequest(
                addon,
                promptRequest,
            )

            is WebExtensionPromptRequest.AfterInstallation.Permissions.Optional -> handleOptionalPermissionsRequest(
                addon,
                promptRequest,
            )

            is WebExtensionPromptRequest.AfterInstallation.PostInstallation -> handlePostInstallationRequest(
                addon,
            )
        }
    }

    private fun handleBeforeInstallationRequest(promptRequest: WebExtensionPromptRequest.BeforeInstallation) {
        when (promptRequest) {
            is WebExtensionPromptRequest.BeforeInstallation.InstallationFailed -> {
                handleInstallationFailedRequest(
                    exception = promptRequest.exception,
                )
                consumePromptRequest()
            }
        }
    }

    private fun handlePostInstallationRequest(
        addon: Addon,
    ) {
        showPostInstallationDialog(addon)
    }

    private fun handleRequiredPermissionRequest(
        addon: Addon,
        promptRequest: WebExtensionPromptRequest.AfterInstallation.Permissions.Required,
    ) {
        showPermissionDialog(addon = addon, promptRequest = promptRequest)
    }

    @VisibleForTesting
    internal fun handleOptionalPermissionsRequest(
        addon: Addon,
        promptRequest: WebExtensionPromptRequest.AfterInstallation.Permissions.Optional,
    ) {
        val shouldGrantWithoutPrompt = Addon.localizePermissions(promptRequest.permissions, context).isEmpty()

        // If we don't have any promptable permissions, just proceed.
        if (shouldGrantWithoutPrompt) {
            handlePermissions(promptRequest, granted = true)
            return
        }

        showPermissionDialog(
            addon = addon,
            promptRequest = promptRequest,
            forOptionalPermissions = true,
            optionalPermissions = promptRequest.permissions,
        )
    }

    @VisibleForTesting
    internal fun handleInstallationFailedRequest(
        exception: WebExtensionInstallException,
    ) {
        val addonName = exception.extensionName ?: ""
        var title = context.getString(R.string.mozac_feature_addons_failed_to_install, "")
        val message = when (exception) {
            is WebExtensionInstallException.Blocklisted -> {
                context.getString(R.string.mozac_feature_addons_blocklisted_1, addonName)
            }

            is WebExtensionInstallException.UserCancelled -> {
                // We don't want to show an error message when users cancel installation.
                return
            }

            is WebExtensionInstallException.Unknown -> {
                // Making sure we don't have a
                // Title = Failed to install
                // Message = Failed to install $addonName
                title = ""
                if (addonName.isNotEmpty()) {
                    context.getString(R.string.mozac_feature_addons_failed_to_install, addonName)
                } else {
                    context.getString(R.string.mozac_feature_addons_failed_to_install_generic)
                }
            }

            is WebExtensionInstallException.NetworkFailure -> {
                context.getString(R.string.mozac_feature_addons_failed_to_install_network_error)
            }

            is WebExtensionInstallException.CorruptFile -> {
                context.getString(R.string.mozac_feature_addons_failed_to_install_corrupt_error)
            }

            is WebExtensionInstallException.NotSigned -> {
                context.getString(
                    R.string.mozac_feature_addons_failed_to_install_not_signed_error,
                )
            }

            is WebExtensionInstallException.Incompatible -> {
                val appName = context.getString(R.string.app_name)
                val version = context.appVersionName
                context.getString(
                    R.string.mozac_feature_addons_failed_to_install_incompatible_error,
                    addonName,
                    appName,
                    version,
                )
            }
        }

        showDialog(
            title = title,
            message = message,
        )
    }

    /**
     * Stops observing the selected session for incoming window requests.
     */
    override fun stop() {
        scope?.cancel()
    }

    @VisibleForTesting
    internal fun showPermissionDialog(
        addon: Addon,
        promptRequest: WebExtensionPromptRequest.AfterInstallation.Permissions,
        forOptionalPermissions: Boolean = false,
        optionalPermissions: List<String> = emptyList(),
    ) {
        if (isInstallationInProgress || hasExistingPermissionDialogFragment()) {
            return
        }

        val dialog = PermissionsDialogFragment.newInstance(
            addon = addon,
            forOptionalPermissions = forOptionalPermissions,
            optionalPermissions = optionalPermissions,
            promptsStyling = AddonDialogFragment.PromptsStyling(
                gravity = Gravity.BOTTOM,
                shouldWidthMatchParent = true,
                confirmButtonBackgroundColor = ThemeManager.resolveAttribute(
                    R.attr.accent,
                    context,
                ),
                confirmButtonTextColor = ThemeManager.resolveAttribute(
                    R.attr.textOnColorPrimary,
                    context,
                ),
                confirmButtonRadius =
                (context.resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat(),
            ),
            onPositiveButtonClicked = { handlePermissions(promptRequest, granted = true) },
            onNegativeButtonClicked = { handlePermissions(promptRequest, granted = false) },
        )
        dialog.show(
            fragmentManager,
            PERMISSIONS_DIALOG_FRAGMENT_TAG,
        )
    }

    private fun tryToReAttachButtonHandlersToPreviousDialog() {
        findPreviousPermissionDialogFragment()?.let { dialog ->
            dialog.onPositiveButtonClicked = { addon ->
                store.state.webExtensionPromptRequest?.let { promptRequest ->
                    if (promptRequest is WebExtensionPromptRequest.AfterInstallation.Permissions &&
                        addon.id == promptRequest.extension.id
                    ) {
                        handlePermissions(promptRequest, granted = true)
                    }
                }
            }
            dialog.onNegativeButtonClicked = {
                store.state.webExtensionPromptRequest?.let { promptRequest ->
                    if (promptRequest is WebExtensionPromptRequest.AfterInstallation.Permissions) {
                        handlePermissions(promptRequest, granted = false)
                    }
                }
            }
        }

        findPreviousPostInstallationDialogFragment()?.let { dialog ->
            dialog.onConfirmButtonClicked = { addon, allowInPrivateBrowsing ->
                store.state.webExtensionPromptRequest?.let { promptRequest ->
                    if (promptRequest is WebExtensionPromptRequest.AfterInstallation.PostInstallation &&
                        addon.id == promptRequest.extension.id
                    ) {
                        handlePostInstallationButtonClicked(
                            allowInPrivateBrowsing = allowInPrivateBrowsing,
                            context = WeakReference(context),
                            addon = addon,
                        )
                    }
                }
            }
            dialog.onDismissed = {
                store.state.webExtensionPromptRequest?.let { _ ->
                    consumePromptRequest()
                }
            }
        }
    }

    private fun handlePermissions(
        promptRequest: WebExtensionPromptRequest.AfterInstallation.Permissions,
        granted: Boolean,
    ) {
        promptRequest.onConfirm(granted)
        consumePromptRequest()
    }

    @VisibleForTesting
    internal fun consumePromptRequest() {
        store.dispatch(WebExtensionAction.ConsumePromptRequestWebExtensionAction)
    }

    private fun hasExistingPermissionDialogFragment(): Boolean {
        return findPreviousPermissionDialogFragment() != null
    }

    private fun hasExistingAddonPostInstallationDialogFragment(): Boolean {
        return fragmentManager.findFragmentByTag(POST_INSTALLATION_DIALOG_FRAGMENT_TAG)
            as? AddonInstallationDialogFragment != null
    }

    private fun findPreviousPermissionDialogFragment(): PermissionsDialogFragment? {
        return fragmentManager.findFragmentByTag(PERMISSIONS_DIALOG_FRAGMENT_TAG) as? PermissionsDialogFragment
    }

    private fun findPreviousPostInstallationDialogFragment(): AddonInstallationDialogFragment? {
        return fragmentManager.findFragmentByTag(
            POST_INSTALLATION_DIALOG_FRAGMENT_TAG,
        ) as? AddonInstallationDialogFragment
    }

    private fun showPostInstallationDialog(addon: Addon) {
        if (!isInstallationInProgress && !hasExistingAddonPostInstallationDialogFragment()) {
            // Fragment may not be attached to the context anymore during onConfirmButtonClicked handling,
            // but we still want to be able to process user selection of the 'allowInPrivateBrowsing' pref.
            // This is a best-effort attempt to do so - retain a weak reference to the application context
            // (to avoid a leak), which we attempt to use to access addonManager.
            // See https://github.com/mozilla-mobile/fenix/issues/15816
            val weakApplicationContext: WeakReference<Context> = WeakReference(context)

            val dialog = AddonInstallationDialogFragment.newInstance(
                addon = addon,
                promptsStyling = AddonDialogFragment.PromptsStyling(
                    gravity = Gravity.BOTTOM,
                    shouldWidthMatchParent = true,
                    confirmButtonBackgroundColor = ThemeManager.resolveAttribute(
                        R.attr.accent,
                        context,
                    ),
                    confirmButtonTextColor = ThemeManager.resolveAttribute(
                        R.attr.textOnColorPrimary,
                        context,
                    ),
                    confirmButtonRadius =
                    (context.resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat(),
                ),
                onDismissed = {
                    consumePromptRequest()
                },
                onConfirmButtonClicked = { _, allowInPrivateBrowsing ->
                    handlePostInstallationButtonClicked(
                        addon = addon,
                        context = weakApplicationContext,
                        allowInPrivateBrowsing = allowInPrivateBrowsing,
                    )
                },
            )
            dialog.show(fragmentManager, POST_INSTALLATION_DIALOG_FRAGMENT_TAG)
        }
    }

    private fun handlePostInstallationButtonClicked(
        context: WeakReference<Context>,
        allowInPrivateBrowsing: Boolean,
        addon: Addon,
    ) {
        if (allowInPrivateBrowsing) {
            context.get()?.components?.addonManager?.setAddonAllowedInPrivateBrowsing(
                addon = addon,
                allowed = true,
                onSuccess = { updatedAddon ->
                    onAddonChanged(updatedAddon)
                },
            )
        }
        consumePromptRequest()
    }

    @VisibleForTesting
    internal fun showDialog(
        title: String,
        message: String,
    ) {
        context.let {
            AlertDialog.Builder(it)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setCancelable(false)
                .setMessage(
                    message,
                )
                .show()
                .withCenterAlignedButtons()
        }
    }

    companion object {
        private const val PERMISSIONS_DIALOG_FRAGMENT_TAG = "ADDONS_PERMISSIONS_DIALOG_FRAGMENT"
        private const val POST_INSTALLATION_DIALOG_FRAGMENT_TAG =
            "ADDONS_INSTALLATION_DIALOG_FRAGMENT"
    }
}
