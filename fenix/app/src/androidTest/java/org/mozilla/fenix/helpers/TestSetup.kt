package org.mozilla.fenix.helpers

import android.util.Log
import kotlinx.coroutines.runBlocking
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.mozilla.fenix.components.PermissionStorage
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.IdlingResourceHelper.unregisterAllIdlingResources
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.ui.robots.notificationShade

open class TestSetup {
    lateinit var mockWebServer: MockWebServer

    private val permissionStorage = PermissionStorage(appContext.applicationContext)
    private val historyStorage = PlacesHistoryStorage(appContext.applicationContext)
    private val bookmarksStorage = PlacesBookmarksStorage(appContext.applicationContext)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
        }
        Log.i(TAG, "TestSetup: Starting the @Before setup")
        // Shutdown old mockWebServer instance, in case it's running.
        Log.i(TAG, "Shutting down mockWebServer")
        mockWebServer.shutdown()
        // Clear pre-existing notifications
        notificationShade {
            cancelAllShownNotifications()
        }
        runBlocking {
            // Reset locale to EN-US if needed.
            AppAndSystemHelper.resetSystemLocaleToEnUS()
            // Check and clear the downloads folder
            AppAndSystemHelper.clearDownloadsFolder()
            // Make sure the Wifi and Mobile Data connections are on
            AppAndSystemHelper.setNetworkEnabled(true)
            // Unregister any remaining idling resources
            unregisterAllIdlingResources()
            // add log check contents before and after
            permissionStorage.deleteAllSitePermissions()
            historyStorage.deleteEverything()
            val bookmarks = bookmarksStorage.getTree(BookmarkRoot.Mobile.id)?.children
            bookmarks?.forEach { bookmarksStorage.deleteNode(it.guid) }
        }
        // Start the mockWebServer
        try {
            Log.i(TAG, "Try starting mockWebServer")
            mockWebServer.start()
        } catch (e: Exception) {
            Log.i(TAG, "Re-starting mockWebServer")
            mockWebServer.shutdown()
            mockWebServer.start()
        }
    }
}
