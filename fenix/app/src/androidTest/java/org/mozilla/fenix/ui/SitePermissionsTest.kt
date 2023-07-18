/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import androidx.core.net.toUri
import androidx.test.rule.GrantPermissionRule
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MockLocationUpdatesRule
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.assertExternalAppOpens
import org.mozilla.fenix.helpers.TestHelper.grantSystemPermission
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.sitePermissionsRobot

/**
 *  Tests for verifying site permissions prompts & functionality
 *
 */
class SitePermissionsTest {
    /* Test page created and handled by the Mozilla mobile test-eng team */
    private lateinit var mockWebServer: MockWebServer
    private val testPage = "https://mozilla-mobile.github.io/testapp/permissions"
    private val testPageSubstring = "https://mozilla-mobile.github.io:443"
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val micManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule(
        isJumpBackInCFREnabled = false,
        isPWAsPromptEnabled = false,
        isTCPCFREnabled = false,
        isDeleteSitePermissionsEnabled = true,
    )

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @get: Rule
    val mockLocationUpdatesRule = MockLocationUpdatesRule()

    @get: Rule
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @SmokeTest
    @Test
    fun audioVideoPermissionChoiceOnEachRequestTest() {
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartAudioVideoButton {
            verifyAudioVideoPermissionPrompt(testPageSubstring)
        }.clickPagePermissionButton(false) {
            verifyPageContent("Camera and Microphone not allowed")
        }.clickStartAudioVideoButton {
        }.clickPagePermissionButton(true) {
            verifyPageContent("Camera and Microphone allowed")
        }
    }

