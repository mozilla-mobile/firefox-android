package org.mozilla.fenix.ui

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.experiments.nimbus.Res
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.HomeScreenSection
import org.mozilla.fenix.nimbus.Homescreen
import org.mozilla.fenix.nimbus.MessageData
import org.mozilla.fenix.nimbus.MessageSurfaceId
import org.mozilla.fenix.nimbus.Messaging
import org.mozilla.fenix.nimbus.StyleData
import org.mozilla.fenix.ui.robots.homeScreen

class NimbusMessageHomescreenTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer

    private var messageButtonLabel = "CLICK ME"
    private var messageText = "Some Nimbus Messaging text"
    private var messageTitle = "A Nimbus title"

    @get:Rule
    val homeActivityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides(
        skipOnboarding = true,
    ).withIntent(
        Intent().apply {
            action = Intent.ACTION_VIEW
        },
    )

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        // Set up nimbus message
        FxNimbus.features.messaging.withInitializer {
            // FML generated objects.
            Messaging(
                messages = mapOf(
                    "test-message" to MessageData(
                        action = Res.string("TEST ACTION"),
                        style = "URGENT",
                        buttonLabel = Res.string(messageButtonLabel),
                        text = Res.string(messageText),
                        title = Res.string(messageTitle),
                        trigger = listOf("ALWAYS"),
                    ),
                ),
                styles = mapOf(
                    "TEST STYLE" to StyleData(),
                ),
                actions = mapOf(
                    "TEST ACTION" to "https://example.com",
                ),
                triggers = mapOf(
                    "ALWAYS" to "true",
                ),
            )
        }

        // Remove some homescreen features not needed for testing
        FxNimbus.features.homescreen.withInitializer {
            // These are FML generated objects and enums
            Homescreen(
                sectionsEnabled = mapOf(
                    HomeScreenSection.JUMP_BACK_IN to false,
                    HomeScreenSection.POCKET to false,
                    HomeScreenSection.POCKET_SPONSORED_STORIES to false,
                    HomeScreenSection.RECENT_EXPLORATIONS to false,
                    HomeScreenSection.RECENTLY_SAVED to false,
                    HomeScreenSection.TOP_SITES to false,
                ),
            )
        }

        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
        // refresh message store
        appContext.components.appStore.dispatch(
            AppAction.MessagingAction.Restore,
        )
        appContext.components.appStore.dispatch(
            AppAction.MessagingAction.Evaluate(
                MessageSurfaceId.HOMESCREEN,
            ),
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testNimbusMessageIsDisplayed() {
        // Checks the home screen card message is displayed correctly
        homeScreen {
            verifyNimbusMessageCard(messageTitle, messageText, messageButtonLabel)
        }
    }
}
