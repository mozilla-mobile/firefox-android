/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.domains

import android.content.Context
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.Locale

typealias ResultCallback = (String, String, String, Int) -> Unit

/**
 * Provides autocomplete functionality for domains, based on a provided list
 * of assets (see @{link Domains}) and/or a custom domain list managed
 * by {@link CustomDomains}.
 */
class DomainAutoCompleteProvider {
    object AutocompleteSource {
        const val DEFAULT_LIST = "default"
        const val CUSTOM_LIST = "custom"
    }

    internal data class Domain(val protocol: String, val hasWww: Boolean, val host: String) {
        internal val url: String
            get() = "$protocol${if (hasWww) "www." else "" }$host"

        companion object {
            private const val PROTOCOL_INDEX = 1
            private const val WWW_INDEX = 2
            private const val HOST_INDEX = 3

            private const val DEFAULT_PROTOCOL = "http://"

            private val urlMatcher = Regex("""(https?://)?(www.)?(.+)?""")

            internal fun create(url: String): Domain {
                val result = urlMatcher.find(url)

                return result?.let {
                    val protocol = it.groups[PROTOCOL_INDEX]?.value ?: DEFAULT_PROTOCOL
                    val hasWww = it.groups[WWW_INDEX]?.value == "www."
                    val host = it.groups[HOST_INDEX]?.value ?: throw IllegalStateException()

                    return Domain(protocol, hasWww, host)
                } ?: throw IllegalStateException()
            }
        }
    }

    private var customDomains = emptyList<Domain>()
    private var shippedDomains = emptyList<Domain>()
    private var useCustomDomains = false
    private var useShippedDomains = true

    /**
     * Computes an autocomplete suggestion for the given text, and invokes the
     * provided callback, passing the result.
     *
     * @param rawText text to be auto completed
     * @param resultCallback callback to be invoked with the autocomplete
     * result. The callback is passed the auto completed text, the source
     * identifier ({@link AutocompleteSource}) and the total number of
     * possible results.
     */
    fun autocomplete(rawText: String, resultCallback: ResultCallback) {
        // Search terms are all lowercase already, we just need to lowercase the search text
        val searchText = rawText.toLowerCase(Locale.US)

        if (useCustomDomains) {
            val result = tryToAutocomplete(searchText, customDomains)
            if (result != null) {
                val (autocomplete, domain) = result
                val resultText = getResultText(rawText, autocomplete)
                resultCallback(resultText, domain.url, AutocompleteSource.CUSTOM_LIST, customDomains.size)
                return
            }
        }

        if (useShippedDomains) {
            val result = tryToAutocomplete(searchText, shippedDomains)
            if (result != null) {
                val (autocomplete, domain) = result
                val resultText = getResultText(rawText, autocomplete)
                resultCallback(resultText, domain.url, AutocompleteSource.DEFAULT_LIST, shippedDomains.size)
                return
            }
        }

        resultCallback("", "", "", 0)
    }

    /**
     * Initializes this provider instance by making sure the shipped and/or custom
     * domains are loaded.
     *
     * @param context the application context
     * @param useShippedDomains true (default) if the domains provided by this
     * module should be used, otherwise false.
     * @param useCustomDomains true if the custom domains provided by
     * {@see CustomDomains} should be used, otherwise false (default).
     * @param loadDomainsFromDisk true (default) if domains should be loaded,
     * otherwise false. This parameter is for testing purposes only.
     */
    fun initialize(
        context: Context,
        useShippedDomains: Boolean = true,
        useCustomDomains: Boolean = false,
        loadDomainsFromDisk: Boolean = true
    ) {
        this.useCustomDomains = useCustomDomains
        this.useShippedDomains = useShippedDomains

        if (loadDomainsFromDisk) {
            launch(UI) {
                val domains = async(CommonPool) { Domains.load(context) }
                val customDomains = async(CommonPool) { CustomDomains.load(context) }

                onDomainsLoaded(domains.await(), customDomains.await())
            }
        }
    }

    internal fun onDomainsLoaded(domains: List<String>, customDomains: List<String>) {
        this.shippedDomains = domains.map { Domain.create(it) }
        this.customDomains = customDomains.map { Domain.create(it) }
    }

    @Suppress("ReturnCount")
    private fun tryToAutocomplete(searchText: String, domains: List<Domain>): Pair<String, Domain>? {
        domains.forEach {
            val wwwDomain = "www.${it.host}"
            if (wwwDomain.startsWith(searchText)) {
                return Pair(wwwDomain, it)
            }

            if (it.host.startsWith(searchText)) {
                return Pair(it.host, it)
            }
        }

        return null
    }

    /**
     * Our autocomplete list is all lower case, however the search text might
     * be mixed case. Our autocomplete EditText code does more string comparison,
     * which fails if the suggestion doesn't exactly match searchText (ie.
     * if casing differs). It's simplest to just build a suggestion
     * that exactly matches the search text - which is what this method is for:
     */
    private fun getResultText(rawSearchText: String, autocomplete: String) =
            rawSearchText + autocomplete.substring(rawSearchText.length)
}
