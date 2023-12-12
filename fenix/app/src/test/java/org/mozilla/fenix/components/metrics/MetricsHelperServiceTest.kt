/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import io.mockk.every
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MetricsHelperServiceTest {
    val context: Context = testContext
    lateinit var metricsHelperService: MetricsHelperService

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @Before
    fun setup() {
        every { context.settings() } returns Settings(testContext)
        metricsHelperService = MetricsHelperService(context)
    }

    @Test
    fun `WHEN an Event of type UsageData is sent THEN shouldTrack returns true`() {
        assertTrue("shouldTrack UsageData", metricsHelperService.shouldTrack(Event.UsageData.OpenMenu))
    }

    @Test
    fun `GIVEN default state of noOperationMenuOpen is false WHEN menu is opened THEN noOperationMenuOpen is set to true`() {
        assertEquals(false, context.settings().noOperationMenuOpen)

        metricsHelperService.track(Event.UsageData.OpenMenu)

        assertEquals(true, context.settings().noOperationMenuOpen)
    }

    @Test
    fun `GIVEN menu is opened WHEN menuItem is interacted with THEN noOperationMenuOpen is set to false`() {
        metricsHelperService.track(Event.UsageData.OpenMenu)

        assertEquals(true, context.settings().noOperationMenuOpen)

        metricsHelperService.track(Event.UsageData.InteractedWithMenu)

        assertEquals(false, context.settings().noOperationMenuOpen)
    }

    @Test
    fun `WHEN setMenuAnchorFragment event is sent THEN saved anchor fragment reflects change`() {
        assertEquals(MetricsHelperService.BROWSER_ANCHOR, context.settings().anchorFragment)

        metricsHelperService.track(Event.UsageData.SetMenuAnchorFragment(MetricsHelperService.HOME_ANCHOR))

        assertEquals(MetricsHelperService.HOME_ANCHOR, context.settings().anchorFragment)

        metricsHelperService.track(Event.UsageData.SetMenuAnchorFragment(MetricsHelperService.BROWSER_ANCHOR))

        assertEquals(MetricsHelperService.BROWSER_ANCHOR, context.settings().anchorFragment)
    }
}
