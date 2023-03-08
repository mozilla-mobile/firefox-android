/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.GrantPermissionRule.grant
import androidx.test.uiautomator.UiDevice
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.HardcodedNimbusFeatures
import org.mozilla.fenix.ui.robots.notificationShade

/**
 * A UI test for testing that surfaces work and remain working.
 */
class NimbusMessagingNotificationTest {
    private lateinit var mDevice: UiDevice

    private lateinit var context: Context
    private lateinit var hardcodedNimbus: HardcodedNimbusFeatures

    private val storage
        get() = context.components.analytics.messagingStorage

    @get:Rule
    val activityTestRule =
        HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) {
            grant(
                "android.permission.POST_NOTIFICATIONS",
            )
        } else {
            grant()
        }

    @Before
    fun setUp() {
        context = TestHelper.appContext
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testShowingNotificationMessage() {
        hardcodedNimbus = HardcodedNimbusFeatures(
            context,
            "messaging" to JSONObject(
                """
                {
                  "message-under-experiment": "test-default-browser-notification",
                  "messages": {
                    "test-default-browser-notification": {
                      "title": "preferences_set_as_default_browser",
                      "text": "default_browser_experiment_card_text",
                      "surface": "notification",
                      "style": "NOTIFICATION",
                      "action": "MAKE_DEFAULT_BROWSER",
                      "trigger": [
                        "ALWAYS"
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        activityTestRule.finishActivity()
        FxNimbus.initialize { hardcodedNimbus }
        activityTestRule.launchActivity(null)

        mDevice.openNotification()
        notificationShade {
            val data = FxNimbus.features.messaging.value().messages["test-default-browser-notification"]
            verifySystemNotificationExists(data!!.title!!)
            verifySystemNotificationExists(data.text)
        }
    }
}
