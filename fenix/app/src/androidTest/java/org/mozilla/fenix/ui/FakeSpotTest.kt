/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.ui.robots.navigationToolbar

class FakeSpotTest {
    @get:Rule
    val composeTestRule = AndroidComposeTestRule(HomeActivityTestRule()) { it.activity }

    private val products = listOf(
        "https://www.walmart.com/ip/Beats-Fit-Pro-Noise-Cancelling-Wireless-Earbuds-Apple-Android-Compatible-Beats-Black/232379973",
        "https://www.bestbuy.com/site/apple-watch-series-9-gps-45mm-midnight-aluminum-case-with-midnight-sport-band-m-l-midnight/6340284.p?skuId=6340284"
    )

    @Test
    fun verifyFakeSpotOptInScreenOnWalmart() {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(products[0].toUri()) {
            waitForPageToLoad()
        }.clickReviewCheckerButton {
            verifyReviewCheckerOptInScreen(composeTestRule)
            clickTryItButton(composeTestRule)
        }
    }

    @Test
    fun verifyFakeSpotOptInScreenOnBestBuy() {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(products[1].toUri()) {
            waitForPageToLoad()
        }.clickReviewCheckerButton {
            verifyReviewCheckerOptInScreen(composeTestRule)
            clickTryItButton(composeTestRule)
        }
    }
}
