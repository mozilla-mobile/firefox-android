/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.SafeUrl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

private const val CLIPBOARD_URL = "https://www.mozilla.org"
private const val CLIPBOARD_TEXT = "Mozilla"

@RunWith(FenixRobolectricTestRunner::class)
class ClipboardHandlerTest {
    private val safeUrl = mock<SafeUrl>()
    private val bestWebUrlFunc = mock<((String) -> String?)>()
    private val isWebUrlFunc = mock<((String) -> Boolean)>()

    private lateinit var clipboard: ClipboardManager
    private lateinit var clipboardHandler: ClipboardHandler

    @Before
    fun setup() {
        clipboard = testContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardHandler =
            ClipboardHandler(testContext, clipboard, safeUrl, bestWebUrlFunc, isWebUrlFunc)
    }

    @Test
    fun `WHEN text is set THEN the bundle and text is set for the clipboard`() {
        `when`(isWebUrlFunc(CLIPBOARD_TEXT)).thenReturn(false)
        assertEquals(null, clipboardHandler.text)

        clipboardHandler.text = CLIPBOARD_TEXT
        val persistableBundle = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", false)
        }
        assertEquals(
            persistableBundle.toString(),
            clipboard.primaryClipDescription?.extras.toString(),
        )
        assertEquals(CLIPBOARD_TEXT, clipboardHandler.text)
    }

    @Test
    fun `WHEN sensitive text is set THEN the bundle and text is set for the clipboard`() {
        `when`(isWebUrlFunc(CLIPBOARD_TEXT)).thenReturn(false)
        assertEquals(null, clipboardHandler.sensitiveText)

        clipboardHandler.sensitiveText = CLIPBOARD_TEXT
        val persistableBundle = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        assertEquals(
            persistableBundle.toString(),
            clipboard.primaryClipDescription?.extras.toString(),
        )
        assertEquals(CLIPBOARD_TEXT, clipboardHandler.sensitiveText)
    }

    @Test
    fun `WHEN clipboard item is null THEN text returns null`() {
        assertEquals(null, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is TEXT_PLAIN and text is a URL THEN text returned as safe URL`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(ClipData.newPlainText("Text", CLIPBOARD_URL))

        `when`(isWebUrlFunc(CLIPBOARD_URL)).thenReturn(true)
        val expected = CLIPBOARD_URL.plus("1")
        `when`(safeUrl.stripUnsafeUrlSchemes(testContext, CLIPBOARD_URL)).thenReturn(expected)
        assertEquals(expected, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is TEXT_PLAIN and text is not a URL THEN text is returned unmodified`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(ClipData.newPlainText("Text", CLIPBOARD_TEXT))

        `when`(isWebUrlFunc(CLIPBOARD_TEXT)).thenReturn(false)
        assertEquals(CLIPBOARD_TEXT, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is TEXT_HTML and text is a URL THEN text is returned as safe URL`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(ClipData.newHtmlText("Html", CLIPBOARD_URL, CLIPBOARD_URL))

        `when`(isWebUrlFunc(CLIPBOARD_URL)).thenReturn(true)
        val expected = CLIPBOARD_URL.plus("1")
        `when`(safeUrl.stripUnsafeUrlSchemes(testContext, CLIPBOARD_URL)).thenReturn(expected)
        assertEquals(expected, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is TEXT_HTML and text is not a URL THEN text is returned unmodified`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(ClipData.newHtmlText("Html", CLIPBOARD_TEXT, CLIPBOARD_TEXT))

        `when`(isWebUrlFunc(CLIPBOARD_TEXT)).thenReturn(false)
        assertEquals(CLIPBOARD_TEXT, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is TEXT_URL THEN text is returned as safe URL`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(
            ClipData(
                CLIPBOARD_URL,
                arrayOf("text/x-moz-url"),
                ClipData.Item(CLIPBOARD_URL),
            ),
        )
        val expected = CLIPBOARD_URL.plus("1")
        `when`(safeUrl.stripUnsafeUrlSchemes(testContext, CLIPBOARD_URL)).thenReturn(expected)
        assertEquals(expected, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is not known THEN text returns null`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(
            ClipData(
                CLIPBOARD_URL,
                arrayOf("text/unknown"),
                ClipData.Item(CLIPBOARD_URL),
            ),
        )

        assertEquals(null, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item MimeType is unsupported THEN text returns null`() {
        assertEquals(null, clipboardHandler.text)

        clipboard.setPrimaryClip(ClipData.newIntent("Intent", Intent()))

        assertEquals(null, clipboardHandler.text)
    }

    @Test
    fun `WHEN clipboard item is null THEN extractURL returns null`() {
        assertEquals(null, clipboardHandler.extractURL())
    }

    @Test
    fun `WHEN text is not a valid URL extractURL THEN returns null`() {
        assertEquals(null, clipboardHandler.extractURL())

        clipboard.setPrimaryClip(ClipData.newPlainText("Text", CLIPBOARD_TEXT))

        `when`(isWebUrlFunc(CLIPBOARD_TEXT)).thenReturn(false)
        assertEquals(null, clipboardHandler.extractURL())
    }

    @Test
    fun `WHEN text is a valid URL extractURL THEN returns the URL`() {
        assertEquals(null, clipboardHandler.extractURL())

        clipboard.setPrimaryClip(ClipData.newPlainText("Text", CLIPBOARD_URL))

        `when`(isWebUrlFunc(CLIPBOARD_URL)).thenReturn(true)
        `when`(safeUrl.stripUnsafeUrlSchemes(testContext, CLIPBOARD_URL)).thenReturn(CLIPBOARD_URL)
        val expected = CLIPBOARD_URL.plus("1")
        `when`(bestWebUrlFunc(CLIPBOARD_URL)).thenReturn(expected)
        assertEquals(expected, clipboardHandler.extractURL())
    }
}
