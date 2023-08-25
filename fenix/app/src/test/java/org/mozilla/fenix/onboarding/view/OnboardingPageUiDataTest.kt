/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.R

class OnboardingPageUiDataTest {

    @Test
    fun `GIVEN first page in the list WHEN sequencePosition called THEN returns the index plus 1`() {
        val expected = "1"
        val actual = allKnownPages.sequencePosition(defaultBrowserPageUiData.type)

        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN last page in the list WHEN sequencePosition called THEN returns the index plus 1`() {
        val expected = "3"
        val actual = allKnownPages.sequencePosition(notificationPageUiData.type)

        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN all known pages of WHEN sequenceId() called THEN should map to the correct sequence id`() {
        val expected = "default_sync_notification"
        val actual = allKnownPages.telemetrySequenceId()

        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN some of the known pages WHEN sequenceId() called THEN should map to the correct sequence id`() {
        val expected = "default_sync"
        val actual = listOf(defaultBrowserPageUiData, syncPageUiData).telemetrySequenceId()

        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a single known page WHEN sequenceId() called THEN should map to the correct sequence id`() {
        val expected = "default"
        val actual = listOf(defaultBrowserPageUiData).telemetrySequenceId()

        assertEquals(expected, actual)
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

private val allKnownPages = listOf(
    defaultBrowserPageUiData,
    syncPageUiData,
    notificationPageUiData,
)
