/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabhistory

import android.view.View
import androidx.compose.ui.platform.ComposeView
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class TabHistoryBottomSheetTest {

    private val tabHistoryComposeView: ComposeView = mockk(relaxed = true)

    private lateinit var tabHistoryBottomSheet: TabHistoryBottomSheet

    @Before
    fun setup() {
        tabHistoryBottomSheet = spy(TabHistoryBottomSheet(null, tabHistoryComposeView))
    }

    @Test
    fun `GIVEN tabHistoryBottomSheet WHEN show method gets called and compose view visibility is Gone THEN initTabHistoryBottomSheet should get called`() {
        every { tabHistoryComposeView.visibility } returns View.GONE
        doNothing().`when`(tabHistoryBottomSheet).initTabHistoryBottomSheet()

        tabHistoryBottomSheet.show()

        verify(tabHistoryBottomSheet).initTabHistoryBottomSheet()
    }

    @Test
    fun `GIVEN tabHistoryBottomSheet WHEN show method gets called and compose view visibility is VISIBLE THEN initTabHistoryBottomSheet should not get called`() {
        every { tabHistoryComposeView.visibility } returns View.VISIBLE
        doNothing().`when`(tabHistoryBottomSheet).initTabHistoryBottomSheet()

        tabHistoryBottomSheet.show()

        verify(tabHistoryBottomSheet, never()).initTabHistoryBottomSheet()
    }
}
