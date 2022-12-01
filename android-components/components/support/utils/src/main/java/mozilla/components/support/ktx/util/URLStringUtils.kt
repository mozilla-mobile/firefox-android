/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.util

import android.net.Uri
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.core.text.TextDirectionHeuristicCompat
import androidx.core.text.TextDirectionHeuristicsCompat
import java.util.regex.Pattern
import kotlin.text.RegexOption.IGNORE_CASE

object URLStringUtils {
    /**
     * Decodes a URL string into a human readable form.
     *
     * This is based on a port of losslessDecodeURI, except that
     * non-ASCII characters are not skipped from decoding.
     */
    fun decodeUrlForDisplay(uriString: String): String {
        var value = uriString
        // Try to decode as UTF-8 if there's no encoding sequence that we would break.
        if (!("%25(?:3B|2F|3F|3A|40|26|3D|2B|24|2C|23)".toRegex(IGNORE_CASE).matches(value))) {
            // This is different from losslessDecodeURI as we do want
            // to decode non-ASCII characters.
            try {
                value = Uri.decode(value)
                    // Uri.decode decodes %25 to %, which creates unintended
                    // encoding sequences. Re-encode it, unless it's part of a
                    // sequence that survived Uri.decode, i.e. one for:
                    // ';', '/', '?', ':', '@', '&', '=', '+', '$', ',', '#'
                    // (RFC 3987 section 3.2)
                    .replace("%(?!3B|2F|3F|3A|40|26|3D|2B|24|2C|23)".toRegex(IGNORE_CASE), {
                        Uri.encode(it.value)
                    })
            } catch (ex: Exception) {}
        }
        // Encode potentially invisible characters:
        //   U+0000-001F: C0/C1 control characters
        //   U+007F-009F: commands
        //   U+00A0, U+1680, U+2000-200A, U+202F, U+205F, U+3000: other spaces
        //   U+2028-2029: line and paragraph separators
        //   U+2800: braille empty pattern
        //   U+FFFC: object replacement character
        // Encode any trailing whitespace that may be part of a pasted URL, so that it
        // doesn't get eaten away by the location bar (bug 410726).
        // Encode all adjacent space chars (U+0020), to prevent spoofing attempts
        // where they would push part of the URL to overflow the location bar
        // (bug 1395508). A single space, or the last space if the are many, is
        // preserved to maintain readability of certain urls. We only do this for the
        // common space, because others may be eaten when copied to the clipboard, so
        // it's safer to preserve them encoded.
        value = value.replace("[\u0000-\u001f\u007f-\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u2800\u3000\ufffc]|[\r\n\t]|\u0020(?=\u0020)|\\s$".toRegex()) {
            Uri.encode(it.value)
        }

        // Encode characters that are ignorable, can't be rendered usefully, or may
        // confuse users.
        //
        // Default ignorable characters; ZWNJ (U+200C) and ZWJ (U+200D) are excluded
        // per bug 582186:
        //   U+00AD, U+034F, U+06DD, U+070F, U+115F-1160, U+17B4, U+17B5, U+180B-180E,
        //   U+2060, U+FEFF, U+200B, U+2060-206F, U+3164, U+FE00-FE0F, U+FFA0,
        //   U+FFF0-FFFB, U+1D173-1D17A (U+D834 + DD73-DD7A),
        //   U+E0000-E0FFF (U+DB40-DB43 + U+DC00-DFFF)
        // Bidi control characters (RFC 3987 sections 3.2 and 4.1 paragraph 6):
        //   U+061C, U+200E, U+200F, U+202A-202E, U+2066-2069
        // Other format characters in the Cf category that are unlikely to be rendered
        // usefully:
        //   U+0600-0605, U+08E2, U+110BD (U+D804 + U+DCBD),
        //   U+110CD (U+D804 + U+DCCD), U+13430-13438 (U+D80D + U+DC30-DC38),
        //   U+1BCA0-1BCA3 (U+D82F + U+DCA0-DCA3)
        // Mimicking UI parts:
        //   U+1F50F-1F513 (U+D83D + U+DD0F-DD13), U+1F6E1 (U+D83D + U+DEE1)
        value = value.replace(
            "[\u00ad\u034f\u061c\u06dd\u070f\u115f\u1160\u17b4\u17b5\u180b-\u180e\u200b\u200e\u200f\u202a-\u202e\u2060-\u206f\u3164\u0600-\u0605\u08e2\ufe00-\ufe0f\ufeff\uffa0\ufff0-\ufffb]|\ud804[\udcbd\udccd]|\ud80d[\udc30-\udc38]|\ud82f[\udca0-\udca3]|\ud834[\udd73-\udd7a]|[\udb40-\udb43][\udc00-\udfff]|\ud83d[\udd0f-\udd13\udee1]".toRegex(),
        ) {
            Uri.encode(it.value)
        }
        return value
    }