    @SmokeTest
    @Test
    fun rememberBlockAudioVideoPermissionChoiceTest() {
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())
        assumeTrue(micManager.microphones.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartAudioVideoButton {
            verifyAudioVideoPermissionPrompt(testPageSubstring)
            selectRememberPermissionDecision()
        }.clickPagePermissionButton(false) {
            verifyPageContent("Camera and Microphone not allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickStartAudioVideoButton { }
        browserScreen {
            verifyPageContent("Camera and Microphone not allowed")
        }
    }

    @SmokeTest
    @Test
    fun rememberAllowAudioVideoPermissionChoiceTest() {
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())
        assumeTrue(micManager.microphones.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartAudioVideoButton {
            verifyAudioVideoPermissionPrompt(testPageSubstring)
            selectRememberPermissionDecision()
        }.clickPagePermissionButton(true) {
            verifyPageContent("Camera and Microphone allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickStartAudioVideoButton { }
        browserScreen {
            verifyPageContent("Camera and Microphone allowed")
        }
    }

    @Test
    fun microphonePermissionChoiceOnEachRequestTest() {
        assumeTrue(micManager.microphones.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartMicrophoneButton {
            verifyMicrophonePermissionPrompt(testPageSubstring)
        }.clickPagePermissionButton(false) {
            verifyPageContent("Microphone not allowed")
        }.clickStartMicrophoneButton {
        }.clickPagePermissionButton(true) {
            verifyPageContent("Microphone allowed")
        }
    }

    @Test
    fun rememberBlockMicrophonePermissionChoiceTest() {
        assumeTrue(micManager.microphones.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartMicrophoneButton {
            verifyMicrophonePermissionPrompt(testPageSubstring)
            selectRememberPermissionDecision()
        }.clickPagePermissionButton(false) {
            verifyPageContent("Microphone not allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickStartMicrophoneButton { }
        browserScreen {
            verifyPageContent("Microphone not allowed")
        }
    }

    @Test
    fun rememberAllowMicrophonePermissionChoiceTest() {
        assumeTrue(micManager.microphones.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartMicrophoneButton {
            verifyMicrophonePermissionPrompt(testPageSubstring)
            selectRememberPermissionDecision()
        }.clickPagePermissionButton(true) {
            verifyPageContent("Microphone allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickStartMicrophoneButton { }
        browserScreen {
            verifyPageContent("Microphone allowed")
        }
    }

    @Test
    fun cameraPermissionChoiceOnEachRequestTest() {
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartCameraButton {
            verifyCameraPermissionPrompt(testPageSubstring)
        }.clickPagePermissionButton(false) {
            verifyPageContent("Camera not allowed")
        }.clickStartCameraButton {
        }.clickPagePermissionButton(true) {
            verifyPageContent("Camera allowed")
        }
    }

    @Test
    fun rememberBlockCameraPermissionChoiceTest() {
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartCameraButton {
            verifyCameraPermissionPrompt(testPageSubstring)
            selectRememberPermissionDecision()
        }.clickPagePermissionButton(false) {
            verifyPageContent("Camera not allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickStartCameraButton { }
        browserScreen {
            verifyPageContent("Camera not allowed")
        }
    }

    @Test
    fun rememberAllowCameraPermissionChoiceTest() {
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
            waitForPageToLoad()
        }.clickStartCameraButton {
            verifyCameraPermissionPrompt(testPageSubstring)
            selectRememberPermissionDecision()
        }.clickPagePermissionButton(true) {
            verifyPageContent("Camera allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickStartCameraButton { }
        browserScreen {
            verifyPageContent("Camera allowed")
        }
    }

    @Test
    fun blockNotificationsPermissionPromptTest() {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickOpenNotificationButton {
            verifyNotificationsPermissionPrompt(testPageSubstring)
        }.clickPagePermissionButton(false) {
            verifyPageContent("Notifications not allowed")
        }.openThreeDotMenu {
        }.refreshPage {
            waitForPageToLoad()
        }.clickOpenNotificationButton {
            verifyNotificationsPermissionPrompt(testPageSubstring, true)
        }
    }

    @Test
    fun allowNotificationsPermissionPromptTest() {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickOpenNotificationButton {
            verifyNotificationsPermissionPrompt(testPageSubstring)
        }.clickPagePermissionButton(true) {
            verifyPageContent("Notifications allowed")
        }
    }

    @Test
    fun allowLocationPermissionsTest() {
        mockLocationUpdatesRule.setMockLocation()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickGetLocationButton {
            verifyLocationPermissionPrompt(url = testPageSubstring, exists = true)
        }.clickPagePermissionButton(true) {
            verifyPageContent("${mockLocationUpdatesRule.latitude}")
            verifyPageContent("${mockLocationUpdatesRule.longitude}")
        }
    }

    @Test
    fun blockLocationPermissionsTest() {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickGetLocationButton {
            verifyLocationPermissionPrompt(url = testPageSubstring, exists = true)
        }.clickPagePermissionButton(false) {
            verifyPageContent("User denied geolocation prompt")
        }
    }

    @SmokeTest
    @Test
    fun fileUploadPermissionTest() {
        val testPage = TestAssetHelper.getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.url) {
            clickPageObject(itemWithResId("upload_file"))
            grantSystemPermission()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertExternalAppOpens("com.google.android.documentsui")
            } else {
                assertExternalAppOpens("com.android.documentsui")
            }
        }
    }

    @Test
    fun verifyPermissionPromptForInsecureWebsiteTest() {
        val insecureWebsite = "http://chrisdavidmills.github.io/location-finder-permissions-api"
        val secureWebsite = "https://chrisdavidmills.github.io/location-finder-permissions-api"
        val secureWebsiteSubstring = "https://chrisdavidmills.github.io:443"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(insecureWebsite.toUri()) {
            waitForPageToLoad()
        }
        sitePermissionsRobot {
            verifyLocationPermissionPrompt(url = secureWebsiteSubstring, exists = false)
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(secureWebsite.toUri()) {
            waitForPageToLoad()
        }
        sitePermissionsRobot {
            verifyLocationPermissionPrompt(url = secureWebsiteSubstring, exists = true)
        }
    }
}
