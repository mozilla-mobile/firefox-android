/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mozilla.experiments.nimbus.StringHolder
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.nimbus.OnboardingCardData
import org.mozilla.fenix.nimbus.OnboardingCardType

class JunoOnboardingMapperTest {

    @get:Rule
    val activityTestRule =
        HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    @Test
    fun showNotificationTrue_showAddWidgetFalse_pagesToDisplay_returnsSortedListOfAllConvertedPages_withoutAddWidgetPage() {
        val expected = listOf(defaultBrowserPageUiData, syncPageUiData, notificationPageUiData)
        assertEquals(expected, unsortedAllKnownCardData.toPageUiData(true, false))
    }

    @Test
    fun showNotificationFalse_showAddWidgetFalse_pagesToDisplay_returnsSortedListOfConvertedPages_withoutNotificationPage_and_addWidgetPage() {
        val expected = listOf(defaultBrowserPageUiData, syncPageUiData)
        assertEquals(expected, unsortedAllKnownCardData.toPageUiData(false, false))
    }

    @Test
    fun showNotificationFalse_showAddWidgetTrue_pagesToDisplay_returnsSortedListOfAllConvertedPages_withoutNotificationPage() {
        val expected = listOf(defaultBrowserPageUiData, addSearchWidgetPageUiData, syncPageUiData)
        assertEquals(expected, unsortedAllKnownCardData.toPageUiData(false, true))
    }

    @Test
    fun showNotificationTrue_and_showAddWidgetTrue_pagesToDisplay_returnsSortedListOfConvertedPages() {
        val expected = listOf(defaultBrowserPageUiData, addSearchWidgetPageUiData, syncPageUiData, notificationPageUiData)
        assertEquals(expected, unsortedAllKnownCardData.toPageUiData(true, true))
    }
}

private val defaultBrowserPageUiData = OnboardingPageUiData(
    type = OnboardingPageUiData.Type.DEFAULT_BROWSER,
    imageRes = R.drawable.ic_onboarding_welcome,
    title = "default browser title",
    description = "default browser body with link text",
    linkText = "link text",
    primaryButtonLabel = "default browser primary button text",
    secondaryButtonLabel = "default browser secondary button text",
)
private val addSearchWidgetPageUiData = OnboardingPageUiData(
    type = OnboardingPageUiData.Type.ADD_SEARCH_WIDGET,
    imageRes = R.drawable.ic_onboarding_search_widget,
    title = "add search widget title",
    description = "add search widget body with link text",
    linkText = "link text",
    primaryButtonLabel = "add search widget primary button text",
    secondaryButtonLabel = "add search widget secondary button text",
)
private val syncPageUiData = OnboardingPageUiData(
    type = OnboardingPageUiData.Type.SYNC_SIGN_IN,
    imageRes = R.drawable.ic_onboarding_sync,
    title = "sync title",
    description = "sync body",
    primaryButtonLabel = "sync primary button text",
    secondaryButtonLabel = "sync secondary button text",
)
private val notificationPageUiData = OnboardingPageUiData(
    type = OnboardingPageUiData.Type.NOTIFICATION_PERMISSION,
    imageRes = R.drawable.ic_notification_permission,
    title = "notification title",
    description = "notification body",
    primaryButtonLabel = "notification primary button text",
    secondaryButtonLabel = "notification secondary button text",
)

private val defaultBrowserCardData = OnboardingCardData(
    cardType = OnboardingCardType.DEFAULT_BROWSER,
    imageRes = R.drawable.ic_onboarding_welcome,
    title = StringHolder(null, "default browser title"),
    body = StringHolder(null, "default browser body with link text"),
    linkText = StringHolder(null, "link text"),
    primaryButtonLabel = StringHolder(null, "default browser primary button text"),
    secondaryButtonLabel = StringHolder(null, "default browser secondary button text"),
    ordering = 10,
)
private val addSearchWidgetCardData = OnboardingCardData(
    cardType = OnboardingCardType.ADD_SEARCH_WIDGET,
    imageRes = R.drawable.ic_onboarding_search_widget,
    title = StringHolder(null, "add search widget title"),
    body = StringHolder(null, "add search widget body with link text"),
    linkText = StringHolder(null, "link text"),
    primaryButtonLabel = StringHolder(null, "add search widget primary button text"),
    secondaryButtonLabel = StringHolder(null, "add search widget secondary button text"),
    ordering = 15,
)
private val syncCardData = OnboardingCardData(
    cardType = OnboardingCardType.SYNC_SIGN_IN,
    imageRes = R.drawable.ic_onboarding_sync,
    title = StringHolder(null, "sync title"),
    body = StringHolder(null, "sync body"),
    primaryButtonLabel = StringHolder(null, "sync primary button text"),
    secondaryButtonLabel = StringHolder(null, "sync secondary button text"),
    ordering = 20,
)
private val notificationCardData = OnboardingCardData(
    cardType = OnboardingCardType.NOTIFICATION_PERMISSION,
    imageRes = R.drawable.ic_notification_permission,
    title = StringHolder(null, "notification title"),
    body = StringHolder(null, "notification body"),
    primaryButtonLabel = StringHolder(null, "notification primary button text"),
    secondaryButtonLabel = StringHolder(null, "notification secondary button text"),
    ordering = 30,
)

private val unsortedAllKnownCardData = listOf(
    syncCardData,
    notificationCardData,
    defaultBrowserCardData,
    addSearchWidgetCardData,
)
