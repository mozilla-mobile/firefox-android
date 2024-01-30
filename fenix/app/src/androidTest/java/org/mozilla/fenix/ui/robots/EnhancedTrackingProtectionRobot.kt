/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.MatcherHelper.assertUIObjectExists
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemWithText
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.helpers.isChecked

/**
 * Implementation of Robot Pattern for Enhanced Tracking Protection UI.
 */
class EnhancedTrackingProtectionRobot {
    fun verifyEnhancedTrackingProtectionSheetStatus(status: String, state: Boolean) =
        assertEnhancedTrackingProtectionSheetStatus(status, state)

    fun verifyETPSwitchVisibility(visible: Boolean) = assertETPSwitchVisibility(visible)

    fun verifyCrossSiteCookiesBlocked(isBlocked: Boolean) {
        assertUIObjectExists(itemWithResId("$packageName:id/cross_site_tracking"))
        crossSiteCookiesBlockListButton().click()
        Log.i(TAG, "verifyCrossSiteCookiesBlocked: Clicked cross site cookies block list button")
        // Verifies the trackers block/allow list
        onView(withId(R.id.details_blocking_header))
            .check(
                matches(
                    withText(
                        if (isBlocked) {
                            ("Blocked")
                        } else {
                            ("Allowed")
                        },
                    ),
                ),
            )
        Log.i(TAG, "verifyCrossSiteCookiesBlocked: Verified cross site cookies are blocked: $isBlocked")
    }

    fun verifySocialMediaTrackersBlocked(isBlocked: Boolean) {
        assertUIObjectExists(itemWithResId("$packageName:id/social_media_trackers"))
        socialTrackersBlockListButton().click()
        Log.i(TAG, "verifySocialMediaTrackersBlocked: Clicked social trackers block list button")
        // Verifies the trackers block/allow list
        onView(withId(R.id.details_blocking_header))
            .check(
                matches(
                    withText(
                        if (isBlocked) {
                            ("Blocked")
                        } else {
                            ("Allowed")
                        },
                    ),
                ),
            )
        Log.i(TAG, "verifySocialMediaTrackersBlocked: Verified social trackers are blocked: $isBlocked")
        onView(withId(R.id.blocking_text_list)).check(matches(isDisplayed()))
        Log.i(TAG, "verifySocialMediaTrackersBlocked: Verified blocked social trackers list is displayed")
    }

    fun verifyFingerprintersBlocked(isBlocked: Boolean) {
        assertUIObjectExists(itemWithResId("$packageName:id/fingerprinters"))
        fingerprintersBlockListButton().click()
        Log.i(TAG, "verifyFingerprintersBlocked: Clicked fingerprinters block list button")
        // Verifies the trackers block/allow list
        onView(withId(R.id.details_blocking_header))
            .check(
                matches(
                    withText(
                        if (isBlocked) {
                            ("Blocked")
                        } else {
                            ("Allowed")
                        },
                    ),
                ),
            )
        Log.i(TAG, "verifyFingerprintersBlocked: Verified fingerprinters are blocked: $isBlocked")
        onView(withId(R.id.blocking_text_list)).check(matches(isDisplayed()))
        Log.i(TAG, "verifyFingerprintersBlocked: Verified blocked fingerprinter trackers list is displayed")
    }

    fun verifyCryptominersBlocked(isBlocked: Boolean) {
        assertUIObjectExists(itemWithResId("$packageName:id/cryptominers"))
        cryptominersBlockListButton().click()
        Log.i(TAG, "verifyCryptominersBlocked: Clicked cryptominers block list button")
        // Verifies the trackers block/allow list
        onView(withId(R.id.details_blocking_header))
            .check(
                matches(
                    withText(
                        if (isBlocked) {
                            ("Blocked")
                        } else {
                            ("Allowed")
                        },
                    ),
                ),
            )
        Log.i(TAG, "verifyCryptominersBlocked: Verified cryptominers are blocked: $isBlocked")
        onView(withId(R.id.blocking_text_list)).check(matches(isDisplayed()))
        Log.i(TAG, "verifyCryptominersBlocked: Verified blocked cryptominers trackers list is displayed")
    }

    fun verifyTrackingContentBlocked(isBlocked: Boolean) {
        assertUIObjectExists(itemWithText("Tracking Content"))
        trackingContentBlockListButton().click()
        Log.i(TAG, "verifyTrackingContentBlocked: Clicked tracking content block list button")
        // Verifies the trackers block/allow list
        onView(withId(R.id.details_blocking_header))
            .check(
                matches(
                    withText(
                        if (isBlocked) {
                            ("Blocked")
                        } else {
                            ("Allowed")
                        },
                    ),
                ),
            )
        Log.i(TAG, "verifyTrackingContentBlocked: Verified tracking content is blocked: $isBlocked")
        onView(withId(R.id.blocking_text_list)).check(matches(isDisplayed()))
        Log.i(TAG, "verifyTrackingContentBlocked: Verified blocked tracking content trackers list is displayed")
    }

    fun viewTrackingContentBlockList() {
        onView(withId(R.id.blocking_text_list))
            .check(
                matches(
                    withText(
                        containsString(
                            "social-track-digest256.dummytracker.org\n" +
                                "ads-track-digest256.dummytracker.org\n" +
                                "analytics-track-digest256.dummytracker.org",
                        ),
                    ),
                ),
            )
        Log.i(TAG, "viewTrackingContentBlockList: Verified blocked tracking content trackers")
    }

    fun verifyETPSectionIsDisplayedInQuickSettingsSheet(isDisplayed: Boolean) =
        assertUIObjectExists(
            itemWithResId("$packageName:id/trackingProtectionLayout"),
            exists = isDisplayed,
        )

