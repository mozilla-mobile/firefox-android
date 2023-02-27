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
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.gleanplumb.CustomAttributeProvider
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.HardcodedNimbusFeatures
import org.mozilla.fenix.nimbus.MessageSurfaceId
import org.mozilla.fenix.ui.robots.notificationShade

/**
 * A UI test for testing that
 * a) surfaces work and remain working
 * b) individual messages are valid and working.
 */
class NimbusMessagingTest {
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

    @After
    fun tearDown() {
        notificationShade {
            clearNotifications()
        }
    }

    /**
     * This is run on every test, to check if the number of messages in the input
     * — i.e. the FML and in the test— are internally consistent, and have well formed
     * JEXL strings.
     */
    @After
    fun checkAllMessageIntegrity() = runTest {
        val messages = storage.getMessages()
        val rawMessages = FxNimbus.features.messaging.value().messages
        assertTrue(rawMessages.isNotEmpty())
        assertEquals(messages.size, rawMessages.size)

        val nimbus = context.components.analytics.experiments
        val helper = nimbus.createMessageHelper(
            CustomAttributeProvider.getCustomAttributes(context),
        )
        messages.forEach { message ->
            storage.isMessageEligible(message, helper)
        }
        assertTrue(storage.malFormedMap.isEmpty())
    }

    @Test
    fun testSingleMessageIsValid() = runTest {
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
                        "I_AM_NOT_DEFAULT_BROWSER",
                        "INACTIVE_2_DAYS"
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        FxNimbus.initialize { hardcodedNimbus }

        val rawMessages = FxNimbus.features.messaging.value().messages
        assertNotNull(rawMessages["test-default-browser-notification"])

        val messages = storage.getMessages().associateBy { it.id }
        val message = messages["test-default-browser-notification"]
        assertNotNull(message)
    }

    @Test
    fun testValidNotificationMessage() = runTest {
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
        FxNimbus.initialize { hardcodedNimbus }

        val messages = storage.getMessages()
        val message = storage.getNextMessage(MessageSurfaceId.NOTIFICATION, messages)
        assertNotNull(message)
        assertTrue(hardcodedNimbus.isExposed("messaging"))
    }

    @Test
    fun testShowingNotificationMessage() {
        hardcodedNimbus = HardcodedNimbusFeatures(
            context,
            "messaging" to JSONObject(
                """
                {
                  "notification-config": {
                    "refresh-interval": 0
                  },
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

    @Test
    fun testValidHomescreenMessage() = runTest {
        hardcodedNimbus = HardcodedNimbusFeatures(
            context,
            "messaging" to JSONObject(
                """
                {
                  "message-under-experiment": "test-default-browser",
                  "messages": {
                    "test-default-browser": {
                      "title": "preferences_set_as_default_browser",
                      "text": "default_browser_experiment_card_text",
                      "surface": "homescreen",
                      "style": "DEFAULT",
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
        FxNimbus.initialize { hardcodedNimbus }

        val messages = storage.getMessages()
        val message = storage.getNextMessage(MessageSurfaceId.HOMESCREEN, messages)
        assertNotNull(message)
        assertTrue(hardcodedNimbus.isExposed("messaging"))
    }
}
