package org.mozilla.fenix.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.ext.toUri
import org.mozilla.fenix.ui.robots.navigationToolbar

class NetworkTest {
    @get:Rule
    val activityTestRule =
        HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    @Test
    fun urlBarTest() {
        // Note: This test may fail in GSM/GPRS
        // Bug: https://bugzilla.mozilla.org/show_bug.cgi?id=1868469
        val urls = listOf("indianexpress.com", "nzherald.co.nz")
        urls.mapNotNull { it.toUri() }.forEach { uri ->
            navigationToolbar {}.enterURLAndEnterToBrowser(uri) {
                println("Entered URL: $uri")
                verifyUrl(uri.toString())
            }
        }
    }
}
