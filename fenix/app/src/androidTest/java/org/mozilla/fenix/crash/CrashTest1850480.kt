/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.crash

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.robots.homeScreen

class CrashTest1850480 {

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    @Test
    fun crashTest1850480() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAutofillSubMenu {
        }.goBack {
        }.goBack {
        }.openThreeDotMenu {
            verifySettingsButton()
        }

        assertEquals(
            Lifecycle.State.RESUMED,
            activityTestRule.activity.lifecycle.currentState,
        )
    }
}
