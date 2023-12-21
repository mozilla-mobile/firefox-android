/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.observeAsState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.components
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Adds tabs for debug purposes
 */
class DebugAddTabsSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FirefoxTheme {
                    DebugAddTabsScreen()
                }
            }
        }
    }

    @Composable
    private fun DebugAddTabsScreen() {
        val currentTabCount by components.core.store.observeAsState(
            initialValue = 0,
            map = { it.tabs.size },
        )

        Column(modifier = Modifier
            .padding(8.dp),
        ) {
            CurrentTabCount(currentTabCount)
            Spacer(modifier = Modifier.size(8.dp))
            AddTabsButton(store = components.core.store)
        }

    }
    
    @Composable
    private fun CurrentTabCount(count: Int) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.debug_add_tabs_count),
                color = FirefoxTheme.colors.textPrimary,
                style = FirefoxTheme.typography.headline7,
                modifier = Modifier.padding(4.dp)
            )

            Text(
                text = count.toString(),
                color = FirefoxTheme.colors.textPrimary,
                modifier = Modifier.padding(4.dp)
            )
        }
    }

    @Composable
    private fun AddTabsButton(store: BrowserStore) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            onClick = {
                store.dispatch(TabListAction.AddMultipleTabsAction(
                    tabs = listOf(
                        createTab("https://www.mozilla.org", private = false),
                        createTab("https://www.wikipedia.org", private = false),
                        createTab("https://www.amazon.com", private = false),
                        createTab("https://www.reddit.com", private = false),
                        createTab("https://androiddev.reddit.com", private = false),
                        createTab("https://www.google.com", private = false),
                        createTab("https://www.facebook.com", private = false),
                        createTab("https://www.whatsapp.com", private = false),
                        createTab("https://www.instagram.com", private = false),
                        createTab("https://www.linkedin.com", private = false),
                        createTab("https://www.tiktok.com", private = false),
                        createTab("https://www.twitter.com", private = false),
                        createTab("https://www.youtube.com", private = false),
                        createTab("https://www.twitch.tv", private = false),
                        createTab("https://www.ebay.com", private = false),
                        createTab("https://www.shopify.com", private = false),
                        createTab("https://www.square.com", private = false),
                        createTab("https://www.ign.com", private = false),
                        createTab("https://www.gamespot.com", private = false),
                        createTab("https://www.pcgamer.com", private = false),
                        createTab("https://www.cnet.com", private = false),
                        createTab("https://www.theverge.com", private = false),
                        createTab("https://www.androidcentral.com", private = false),
                        createTab("https://developers.googleblog.com", private = false),
                        createTab("https://developer.android.com", private = false),
                        createTab("https://www.oracle.com", private = false),
                        createTab("https://www.reddit.com/r/androiddev/comments/14skdld/threads_is_written_almost_completely_in_jetpack/", private = false),
                        createTab("https://www.apple.com", private = false),
                        createTab("https://www.google.com", private = false),
                        createTab("https://www.samsung.com", private = false),
                        createTab("https://www.microsoft.com", private = false),
                        createTab("https://www.expedia.com", private = false),
                        createTab("https://www.united.com", private = false),
                        createTab("https://www.southwest.com", private = false),
                        createTab("https://www.jetblue.com", private = false),
                        createTab("https://www.aa.com", private = false),
                        createTab("https://www.spirit.com", private = false),
                        createTab("https://www.kayak.com", private = false),
                        createTab("https://www.vrbo.com", private = false),
                        createTab("https://www.airbnb.com", private = false),
                        createTab("https://www.zillow.com", private = false),
                        createTab("https://www.redfin.com", private = false),
                        createTab("https://www.realtor.com", private = false),
                        createTab("https://www.apartments.com", private = false),
                        createTab("https://www.autotrader.com", private = false),
                        createTab("https://www.cars.com", private = false),
                        createTab("https://www.carvana.com", private = false),
                        createTab("https://www.carmax.com", private = false),
                        createTab("https://www.honda.com", private = false),
                        createTab("https://www.ford.com", private = false),
                        createTab("https://www.chevy.com", private = false),
                        createTab("https://www.bmw.com", private = false),
                        createTab("https://www.porsche.com", private = false),
                        createTab("https://www.tesla.com", private = false),
                        createTab("https://www.audi.com", private = false),
                        createTab("https://www.subaru.com", private = false),
                        createTab("https://www.adidas.com", private = false),
                        createTab("https://www.nike.com", private = false),
                        createTab("https://www.reebok.com", private = false),
                        createTab("https://www.dsw.com", private = false),
                        createTab("https://www.zappos.com", private = false),
                        createTab("https://www.famousfootwear.com", private = false),
                        createTab("https://www.nordstrom.com", private = false),
                        createTab("https://www.macys.com", private = false),
                        createTab("https://www.bestbuy.com", private = false),
                        createTab("https://www.razer.com", private = false),
                        createTab("https://www.netflix.com", private = false),
                        createTab("https://www.disneyplus.com", private = false),
                        createTab("https://www.hulu.com", private = false),
                        createTab("https://www.espn.com", private = false),
                        createTab("https://tv.apple.com", private = false),
                        createTab("https://www.paramountplus.com/", private = false),
                        createTab("https://www.marvel.com", private = false),
                        createTab("https://www.dc.com", private = false),
                        createTab("https://www.imagecomics.com", private = false),
                        createTab("https://www.asus.com", private = false),
                        createTab("https://www.verizon.com", private = false),
                        createTab("https://www.att.com", private = false),
                        createTab("https://www.t-mobile.com", private = false),
                        createTab("https://www.boostmobile.com", private = false),
                        createTab("https://fi.google.com", private = false),
                        createTab("https://www.ups.com", private = false),
                        createTab("https://www.dhl.com", private = false),
                        createTab("https://www.figma.com", private = false),
                        createTab("https://www.metmuseum.org", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170000", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170001", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170002", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170003", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170004", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170005", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170006", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170007", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170008", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170009", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170010", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170011", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170012", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170013", private = false),
                        createTab("https://bugzilla.mozilla.org/show_bug.cgi?id=170014", private = false),
                    )
                ))
            }
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = "Add 100 tabs",
                color = FirefoxTheme.colors.textOnColorPrimary
            )
        }
    }
}
