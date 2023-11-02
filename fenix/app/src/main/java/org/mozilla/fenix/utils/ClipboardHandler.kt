/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.view.textclassifier.TextClassifier
import androidx.core.content.getSystemService
import mozilla.components.support.ktx.kotlin.MAX_URI_LENGTH
import mozilla.components.support.utils.SafeUrl
import mozilla.components.support.utils.WebURLFinder
import org.mozilla.fenix.perf.Performance.logger

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
private const val MIME_TYPE_TEXT_HTML = "text/html"
private const val MIME_TYPE_TEXT_URL = "text/x-moz-url"

/**
 * A clipboard utility class that allows copying and pasting links/text to & from the clipboard.
 */
class ClipboardHandler(
    private val context: Context,
    private val clipboard: ClipboardManager = context.getSystemService<ClipboardManager>()!!,
    private val safeUrl: SafeUrl = SafeUrl,
    private val bestWebUrlFunc: (String) -> String? = { url: String -> WebURLFinder(url).bestWebURL() },
    private val isWebUrlFunc: (String) -> Boolean = { url: String -> WebURLFinder.isWebURL(url) },
) {
    /**
     * Provides access to the current content of the clipboard, be aware this is a sensitive
     * API as from Android 12 and above, accessing it will trigger a notification letting the user
     * know the app has accessed the clipboard, make sure when you call this API that users are
     * completely aware that we are accessing the clipboard.
     * See for more details https://github.com/mozilla-mobile/fenix/issues/22271.
     */
    var text: String?
        get() = when {
            !isSupportedMimeType() -> null
            primaryClipTextIsUrl() -> firstSafePrimaryClipItemUrl()
            containsText() -> firstPrimaryClipItem()?.text?.toString()
            else -> null
        }
        set(value) {
            setPrimaryClipData(value, false)
        }

    /**
     * Provides access to the sensitive content of the clipboard, be aware this is a sensitive
     * API as from Android 12 and above, accessing it will trigger a notification letting the user
     * know the app has accessed the clipboard, make sure when you call this API that users are
     * completely aware that we are accessing the clipboard.
     * See for more details https://github.com/mozilla-mobile/fenix/issues/22271.
     *
     */
    var sensitiveText: String?
        get() {
            return text
        }
        set(value) {
            setPrimaryClipData(value, true)
        }

    private fun setPrimaryClipData(value: String?, isSensitive: Boolean) {
        val clipData = ClipData.newPlainText("Text", value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clipData.apply {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", isSensitive)
                }
            }
        }
        clipboard.setPrimaryClip(clipData)
    }

    /**
     * Returns a possible URL from the actual content of the clipboard, be aware this is a sensitive
     * API as from Android 12 and above, accessing it will trigger a notification letting the user
     * know the app has accessed the clipboard, make sure when you call this API that users are
     * completely aware that we are accessing the clipboard.
     * See for more details https://github.com/mozilla-mobile/fenix/issues/22271.
     */
    fun extractURL() = text?.let {
        if (it.isValidUrl()) {
            bestWebUrlFunc(it)
        } else {
            null
        }
    }

    /**
     * Checks whether [text] is a valid URL based on the given constraints.
     */
    private fun String.isValidUrl() = primaryClipTextIsUrl() && isValidUriLength()

    private fun String.isValidUriLength() = length <= MAX_URI_LENGTH

    /**
     * Returns whether or not the clipboard data mime type is plain text or HTML.
     */
    fun containsText() = mimeTypeIsHtmlText() || mimeTypeIsPlainText()

    /**
     * Returns whether or not the clipboard data is a URL.
     */
    @Suppress("MagicNumber")
    internal fun containsURL(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val description = clipboard.primaryClipDescription
            // An IllegalStateException is thrown if the url is too long.
            val score =
                try {
                    description?.getConfidenceScore(TextClassifier.TYPE_URL) ?: 0F
                } catch (e: IllegalStateException) {
                    0F
                }
            score >= 0.7F
        } else {
            !extractURL().isNullOrEmpty()
        }
    }

    private fun isSupportedMimeType() =
        mimeTypeIsPlainText() || mimeTypeIsHtmlText() || mimeTypeIsUrlText()

    private fun mimeTypeIsPlainText() = primaryClipIsMimeType(MIME_TYPE_TEXT_PLAIN)

    private fun mimeTypeIsHtmlText() = primaryClipIsMimeType(MIME_TYPE_TEXT_HTML)

    private fun mimeTypeIsUrlText() = primaryClipIsMimeType(MIME_TYPE_TEXT_URL)

    private fun primaryClipTextIsUrl() =
        mimeTypeIsUrlText() || firstPrimaryClipItem()?.let { isWebUrlFunc(it.text.toString()) } ?: false

    private fun primaryClipIsMimeType(mimeType: String) =
        clipboard.primaryClipDescription?.hasMimeType(mimeType) ?: false

    /**
     * Returns a [ClipData.Item] from the Android clipboard.
     * @return a string representation of the first item on the clipboard, if
     * the clipboard currently has an item or null if it does not.
     *
     * Note: this can throw a [android.os.DeadSystemException] if the clipboard content is too large,
     * or various exceptions for certain vendors, due to modifications made to the Android clipboard code.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun firstPrimaryClipItem(): ClipData.Item? = try {
        clipboard.primaryClip?.getItemAt(0)
    } catch (exception: Exception) {
        logger.error("Fetching clipboard content failed with: $exception")
        null
    }

    private fun firstSafePrimaryClipItemUrl() =
        firstPrimaryClipItem()?.let { safeUrl.stripUnsafeUrlSchemes(context, it.text) }
}
