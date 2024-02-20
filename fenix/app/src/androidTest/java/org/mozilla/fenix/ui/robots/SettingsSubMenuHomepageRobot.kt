/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.isChecked

/**
 * Implementation of Robot Pattern for the settings Homepage sub menu.
 */
class SettingsSubMenuHomepageRobot {

    fun verifyHomePageView(
        shortcutsSwitchEnabled: Boolean = true,
        sponsoredShortcutsCheckBox: Boolean = true,
        jumpBackInSwitchEnabled: Boolean = true,
        recentBookmarksSwitchEnabled: Boolean = true,
        recentlyVisitedSwitchEnabled: Boolean = true,
        pocketSwitchEnabled: Boolean = true,
        sponsoredStoriesCheckBox: Boolean = true,
    ) {
        shortcutsButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (shortcutsSwitchEnabled) {
            shortcutsButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isChecked(),
                            ),
                        ),
                    ),
                )
        } else {
            shortcutsButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isNotChecked(),
                            ),
                        ),
                    ),
                )
        }

        sponsoredShortcutsButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (sponsoredShortcutsCheckBox) {
            sponsoredShortcutsButton()
                .check(
                    matches(
                        hasSibling(
                            ViewMatchers.withChild(
                                allOf(
                                    withClassName(CoreMatchers.endsWith("CheckBox")),
                                    isChecked(),
                                ),
                            ),
                        ),
                    ),
                )
        } else {
            sponsoredShortcutsButton()
                .check(
                    matches(
                        hasSibling(
                            ViewMatchers.withChild(
                                allOf(
                                    withClassName(CoreMatchers.endsWith("CheckBox")),
                                    isNotChecked(),
                                ),
                            ),
                        ),
                    ),
                )
        }

        jumpBackInButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (jumpBackInSwitchEnabled) {
            jumpBackInButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isChecked(),
                            ),
                        ),
                    ),
                )
        } else {
            jumpBackInButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isNotChecked(),
                            ),
                        ),
                    ),
                )
        }

        recentBookmarksButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (recentBookmarksSwitchEnabled) {
            recentBookmarksButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isChecked(),
                            ),
                        ),
                    ),
                )
        } else {
            recentBookmarksButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isNotChecked(),
                            ),
                        ),
                    ),
                )
        }

        recentlyVisitedButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (recentlyVisitedSwitchEnabled) {
            recentlyVisitedButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isChecked(),
                            ),
                        ),
                    ),
                )
        } else {
            recentlyVisitedButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isNotChecked(),
                            ),
                        ),
                    ),
                )
        }

        pocketButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (pocketSwitchEnabled) {
            pocketButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isChecked(),
                            ),
                        ),
                    ),
                )
        } else {
            pocketButton()
                .check(
                    matches(
                        TestHelper.hasCousin(
                            Matchers.allOf(
                                withClassName(Matchers.endsWith("Switch")),
                                isNotChecked(),
                            ),
                        ),
                    ),
                )
        }

        sponsoredStoriesButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        if (sponsoredStoriesCheckBox) {
            sponsoredStoriesButton()
                .check(
                    matches(
                        hasSibling(
                            ViewMatchers.withChild(
                                allOf(
                                    withClassName(CoreMatchers.endsWith("CheckBox")),
                                    isChecked(),
                                ),
                            ),
                        ),
                    ),
                )
        } else {
            sponsoredStoriesButton()
                .check(
                    matches(
                        hasSibling(
                            ViewMatchers.withChild(
                                allOf(
                                    withClassName(CoreMatchers.endsWith("CheckBox")),
                                    isNotChecked(),
                                ),
                            ),
                        ),
                    ),
                )
        }

        openingScreenHeading().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        homepageButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        lastTabButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        homepageAfterFourHoursButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        Log.i(Constants.TAG, "verifyHomePageView: Verified the home page elements")
    }

    fun verifySelectedOpeningScreenOption(openingScreenOption: String) =
        onView(
            allOf(
                withId(R.id.radio_button),
                hasSibling(withText(openingScreenOption)),
            ),
        ).check(matches(isChecked(true)))

    fun clickShortcutsButton() = shortcutsButton().click()

    fun clickSponsoredShortcuts() = sponsoredShortcutsButton().click()

    fun clickJumpBackInButton() = jumpBackInButton().click()

    fun clickRecentlyVisited() = recentlyVisitedButton().click()

    fun clickRecentBookmarksButton() = recentBookmarksButton().click()

    fun clickRecentSearchesButton() = recentlyVisitedButton().click()

    fun clickPocketButton() = pocketButton().click()

    fun clickOpeningScreenOption(openingScreenOption: String) {
        when (openingScreenOption) {
            "Homepage" -> homepageButton().click()
            "Last tab" -> lastTabButton().click()
            "Homepage after four hours of inactivity" -> homepageAfterFourHoursButton().click()
        }
    }

    fun openWallpapersMenu() = wallpapersMenuButton().click()

    fun selectWallpaper(wallpaperName: String) =
        mDevice.findObject(UiSelector().description(wallpaperName)).click()

    fun verifySponsoredShortcutsCheckBox(checked: Boolean) {
        if (checked) {
            sponsoredShortcutsButton()
                .check(
                    matches(
                        hasSibling(
                            ViewMatchers.withChild(
                                allOf(
                                    withClassName(CoreMatchers.endsWith("CheckBox")),
                                    isChecked(),
                                ),
                            ),
                        ),
                    ),
                )
        } else {
            sponsoredShortcutsButton()
                .check(
                    matches(
                        hasSibling(
                            ViewMatchers.withChild(
                                allOf(
                                    withClassName(CoreMatchers.endsWith("CheckBox")),
                                    isNotChecked(),
                                ),
                            ),
                        ),
                    ),
                )
        }
    }

    class Transition {

        fun goBackToHomeScreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            goBackButton().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun clickSnackBarViewButton(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            val snackBarButton = mDevice.findObject(UiSelector().text("VIEW"))
            snackBarButton.waitForExists(waitingTimeShort)
            snackBarButton.click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

private fun shortcutsButton() =
    onView(allOf(withText(R.string.top_sites_toggle_top_recent_sites_4)))

private fun sponsoredShortcutsButton() =
    onView(allOf(withText(R.string.customize_toggle_contile)))

private fun jumpBackInButton() =
    onView(allOf(withText(R.string.customize_toggle_jump_back_in)))

private fun recentBookmarksButton() =
    onView(allOf(withText(R.string.customize_toggle_recent_bookmarks)))

private fun recentlyVisitedButton() =
    onView(allOf(withText(R.string.customize_toggle_recently_visited)))

private fun pocketButton() =
    onView(allOf(withText(R.string.customize_toggle_pocket_2)))

private fun sponsoredStoriesButton() =
    onView(allOf(withText(R.string.customize_toggle_pocket_sponsored)))

private fun openingScreenHeading() = onView(withText(R.string.preferences_opening_screen))

private fun homepageButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_homepage),
            hasSibling(withId(R.id.radio_button)),
        ),
    )

private fun lastTabButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_last_tab),
            hasSibling(withId(R.id.radio_button)),
        ),
    )

private fun homepageAfterFourHoursButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_after_four_hours_of_inactivity),
            hasSibling(withId(R.id.radio_button)),
        ),
    )

private fun goBackButton() = onView(allOf(withContentDescription(R.string.action_bar_up_description)))

private fun wallpapersMenuButton() = onView(withText("Wallpapers"))
