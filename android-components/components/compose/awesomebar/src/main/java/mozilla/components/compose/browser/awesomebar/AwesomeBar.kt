/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.browser.awesomebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import mozilla.components.compose.browser.awesomebar.internal.SuggestionFetcher
import mozilla.components.compose.browser.awesomebar.internal.Suggestions
import mozilla.components.concept.awesomebar.AwesomeBar

/**
 * An awesome bar displaying suggestions from the list of provided [AwesomeBar.SuggestionProvider]s.
 */
@Composable
fun AwesomeBar(
    text: String,
    colors: AwesomeBarColors = AwesomeBarDefaults.colors(),
    providers: List<AwesomeBar.SuggestionProvider>,
    onSuggestionClicked: (AwesomeBar.Suggestion) -> Unit,
    onAutoComplete: (AwesomeBar.Suggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        val fetcher = remember(providers) { SuggestionFetcher(providers) }

        LaunchedEffect(text) {
            fetcher.fetch(text)
        }

        Suggestions(
            fetcher.state.value,
            colors,
            onSuggestionClicked,
            onAutoComplete
        )
    }
}
