/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.mozilla.fenix.helpers.HomeActivityComposeTestRule

class FakeSpotRobot() {
    @OptIn(ExperimentalTestApi::class)
    fun verifyReviewCheckerOptInScreen(composeTestRule: ComposeTestRule) {
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Review Checker, Beta, Heading"))
        composeTestRule.onNodeWithContentDescription("Review Checker, Beta, Heading")
            .assertExists()
        composeTestRule.onNodeWithText("Try our trusted guide to product reviews")
            .assertExists()
    }

    fun clickTryItButton(composeTestRule: ComposeTestRule) {
        composeTestRule.onNodeWithText("Yes, try it")
            .assertExists()
            .performClick()
    }

    class Transition
}
