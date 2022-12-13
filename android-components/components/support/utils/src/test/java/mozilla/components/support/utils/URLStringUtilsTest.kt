/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils

import androidx.core.text.TextDirectionHeuristicCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.support.ktx.util.URLStringUtils
import mozilla.components.support.ktx.util.URLStringUtils.isSearchTerm
import mozilla.components.support.ktx.util.URLStringUtils.isURLLike
import mozilla.components.support.ktx.util.URLStringUtils.toNormalizedURL
import mozilla.components.support.ktx.util.URLStringUtils.urlHasPublicSuffix
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import kotlin.random.Random

private const val HTTP = "http://"
private const val HTTPS = "https://"
private const val WWW = "www."

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class URLStringUtilsTest {

    @Before
    fun configurePatternFlags() {
        URLStringUtils.flags = URLStringUtils.UNICODE_CHARACTER_CLASS
    }

    @Test
    fun toNormalizedURL() {
        val expectedUrl = "http://mozilla.org"
        assertEquals(expectedUrl, toNormalizedURL("http://mozilla.org"))
        assertEquals(expectedUrl, toNormalizedURL("  http://mozilla.org  "))
        assertEquals(expectedUrl, toNormalizedURL("mozilla.org"))
        assertEquals(expectedUrl, toNormalizedURL("HTTP://mozilla.org"))
    }

    @Test
    fun isUrlLike() {
        assertFalse(isURLLike("inurl:mozilla.org advanced search"))
        assertFalse(isURLLike("sf: help"))
        assertFalse(isURLLike("mozilla./~"))
        assertFalse(isURLLike("cnn.com politics"))

        assertTrue(isURLLike("about:config"))
        assertTrue(isURLLike("about:config:8000"))
        assertTrue(isURLLike("file:///home/user/myfile.html"))
        assertTrue(isURLLike("file://////////////home//user/myfile.html"))
        assertTrue(isURLLike("file://C:\\Users\\user\\myfile.html"))
        assertTrue(isURLLike("http://192.168.255.255"))
        assertTrue(isURLLike("link.unknown"))
        // Per https://bugs.chromium.org/p/chromium/issues/detail?id=31405, ICANN will accept
        // purely numeric gTLDs.
        assertTrue(isURLLike("3.14.2019"))
        assertTrue(isURLLike("3-four.14.2019"))
        assertTrue(isURLLike(" cnn.com "))
        assertTrue(isURLLike(" cnn.com"))
        assertTrue(isURLLike("cnn.com "))
        assertTrue(isURLLike("mozilla.com/~userdir"))
        assertTrue(isURLLike("my-domain.com"))
        assertTrue(isURLLike("http://faß.de//"))
        assertTrue(isURLLike("cnn.cơḿ"))
        assertTrue(isURLLike("cnn.çơḿ"))

        // Examples from the code comments:
        assertTrue(isURLLike("c-c.com"))
        assertTrue(isURLLike("c-c-c-c.c-c-c"))
        assertTrue(isURLLike("c-http://c.com"))
        assertTrue(isURLLike("about-mozilla:mozilla"))
        assertTrue(isURLLike("c-http.d-x"))
        assertTrue(isURLLike("www.c.-"))
        assertTrue(isURLLike("3-3.3"))
        assertTrue(isURLLike("www.c-c.-"))

        assertFalse(isURLLike(" -://x.com "))
        assertFalse(isURLLike("  -x.com"))
        assertFalse(isURLLike("http://www-.com"))
        assertFalse(isURLLike("www.c-c-  "))
        assertFalse(isURLLike("3-3 "))

        // Examples from issues
        assertTrue(isURLLike("https://abc--cba.com/")) // #7096
    }

    @Test
    fun isSearchTerm() {
        assertTrue(isSearchTerm("inurl:mozilla.org advanced search"))
        assertTrue(isSearchTerm("sf: help"))
        assertTrue(isSearchTerm("mozilla./~"))
        assertTrue(isSearchTerm("cnn.com politics"))

        assertFalse(isSearchTerm("about:config"))
        assertFalse(isSearchTerm("about:config:8000"))
        assertFalse(isSearchTerm("file:///home/user/myfile.html"))
        assertFalse(isSearchTerm("file://////////////home//user/myfile.html"))
        assertFalse(isSearchTerm("file://C:\\Users\\user\\myfile.html"))
        assertFalse(isSearchTerm("http://192.168.255.255"))
        assertFalse(isSearchTerm("link.unknown"))
        // Per https://bugs.chromium.org/p/chromium/issues/detail?id=31405, ICANN will accept
        // purely numeric gTLDs.
        assertFalse(isSearchTerm("3.14.2019"))
        assertFalse(isSearchTerm("3-four.14.2019"))
        assertFalse(isSearchTerm(" cnn.com "))
        assertFalse(isSearchTerm(" cnn.com"))
        assertFalse(isSearchTerm("cnn.com "))
        assertFalse(isSearchTerm("my-domain.com"))
        assertFalse(isSearchTerm("camp-firefox.de"))
        assertFalse(isSearchTerm("http://my-domain.com"))
        assertFalse(isSearchTerm("mozilla.com/~userdir"))
        assertFalse(isSearchTerm("http://faß.de//"))
        assertFalse(isSearchTerm("cnn.cơḿ"))
        assertFalse(isSearchTerm("cnn.çơḿ"))

        // Examples from the code comments:
        assertFalse(isSearchTerm("c-c.com"))
        assertFalse(isSearchTerm("c-c-c-c.c-c-c"))
        assertFalse(isSearchTerm("c-http://c.com"))
        assertFalse(isSearchTerm("about-mozilla:mozilla"))
        assertFalse(isSearchTerm("c-http.d-x"))
        assertFalse(isSearchTerm("www.c.-"))
        assertFalse(isSearchTerm("3-3.3"))
        assertFalse(isSearchTerm("www.c-c.-"))

        assertTrue(isSearchTerm(" -://x.com "))
        assertTrue(isSearchTerm("  -x.com"))
        assertTrue(isSearchTerm("http://www-.com"))
        assertTrue(isSearchTerm("www.c-c-  "))
        assertTrue(isSearchTerm("3-3 "))

        // Examples from issues
        assertFalse(isSearchTerm("https://abc--cba.com/")) // #7096
    }

    @Test
    fun stripUrlSchemeUrlWithHttps() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("https://mozilla.com")
        assertEquals("mozilla.com", testDisplayUrl)
    }

    @Test
    fun stripTrailingSlash() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("mozilla.com/")
        assertEquals("mozilla.com", testDisplayUrl)
    }

    @Test
    fun stripUrlSchemeUrlWithHttpsAndTrailingSlash() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("https://mozilla.com/")
        assertEquals("mozilla.com", testDisplayUrl)
    }

    @Test
    fun stripUrlSchemeUrlWithHttp() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("http://mozilla.com")
        assertEquals("mozilla.com", testDisplayUrl)
    }

    @Test
    fun stripUrlSubdomainUrlWithHttps() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("https://www.mozilla.com")
        assertEquals("mozilla.com", testDisplayUrl)
    }

    @Test
    fun stripUrlSubdomainUrlWithHttp() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("http://www.mozilla.com")
        assertEquals("mozilla.com", testDisplayUrl)
    }

    @Test
    fun stripUrlSchemeAndSubdomainUrlNoMatch() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("zzz://www.mozillahttp://.com")
        assertEquals("zzz://www.mozillahttp://.com", testDisplayUrl)
    }

    @Test
    fun showDisplayUrlAsLTREvenIfTextStartsWithArabicCharacters() {
        val testDisplayUrl = URLStringUtils.toDisplayUrl("http://ختار.ار/www.mozilla.org/1")
        assertEquals("\u200Eختار.ار/www.mozilla.org/1", testDisplayUrl)
    }

    @Test
    fun toDisplayUrlAlwaysUseATextDirectionHeuristicToDetermineDirectionality() {
        val textHeuristic = spy(TestTextDirectionHeuristicCompat())

        URLStringUtils.toDisplayUrl("http://ختار.ار/www.mozilla.org/1", textHeuristic)
        verify(textHeuristic).isRtl("ختار.ار/www.mozilla.org/1", 0, 1)

        URLStringUtils.toDisplayUrl("http://www.mozilla.org/1", textHeuristic)
        verify(textHeuristic).isRtl("mozilla.org/1", 0, 1)
    }

    @Test
    fun toDisplayUrlHandlesBlankStrings() {
        assertEquals("", URLStringUtils.toDisplayUrl(""))

        assertEquals("  ", URLStringUtils.toDisplayUrl("  "))
    }

    @Test
    fun toDisplayUrlStripsTrailingSuffixData() {
        val url = "mozilla.org/en-GB/firefox/browsers"

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(
                originalUrl = "$HTTP$WWW$url/",
                stripTrailingData = true,
            ),
        )

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(
                originalUrl = "$HTTPS$WWW$url/",
                stripTrailingData = true,
            ),
        )

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(
                originalUrl = "$HTTP$WWW$url",
                stripTrailingData = true,
            ),
        )

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(
                originalUrl = "$HTTPS$WWW$url",
                stripTrailingData = true,
            ),
        )

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(originalUrl = "$HTTP$url", stripTrailingData = true),
        )

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(originalUrl = "$HTTPS$url", stripTrailingData = true),
        )

        assertEquals(
            "mozilla.org",
            URLStringUtils.toDisplayUrl(originalUrl = url, stripTrailingData = true),
        )
    }

    @Test
    fun urlHasPublicSuffix() = runTest {
        val publicSuffixList = PublicSuffixList(testContext)

        testURLSFromIsLikeAndIsSearchTermTests(publicSuffixList)

        // Invalid URL structures. These examples are not exhaustive, see [Patterns.Pattern.WEB_URL] for full details.
        assertFalse("org.mozilla.www".urlHasPublicSuffix(publicSuffixList))
        assertFalse("mozilla.https".urlHasPublicSuffix(publicSuffixList))
        assertFalse("www.mozilla.https".urlHasPublicSuffix(publicSuffixList))
        assertFalse("www.mozilla/firefox.org".urlHasPublicSuffix(publicSuffixList))

        // Valid URL structure
        // Known/valid suffixes
        assertTrue("org".asSuffixForUrl().urlHasPublicSuffix(publicSuffixList))
        assertTrue("com".asSuffixForUrl().urlHasPublicSuffix(publicSuffixList))
        assertTrue("gov".asSuffixForUrl().urlHasPublicSuffix(publicSuffixList))

        // Unknown/invalid suffixes
        assertFalse("gro".asSuffixForUrl().urlHasPublicSuffix(publicSuffixList))
        assertFalse("moc".asSuffixForUrl().urlHasPublicSuffix(publicSuffixList))
        assertFalse("vog".asSuffixForUrl().urlHasPublicSuffix(publicSuffixList))
    }

    /**
     * Test [urlHasPublicSuffix] with the URLs used in tests for [isSearchTerm] and [isSearchTerm].
     */
    private suspend fun testURLSFromIsLikeAndIsSearchTermTests(publicSuffixList: PublicSuffixList) {
        assertFalse("inurl:mozilla.org advanced search".urlHasPublicSuffix(publicSuffixList))
        assertFalse("sf: help".urlHasPublicSuffix(publicSuffixList))
        assertFalse("mozilla./~".urlHasPublicSuffix(publicSuffixList))
        assertFalse("cnn.com politics".urlHasPublicSuffix(publicSuffixList))

        assertFalse("about:config".urlHasPublicSuffix(publicSuffixList))
        assertFalse("about:config:8000".urlHasPublicSuffix(publicSuffixList))
        assertFalse("file:///home/user/myfile.html".urlHasPublicSuffix(publicSuffixList))
        assertFalse("file://////////////home//user/myfile.html".urlHasPublicSuffix(publicSuffixList))
        assertFalse("file://C:\\Users\\user\\myfile.html".urlHasPublicSuffix(publicSuffixList))
        assertFalse("http://192.168.255.255".urlHasPublicSuffix(publicSuffixList))
        assertFalse("link.unknown".urlHasPublicSuffix(publicSuffixList))
        assertFalse("3.14.2019".urlHasPublicSuffix(publicSuffixList))
        assertFalse("3-four.14.2019".urlHasPublicSuffix(publicSuffixList))

        assertTrue(" cnn.com ".urlHasPublicSuffix(publicSuffixList))
        assertTrue(" cnn.com".urlHasPublicSuffix(publicSuffixList))
        assertTrue("cnn.com ".urlHasPublicSuffix(publicSuffixList))
        assertTrue("mozilla.com/~userdir".urlHasPublicSuffix(publicSuffixList))
        assertTrue("my-domain.com".urlHasPublicSuffix(publicSuffixList))
        assertTrue("http://faß.de//".urlHasPublicSuffix(publicSuffixList))

        assertFalse("cnn.cơḿ".urlHasPublicSuffix(publicSuffixList))
        assertFalse("cnn.çơḿ".urlHasPublicSuffix(publicSuffixList))

        // Examples from the code comments:
        assertTrue("c-c.com".urlHasPublicSuffix(publicSuffixList))
        assertFalse("c-c-c-c.c-c-c".urlHasPublicSuffix(publicSuffixList))
        assertFalse("c-http://c.com".urlHasPublicSuffix(publicSuffixList))
        assertFalse("about-mozilla:mozilla".urlHasPublicSuffix(publicSuffixList))
        assertFalse("c-http.d-x".urlHasPublicSuffix(publicSuffixList))
        assertFalse("www.c.-".urlHasPublicSuffix(publicSuffixList))
        assertFalse("3-3.3".urlHasPublicSuffix(publicSuffixList))
        assertFalse("www.c-c.-".urlHasPublicSuffix(publicSuffixList))

        assertFalse(" -://x.com ".urlHasPublicSuffix(publicSuffixList))
        assertFalse("  -x.com".urlHasPublicSuffix(publicSuffixList))
        assertFalse("http://www-.com".urlHasPublicSuffix(publicSuffixList))
        assertFalse("www.c-c-  ".urlHasPublicSuffix(publicSuffixList))
        assertFalse("3-3 ".urlHasPublicSuffix(publicSuffixList))

        // Examples from issues
        assertTrue("https://abc--cba.com/".urlHasPublicSuffix(publicSuffixList)) // #7096
    }

    /**
     * Gets a URL using the provided string as a suffix.
     */
    private fun String.asSuffixForUrl() =
        "https://www.mozilla.$this/test/data/not/included/in/check/"
}

/**
 * Custom [TextDirectionHeuristicCompat] used only in tests to make possible testing of RTL checks.
 * Overcomes the limitations not allowing Mockito to mock platform implementations.
 *
 * The return of both [isRtl] is non-deterministic. Setup a different behavior if needed.
 */
private open class TestTextDirectionHeuristicCompat : TextDirectionHeuristicCompat {
    override fun isRtl(array: CharArray?, start: Int, count: Int): Boolean {
        return Random.nextBoolean()
    }

    override fun isRtl(cs: CharSequence?, start: Int, count: Int): Boolean {
        return Random.nextBoolean()
    }
}
