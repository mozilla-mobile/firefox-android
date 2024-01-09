/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabhistory

import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.session.SessionUseCases
import org.junit.Before
import org.junit.Test

class TabHistoryControllerTest {

    private lateinit var goToHistoryIndexUseCase: SessionUseCases.GoToHistoryIndexUseCase
    private lateinit var currentItem: TabHistoryItem
    private lateinit var store: BrowserStore

    @Before
    fun setUp() {
        store = BrowserStore()
        goToHistoryIndexUseCase = spyk(SessionUseCases(store).goToHistoryIndex)
        currentItem = TabHistoryItem(
            index = 0,
            title = "",
            url = "",
        )
    }

    @Test
    fun handleGoToHistoryIndexNormalBrowsing() {
        val controller = DefaultTabHistoryController(
            goToHistoryIndexUseCase = goToHistoryIndexUseCase,
        )

        controller.handleGoToHistoryItem(currentItem)
        verify { goToHistoryIndexUseCase.invoke(currentItem.index) }
    }

    @Test
    fun handleGoToHistoryIndexCustomTab() {
        val customTabId = "customTabId"
        val customTabController = DefaultTabHistoryController(
            goToHistoryIndexUseCase = goToHistoryIndexUseCase,
            customTabId = customTabId,
        )

        customTabController.handleGoToHistoryItem(currentItem)
        verify { goToHistoryIndexUseCase.invoke(currentItem.index, customTabId) }
    }
}