    fun navigateBackToDetails() {
        onView(withId(R.id.details_back)).click()
        Log.i(TAG, "navigateBackToDetails: Clicked details list back button")
    }

    class Transition {
        fun openEnhancedTrackingProtectionSheet(interact: EnhancedTrackingProtectionRobot.() -> Unit): Transition {
            Log.i(TAG, "openEnhancedTrackingProtectionSheet: Looking for site security button")
            pageSecurityIndicator().waitForExists(waitingTime)
            pageSecurityIndicator().click()
            Log.i(TAG, "openEnhancedTrackingProtectionSheet: Clicked site security button")
            assertSecuritySheetIsCompletelyDisplayed()

            EnhancedTrackingProtectionRobot().interact()
            return Transition()
        }

        fun closeEnhancedTrackingProtectionSheet(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            // Back out of the Enhanced Tracking Protection sheet
            mDevice.pressBack()
            Log.i(TAG, "closeEnhancedTrackingProtectionSheet: Clicked device back button")

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun toggleEnhancedTrackingProtectionFromSheet(interact: EnhancedTrackingProtectionRobot.() -> Unit): Transition {
            enhancedTrackingProtectionSwitch().click()
            Log.i(TAG, "toggleEnhancedTrackingProtectionFromSheet: Clicked ETP switch")

            EnhancedTrackingProtectionRobot().interact()
            return Transition()
        }

        fun openProtectionSettings(interact: SettingsSubMenuEnhancedTrackingProtectionRobot.() -> Unit): SettingsSubMenuEnhancedTrackingProtectionRobot.Transition {
            Log.i(TAG, "openProtectionSettings: Looking for ETP sheet \"Details\" button")
            openEnhancedTrackingProtectionDetails().waitForExists(waitingTime)
            openEnhancedTrackingProtectionDetails().click()
            Log.i(TAG, "openProtectionSettings: Clicked ETP sheet \"Details\" button")
            trackingProtectionSettingsButton().click()
            Log.i(TAG, "openProtectionSettings: Clicked \"Protection Settings\" button")

            SettingsSubMenuEnhancedTrackingProtectionRobot().interact()
            return SettingsSubMenuEnhancedTrackingProtectionRobot.Transition()
        }

        fun openDetails(interact: EnhancedTrackingProtectionRobot.() -> Unit): Transition {
            Log.i(TAG, "openDetails: Looking for ETP sheet \"Details\" button")
            openEnhancedTrackingProtectionDetails().waitForExists(waitingTime)
            openEnhancedTrackingProtectionDetails().click()
            Log.i(TAG, "openDetails: Clicked ETP sheet \"Details\" button")

            EnhancedTrackingProtectionRobot().interact()
            return Transition()
        }
    }
}

fun enhancedTrackingProtection(interact: EnhancedTrackingProtectionRobot.() -> Unit): EnhancedTrackingProtectionRobot.Transition {
    EnhancedTrackingProtectionRobot().interact()
    return EnhancedTrackingProtectionRobot.Transition()
}

private fun assertETPSwitchVisibility(visible: Boolean) {
    if (visible) {
        enhancedTrackingProtectionSwitch()
            .check(matches(isDisplayed()))
        Log.i(TAG, "assertETPSwitchVisibility: Verified ETP toggle is displayed")
    } else {
        enhancedTrackingProtectionSwitch()
            .check(matches(not(isDisplayed())))
        Log.i(TAG, "assertETPSwitchVisibility: Verified ETP toggle is not displayed")
    }
}

private fun assertEnhancedTrackingProtectionSheetStatus(status: String, state: Boolean) {
    mDevice.waitNotNull(Until.findObjects(By.text("Protections are $status for this site")))
    onView(ViewMatchers.withResourceName("switch_widget")).check(
        matches(
            isChecked(
                state,
            ),
        ),
    )
    Log.i(TAG, "assertEnhancedTrackingProtectionSheetStatus: Verified ETP toggle is checked: $state")
}

private fun pageSecurityIndicator() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_security_indicator"))

private fun enhancedTrackingProtectionSwitch() =
    onView(ViewMatchers.withResourceName("switch_widget"))

private fun trackingProtectionSettingsButton() =
    onView(withId(R.id.protection_settings)).inRoot(RootMatchers.isDialog()).check(
        matches(
            isDisplayed(),
        ),
    )

private fun openEnhancedTrackingProtectionDetails() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/trackingProtectionDetails"))

private fun trackingContentBlockListButton() =
    onView(
        allOf(
            withText("Tracking Content"),
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
        ),
    )

private fun socialTrackersBlockListButton() =
    onView(
        allOf(
            withId(R.id.social_media_trackers),
            withText("Social Media Trackers"),
        ),
    )

private fun crossSiteCookiesBlockListButton() =
    onView(
        allOf(
            withId(R.id.cross_site_tracking),
            withText("Cross-Site Cookies"),
        ),
    )

private fun cryptominersBlockListButton() =
    onView(
        allOf(
            withId(R.id.cryptominers),
            withText("Cryptominers"),
        ),
    )

private fun fingerprintersBlockListButton() =
    onView(
        allOf(
            withId(R.id.fingerprinters),
            withText("Fingerprinters"),
        ),
    )

private fun assertSecuritySheetIsCompletelyDisplayed() {
    Log.i(TAG, "assertSecuritySheetIsCompletelyDisplayed: Looking for quick actions sheet")
    mDevice.findObject(UiSelector().description(getStringResource(R.string.quick_settings_sheet)))
        .waitForExists(waitingTime)
    assertUIObjectExists(itemWithResId("$packageName:id/quick_action_sheet"))
}
