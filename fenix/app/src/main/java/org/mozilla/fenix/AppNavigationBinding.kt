package org.mozilla.fenix

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.addons.AddonDetailsFragmentDirections
import org.mozilla.fenix.addons.AddonPermissionsDetailsFragmentDirections
import org.mozilla.fenix.addons.AddonsManagementFragmentDirections
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.appstate.Screen
import org.mozilla.fenix.exceptions.trackingprotection.TrackingProtectionExceptionsFragmentDirections
import org.mozilla.fenix.ext.alreadyOnDestination
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.library.bookmarks.BookmarkFragmentDirections
import org.mozilla.fenix.library.history.HistoryFragmentDirections
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragmentDirections
import org.mozilla.fenix.library.recentlyclosed.RecentlyClosedFragmentDirections
import org.mozilla.fenix.search.SearchDialogFragmentDirections
import org.mozilla.fenix.settings.CookieBannersFragmentDirections
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

class AppNavigationBinding(
    appStore: AppStore,
    private val navController: NavController,
) : AbstractBinding<AppState>(appStore) {
    override suspend fun onState(flow: Flow<AppState>) = flow.distinctUntilChangedBy { it.screen }
        .collect {
            when (val screen = it.screen) {
                is Screen.Home -> {
                    navController.navigate(R.id.homeFragment)
                }
                is Screen.Browser -> {
                    val from = screen.from
                    if (navController.alreadyOnDestination(R.id.browserFragment)) return@collect
                    @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
                    val directions = getNavDirections(from, screen.customTabSessionId)
                    navController.nav(fragmentId, directions)
                }
            }
        }

    private fun getNavDirections(
        from: BrowserDirection,
        customTabSessionId: String?,
    ): NavDirections = when (from) {
        BrowserDirection.FromGlobal ->
            NavGraphDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHome ->
            HomeFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromWallpaper ->
            WallpaperSettingsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSearchDialog ->
            SearchDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSettings ->
            SettingsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromBookmarks ->
            BookmarkFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHistory ->
            HistoryFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHistoryMetadataGroup ->
            HistoryMetadataGroupFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTrackingProtectionExceptions ->
            TrackingProtectionExceptionsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromCookieBanner ->
            CookieBannersFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHttpsOnlyMode ->
            HttpsOnlyFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAbout ->
            AboutFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTrackingProtection ->
            TrackingProtectionFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTrackingProtectionDialog ->
            TrackingProtectionPanelDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSavedLoginsFragment ->
            SavedLoginsAuthFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddNewDeviceFragment ->
            AddNewDeviceFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSearchEngineFragment ->
            SearchEngineFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSaveSearchEngineFragment ->
            SaveSearchEngineFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddonDetailsFragment ->
            AddonDetailsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddonPermissionsDetailsFragment ->
            AddonPermissionsDetailsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromLoginDetailFragment ->
            LoginDetailFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTabsTray ->
            TabsTrayFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromRecentlyClosed ->
            RecentlyClosedFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromStudiesFragment -> StudiesFragmentDirections.actionGlobalBrowser(
            customTabSessionId,
        )
        BrowserDirection.FromReviewQualityCheck -> ReviewQualityCheckFragmentDirections.actionGlobalBrowser(
            customTabSessionId,
        )
        BrowserDirection.FromAddonsManagementFragment -> AddonsManagementFragmentDirections.actionGlobalBrowser(
            customTabSessionId,
        )
    }
}
