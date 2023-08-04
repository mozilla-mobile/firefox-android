/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.extension

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.state.action.WebExtensionAction.UpdatePromptRequestWebExtensionAction
import mozilla.components.browser.state.state.extension.WebExtensionPromptRequest.PreInstallation
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(FenixRobolectricTestRunner::class)
class WebExtensionPromptFeatureTest {

    private lateinit var webExtensionPromptFeature: WebExtensionPromptFeature
    private lateinit var store: BrowserStore

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @Before
    fun setup() {
        store = BrowserStore()
        webExtensionPromptFeature = spyk(
            WebExtensionPromptFeature(
                store = store,
                provideAddons = { emptyList() },
                context = testContext,
                view = mockk(relaxed = true),
                fragmentManager = mockk(relaxed = true),
            ),
        )
    }

    @Test
    fun `WHEN DownloadStarted is dispatched THEN showDownloadDialog`() {
        assertNull(webExtensionPromptFeature.activeDownloadingDialog)

        webExtensionPromptFeature.start()

        every { webExtensionPromptFeature.showDownloadDialog() } returns mockk()

        store.dispatch(UpdatePromptRequestWebExtensionAction(PreInstallation.DownloadStarted))
            .joinBlocking()

        verify { webExtensionPromptFeature.showDownloadDialog() }
        assertNotNull(webExtensionPromptFeature.activeDownloadingDialog)
    }

    @Test
    fun `WHEN DownloadEnded is dispatched THEN clear download dialog reference`() {
        val activeDialog: WeakReference<AddonDownloadingDialogFragment> = mockk(relaxed = true)

        webExtensionPromptFeature.start()

        webExtensionPromptFeature.activeDownloadingDialog = activeDialog

        assertNotNull(webExtensionPromptFeature.activeDownloadingDialog)

        store.dispatch(UpdatePromptRequestWebExtensionAction(PreInstallation.DownloadEnded))
            .joinBlocking()

        verify { activeDialog.clear() }
    }

    @Test
    fun `WHEN DownloadCancelled is dispatched THEN clear download dialog reference`() {
        val activeDialog: WeakReference<AddonDownloadingDialogFragment> = mockk(relaxed = true)

        webExtensionPromptFeature.start()

        webExtensionPromptFeature.activeDownloadingDialog = activeDialog

        assertNotNull(webExtensionPromptFeature.activeDownloadingDialog)

        store.dispatch(UpdatePromptRequestWebExtensionAction(PreInstallation.DownloadCancelled))
            .joinBlocking()

        verify { activeDialog.clear() }
    }

    @Test
    fun `WHEN DownloadFailed is dispatched THEN clear download dialog reference`() {
        val activeDialog: WeakReference<AddonDownloadingDialogFragment> = mockk(relaxed = true)

        webExtensionPromptFeature.start()

        webExtensionPromptFeature.activeDownloadingDialog = activeDialog

        assertNotNull(webExtensionPromptFeature.activeDownloadingDialog)

        store.dispatch(UpdatePromptRequestWebExtensionAction(PreInstallation.DownloadFailed))
            .joinBlocking()

        verify { activeDialog.clear() }
    }

    @Test
    fun `WHEN dismissing the download dialog THEN clear download dialog reference and consumePromptRequest`() {
        val activeDialog: WeakReference<AddonDownloadingDialogFragment> = mockk(relaxed = true)

        every { webExtensionPromptFeature.consumePromptRequest() } just runs

        webExtensionPromptFeature.activeDownloadingDialog = activeDialog

        val dialog = webExtensionPromptFeature.showDownloadDialog()

        dialog.onDismissed!!.invoke()

        verify { activeDialog.clear() }
        verify { webExtensionPromptFeature.consumePromptRequest() }
    }
}
