/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.app.Activity
import androidx.navigation.NavDirections
import mozilla.components.concept.engine.manifest.WebAppManifestParser
import mozilla.components.feature.pwa.ext.getWebAppManifest
import org.mozilla.fenix.addons.AddonDetailsFragmentDirections
import org.mozilla.fenix.addons.AddonPermissionsDetailsFragmentDirections
import org.mozilla.fenix.addons.AddonsManagementFragmentDirections
import org.mozilla.fenix.customtabs.EXTRA_IS_SANDBOX_CUSTOM_TAB
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity
import org.mozilla.fenix.exceptions.trackingprotection.TrackingProtectionExceptionsFragmentDirections
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.library.bookmarks.BookmarkFragmentDirections
import org.mozilla.fenix.library.history.HistoryFragmentDirections
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragmentDirections
import org.mozilla.fenix.library.recentlyclosed.RecentlyClosedFragmentDirections
import org.mozilla.fenix.search.SearchDialogFragmentDirections
import org.mozilla.fenix.settings.HttpsOnlyFragmentDirections
import org.mozilla.fenix.settings.SettingsFragmentDirections
import org.mozilla.fenix.settings.TrackingProtectionFragmentDirections
import org.mozilla.fenix.settings.about.AboutFragmentDirections
import org.mozilla.fenix.settings.logins.fragment.LoginDetailFragmentDirections
import org.mozilla.fenix.settings.logins.fragment.SavedLoginsAuthFragmentDirections
import org.mozilla.fenix.settings.search.SaveSearchEngineFragmentDirections
import org.mozilla.fenix.settings.search.SearchEngineFragmentDirections
import org.mozilla.fenix.settings.studies.StudiesFragmentDirections
import org.mozilla.fenix.settings.wallpaper.WallpaperSettingsFragmentDirections
import org.mozilla.fenix.share.AddNewDeviceFragmentDirections
import org.mozilla.fenix.shopping.ReviewQualityCheckFragmentDirections
import org.mozilla.fenix.tabstray.TabsTrayFragmentDirections
import org.mozilla.fenix.trackingprotection.TrackingProtectionPanelDialogFragmentDirections
import java.security.InvalidParameterException

fun Activity.getNavDirections(
    from: BrowserDirection,
    customTabSessionId: String? = null,
): NavDirections? = when (this) {
    is ExternalAppBrowserActivity -> {
        getExternalAppBrowserNavDirections(from, customTabSessionId)
    }

    else -> {
        getHomeNavDirections(from)
    }
}

private fun Activity.getExternalAppBrowserNavDirections(
    from: BrowserDirection,
    customTabSessionId: String?,
): NavDirections? {
    if (customTabSessionId == null) {
        finishAndRemoveTask()
        return null
    }

    val manifest = intent
        .getWebAppManifest()
        ?.let { WebAppManifestParser().serialize(it).toString() }
    return when (from) {
        BrowserDirection.FromGlobal ->
            NavGraphDirections.actionGlobalExternalAppBrowser(
                activeSessionId = customTabSessionId,
                webAppManifest = manifest,
                isSandboxCustomTab = intent.getBooleanExtra(EXTRA_IS_SANDBOX_CUSTOM_TAB, false),
            )

        else -> throw InvalidParameterException(
            "Tried to navigate to ExternalAppBrowserFragment from $from",
        )
    }
}

private fun getHomeNavDirections(
    from: BrowserDirection,
): NavDirections = when (from) {
    BrowserDirection.FromGlobal ->
        NavGraphDirections.actionGlobalBrowser(null)

    BrowserDirection.FromHome ->
        HomeFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromWallpaper ->
        WallpaperSettingsFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromSearchDialog ->
        SearchDialogFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromSettings ->
        SettingsFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromBookmarks ->
        BookmarkFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromHistory ->
        HistoryFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromHistoryMetadataGroup ->
        HistoryMetadataGroupFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromTrackingProtectionExceptions ->
        TrackingProtectionExceptionsFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromHttpsOnlyMode ->
        HttpsOnlyFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromAbout ->
        AboutFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromTrackingProtection ->
        TrackingProtectionFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromTrackingProtectionDialog ->
        TrackingProtectionPanelDialogFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromSavedLoginsFragment ->
        SavedLoginsAuthFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromAddNewDeviceFragment ->
        AddNewDeviceFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromSearchEngineFragment ->
        SearchEngineFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromSaveSearchEngineFragment ->
        SaveSearchEngineFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromAddonDetailsFragment ->
        AddonDetailsFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromAddonPermissionsDetailsFragment ->
        AddonPermissionsDetailsFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromLoginDetailFragment ->
        LoginDetailFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromTabsTray ->
        TabsTrayFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromRecentlyClosed ->
        RecentlyClosedFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromStudiesFragment ->
        StudiesFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromReviewQualityCheck ->
        ReviewQualityCheckFragmentDirections.actionGlobalBrowser(null)

    BrowserDirection.FromAddonsManagementFragment ->
        AddonsManagementFragmentDirections.actionGlobalBrowser(null)
}
