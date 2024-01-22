/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.privatebrowsing

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.openToBrowserAndLoad
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.privatebrowsing.controller.DefaultPrivateBrowsingController
import org.mozilla.fenix.utils.Settings

class DefaultPrivateBrowsingControllerTest {

    private val activity: HomeActivity = mockk(relaxed = true)
    private val appStore: AppStore = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)

    private lateinit var store: BrowserStore
    private lateinit var controller: DefaultPrivateBrowsingController

    @Before
    fun setup() {
        store = BrowserStore()
        controller = DefaultPrivateBrowsingController(
            activity = activity,
            appStore = appStore,
            navController = navController,
        )

        every { appStore.state } returns AppState()

        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }
        every { activity.components.settings } returns settings
        every { activity.settings() } returns settings
    }

    @Test
    fun `WHEN private browsing learn more link is clicked THEN open support page in browser`() {
        val learnMoreURL = "https://support.mozilla.org/en-US/kb/common-myths-about-private-browsing?as=u&utm_source=inproduct"

        mockkStatic(FragmentActivity::openToBrowserAndLoad)
        every {
            activity.openToBrowserAndLoad(
                navController = activity.navHost.navController,
                searchTermOrURL = learnMoreURL,
                newTab = true,
                from = BrowserDirection.FromHome,
                browsingMode = activity.browsingModeManager.mode,
            )
        } just Runs

        controller.handleLearnMoreClicked()

        verify {
            activity.openToBrowserAndLoad(
                navController = activity.navHost.navController,
                searchTermOrURL = learnMoreURL,
                newTab = true,
                from = BrowserDirection.FromHome,
                browsingMode = activity.browsingModeManager.mode,
            )
        }
        unmockkStatic(FragmentActivity::openToBrowserAndLoad)
    }

    @Test
    fun `WHEN private mode button is selected from home THEN handle mode change`() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }

        every { settings.incrementNumTimesPrivateModeOpened() } just Runs

        val newMode = BrowsingMode.Private

        controller.handlePrivateModeButtonClicked(newMode)

        verify {
            settings.incrementNumTimesPrivateModeOpened()
            AppAction.ModeChange(newMode)
        }
    }

    @Test
    fun `WHEN private mode is selected on home from behind search THEN handle mode change`() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.searchDialogFragment
        }

        every { settings.incrementNumTimesPrivateModeOpened() } just Runs

        val url = "https://mozilla.org"
        val tab = createTab(
            id = "otherTab",
            url = url,
            private = false,
            engineSession = mockk(relaxed = true),
        )
        store.dispatch(TabListAction.AddTabAction(tab, select = true)).joinBlocking()

        val newMode = BrowsingMode.Private

        controller.handlePrivateModeButtonClicked(newMode)

        verify {
            settings.incrementNumTimesPrivateModeOpened()
            AppAction.ModeChange(newMode)
            navController.navigate(
                BrowserFragmentDirections.actionGlobalSearchDialog(
                    sessionId = null,
                ),
            )
        }
    }

    @Test
    fun `WHEN private mode is deselected on home from behind search THEN handle mode change`() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.searchDialogFragment
        }

        val url = "https://mozilla.org"
        val tab = createTab(
            id = "otherTab",
            url = url,
            private = true,
            engineSession = mockk(relaxed = true),
        )
        store.dispatch(TabListAction.AddTabAction(tab, select = true)).joinBlocking()

        val newMode = BrowsingMode.Normal

        controller.handlePrivateModeButtonClicked(newMode)

        verify(exactly = 0) {
            settings.incrementNumTimesPrivateModeOpened()
        }
        verify {
            appStore.dispatch(
                AppAction.ModeChange(newMode),
            )
            navController.navigate(
                BrowserFragmentDirections.actionGlobalSearchDialog(
                    sessionId = null,
                ),
            )
        }
    }
}
