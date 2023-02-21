/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
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
import org.mozilla.experiments.nimbus.FeaturesInterface
import org.mozilla.experiments.nimbus.JSONVariables
import org.mozilla.experiments.nimbus.NimbusInterface
import org.mozilla.experiments.nimbus.NullVariables
import org.mozilla.experiments.nimbus.Variables
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.gleanplumb.CustomAttributeProvider
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.setLongTapTimeout
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.MessageSurfaceId
import org.mozilla.fenix.ui.robots.notificationShade

/**
 * Test to instantiate Nimbus and automatically test all trigger expressions shipping with the app.
 *
 * We do this as a UI test to make sure that:
 * - as much of the custom targeting and trigger attributes are recorded as possible.
 * - we can run the Rust JEXL evaluator.
 */
class NimbusMessagingTest {
    private lateinit var mDevice: UiDevice

    private lateinit var context: Context
    private lateinit var hardcodedNimbus: HardcodedNimbusFeatures

    @get:Rule
    val activityTestRule =
        HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    @Before
    fun setUp() {
        context = TestHelper.appContext
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun forAllTests() = runTest {
        testMessageValidity()
    }

    private val storage
        get() = context.components.analytics.messagingStorage

    private val nimbus: NimbusInterface
        get() = context.components.analytics.experiments

    @Test
    fun testValidMessages() = runTest {
        hardcodedNimbus = HardcodedNimbusFeatures(
            context,
            "messaging" to JSONObject(
                """{
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
            }""".trimIndent(),
            ),
        )
        FxNimbus.initialize { hardcodedNimbus }


        val messageDatas = FxNimbus.features.messaging.value().messages
        assertNotNull(messageDatas["test-default-browser-notification"])

        val messages = storage.getMessages().associateBy { it.id }
        val message = messages["test-default-browser-notification"]
        assertNotNull(message)
    }

    private suspend fun testMessageValidity() {
        val messages = storage.getMessages()
        val datas = FxNimbus.features.messaging.value().messages
        assertTrue(datas.isNotEmpty())
        assertEquals(messages.size, datas.size)

        val helper =
            nimbus.createMessageHelper(CustomAttributeProvider.getCustomAttributes(context))
        messages.forEach { message ->
            storage.isMessageEligible(message, helper)
        }
        assertTrue(storage.malFormedMap.isEmpty())
    }

    @Test
    fun testValidNotificationMessage() = runTest {
        hardcodedNimbus = HardcodedNimbusFeatures(
            context,
            "messaging" to JSONObject("""{
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
            }""".trimIndent(),
            ),
        )
        FxNimbus.initialize { hardcodedNimbus }

        val messages = storage.getMessages()
        val message = storage.getNextMessage(MessageSurfaceId.NOTIFICATION, messages)
        assertNotNull(message)
        assertTrue(hardcodedNimbus.exposure.contains("messaging"))
    }

    @Test
    fun testShowingNotificationMessage() {
        hardcodedNimbus = HardcodedNimbusFeatures(
            context,
            "messaging" to JSONObject("""{
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
            }""".trimIndent(),
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
                """{
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
            }""".trimIndent(),
            ),
        )
        FxNimbus.initialize { hardcodedNimbus }

        val messages = storage.getMessages()
        val message = storage.getNextMessage(MessageSurfaceId.HOMESCREEN, messages)
        assertNotNull(message)
        assertTrue(hardcodedNimbus.exposure.contains("messaging"))
    }
}

class HardcodedNimbusFeatures(
    override val context: Context,
    vararg feature: Pair<String, JSONObject>,
) : FeaturesInterface {
    private val features: Map<String, JSONObject>
    val exposure = mutableSetOf<String>()

    init {
        features = feature.toMap()
    }

    override fun getVariables(featureId: String, recordExposureEvent: Boolean): Variables =
        features[featureId]?.let {
            JSONVariables(context, it)
        } ?: NullVariables.instance

    override fun recordExposureEvent(featureId: String) {
        exposure.add(featureId)
    }
}