    /**
     * Determine whether a string is a URL.
     *
     * This method performs a lenient check to determine whether a string is a URL. Anything that
     * contains a :, ://, or . and has no internal spaces is potentially a URL. If you need a
     * stricter check, consider using isURLLikeStrict().
     */
    fun isURLLike(string: String) = isURLLenient.matcher(string).matches()

    /**
     * Determine whether a string is a search term.
     *
     * This method recognizes a string as a search term as anything other than a URL.
     */
    fun isSearchTerm(string: String) = !isURLLike(string)

    /**
     * Normalizes a URL String.
     */
    fun toNormalizedURL(string: String): String {
        val trimmedInput = string.trim()
        var uri = Uri.parse(trimmedInput)
        if (TextUtils.isEmpty(uri.scheme)) {
            uri = Uri.parse("http://$trimmedInput")
        } else {
            uri = uri.normalizeScheme()
        }
        return uri.toString()
    }

    private val isURLLenient by lazy {
        // Be lenient about what is classified as potentially a URL.
        // (\w+-+)*\w+(://[/]*|:|\.)(\w+-+)*\w+([\S&&[^\w-]]\S*)?
        // -------                 -------
        // 0 or more pairs of consecutive word letters or dashes
        //        ---                     ---
        // followed by at least a single word letter.
        // -----------             ----------
        // Combined, that means "w", "w-w", "w-w-w", etc match, but "w-", "w-w-", "w-w-w-" do not.
        //          --------------
        // That surrounds :, :// or .
        //                                                    -
        // At the end, there may be an optional
        //                                    ------------
        // non-word, non-- but still non-space character (e.g., ':', '/', '.', '?' but not 'a', '-', '\t')
        //                                                ---
        // and 0 or more non-space characters.
        //
        // These are some (odd) examples of valid urls according to this pattern:
        // c-c.com
        // c-c-c-c.c-c-c
        // c-http://c.com
        // about-mozilla:mozilla
        // c-http.d-x
        // www.c-
        // 3-3.3
        // www.c-c.-
        //
        // There are some examples of non-URLs according to this pattern:
        // -://x.com
        // -x.com
        // http://www-.com
        // www.c-c-
        // 3-3
        Pattern.compile(
            "^\\s*(\\w+-+)*\\w+(://[/]*|:|\\.)(\\w+-+)*\\w+([\\S&&[^\\w-]]\\S*)?\\s*$",
            flags,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal const val UNICODE_CHARACTER_CLASS: Int = 0x100

    // To run tests on a non-Android device (like a computer), Pattern.compile
    // requires a flag to enable unicode support. Set a value like flags here with a local
    // copy of UNICODE_CHARACTER_CLASS. Use a local copy because that constant is not
    // available on Android platforms < 24 (Fenix targets 21). At runtime this is not an issue
    // because, again, Android REs are always unicode compliant.
    // NB: The value has to go through an intermediate variable; otherwise, the linter will
    // complain that this value is not one of the predefined enums that are allowed.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var flags = 0

    private const val HTTP = "http://"
    private const val HTTPS = "https://"
    private const val WWW = "www."

    /**
     * Generates a shorter version of the provided URL for display purposes by stripping it of
     * https/http and/or WWW prefixes and/or trailing slash when applicable.
     *
     * The returned text will always be displayed from left to right.
     * If the directionality would otherwise be RTL "\u200E" will be prepended to the result to force LTR.
     */
    fun toDisplayUrl(
        originalUrl: CharSequence,
        textDirectionHeuristic: TextDirectionHeuristicCompat = TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR,
    ): CharSequence {
        val strippedText = maybeStripTrailingSlash(maybeStripUrlProtocol(originalUrl))

        return if (
            strippedText.isNotBlank() &&
            textDirectionHeuristic.isRtl(strippedText, 0, 1)
        ) {
            "\u200E" + strippedText
        } else {
            strippedText
        }
    }

    private fun maybeStripUrlProtocol(url: CharSequence): CharSequence {
        var noPrefixUrl = url
        if (url.toString().startsWith(HTTPS)) {
            noPrefixUrl = maybeStripUrlSubDomain(url.toString().replaceFirst(HTTPS, ""))
        } else if (url.toString().startsWith(HTTP)) {
            noPrefixUrl = maybeStripUrlSubDomain(url.toString().replaceFirst(HTTP, ""))
        }
        return noPrefixUrl
    }

    private fun maybeStripUrlSubDomain(url: CharSequence): CharSequence {
        return if (url.toString().startsWith(WWW)) {
            url.toString().replaceFirst(WWW, "")
        } else {
            url
        }
    }

    private fun maybeStripTrailingSlash(url: CharSequence): CharSequence {
        return url.trimEnd('/')
    }
}
