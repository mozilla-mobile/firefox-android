/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.app.NotificationManager
import android.content.Context
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.Constants.RETRY_COUNT
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdAndTextExists
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import kotlin.AssertionError

class NotificationRobot {

    fun verifySystemNotificationExists(notificationMessage: String) {
        val notification = UiSelector().text(notificationMessage)
        var notificationFound = mDevice.findObject(notification).waitForExists(waitingTime)

        while (!notificationFound) {
            scrollToEnd()
            notificationFound = mDevice.findObject(notification).waitForExists(waitingTime)
        }

        assertTrue(notificationFound)
    }

    fun clearNotifications() {
        if (clearButton.exists()) {
            clearButton.click()
        } else {
            scrollToEnd()
            if (clearButton.exists()) {
                clearButton.click()
            } else if (notificationTray().exists()) {
                mDevice.pressBack()
            }
        }
    }

    fun cancelAllShownNotifications() {
        cancelAll()
    }

    fun verifySystemNotificationDoesNotExist(notificationMessage: String) {
        mDevice.findObject(UiSelector().textContains(notificationMessage)).waitUntilGone(waitingTime)
        assertFalse(
            mDevice.findObject(UiSelector().textContains(notificationMessage)).waitForExists(waitingTimeShort),
        )
    }

    fun verifyPrivateTabsNotification() {
        verifySystemNotificationExists("$appName (Private)")
        verifySystemNotificationExists("Close private tabs")
    }

    fun clickMediaNotificationControlButton(action: String) {
        mediaSystemNotificationButton(action).waitForExists(waitingTime)
        mediaSystemNotificationButton(action).click()
    }

    fun clickDownloadNotificationControlButton(action: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertItemWithResIdAndTextExists(downloadSystemNotificationButton(action))
                downloadSystemNotificationButton(action).clickAndWaitForNewWindow(waitingTimeShort)
                assertItemWithResIdAndTextExists(
                    downloadSystemNotificationButton(action),
                    exists = false,
                )

                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                }
                mDevice.waitForWindowUpdate(packageName, waitingTimeShort)
            }
        }
    }

    fun verifyMediaSystemNotificationButtonState(action: String) {
        assertTrue(mediaSystemNotificationButton(action).waitForExists(waitingTime))
    }

    fun expandNotificationMessage() {
        while (!notificationHeader.exists()) {
            scrollToEnd()
        }

        if (notificationHeader.exists()) {
            // expand the notification
            notificationHeader.click()

            // double check if notification actions are viewable by checking for action existence; otherwise scroll again
            while (!mDevice.findObject(UiSelector().resourceId("android:id/action0")).exists() &&
                !mDevice.findObject(UiSelector().resourceId("android:id/actions_container")).exists()
            ) {
                scrollToEnd()
            }
        }
    }

    // Performs swipe action on download system notifications
    fun swipeDownloadNotification(
        direction: String,
        shouldDismissNotification: Boolean,
        canExpandNotification: Boolean = true,
    ) {
        // In case it fails, retry max 3x the swipe action on download system notifications
        for (i in 1..RETRY_COUNT) {
            try {
                var retries = 0
                while (itemContainingText(appName).exists() && retries++ < 3) {
                    // Swipe left the download system notification
                    if (direction == "Left") {
                        itemContainingText(appName)
                            .also {
                                it.waitForExists(waitingTime)
                                it.swipeLeft(3)
                            }
                    } else {
                        // Swipe right the download system notification
                        itemContainingText(appName)
                            .also {
                                it.waitForExists(waitingTime)
                                it.swipeRight(3)
                            }
                    }
                }
                // Not all download related system notifications can be dismissed
                if (shouldDismissNotification) {
                    assertFalse(itemContainingText(appName).waitForExists(waitingTimeShort))
                } else {
                    assertTrue(itemContainingText(appName).waitForExists(waitingTimeShort))
                }

                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    notificationShade {
                    }.closeNotificationTray {
                    }.openNotificationShade {
                        // The download complete system notification can't be expanded
                        if (canExpandNotification) {
                            // In all cases the download system notification title will be the app name
                            verifySystemNotificationExists(appName)
                            expandNotificationMessage()
                        } else {
                            // Using the download completed system notification summary to bring in to view an properly verify it
                            verifySystemNotificationExists("Download completed")
                        }
                    }
                }
            }
        }
    }

    fun clickNotification(notificationMessage: String) {
        mDevice.findObject(UiSelector().text(notificationMessage)).waitForExists(waitingTime)
        mDevice.findObject(UiSelector().text(notificationMessage)).clickAndWaitForNewWindow(waitingTimeShort)
    }

    class Transition {

        fun clickClosePrivateTabsNotification(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            try {
                assertTrue(
                    closePrivateTabsNotification().exists(),
                )
            } catch (e: AssertionError) {
                notificationTray().flingToEnd(1)
            }

            closePrivateTabsNotification().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun closeNotificationTray(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.pressBack()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun notificationShade(interact: NotificationRobot.() -> Unit): NotificationRobot.Transition {
    NotificationRobot().interact()
    return NotificationRobot.Transition()
}

private fun closePrivateTabsNotification() =
    mDevice.findObject(UiSelector().text("Close private tabs"))

private fun downloadSystemNotificationButton(action: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("android:id/action0")
            .textContains(action),
    )

private fun mediaSystemNotificationButton(action: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("com.android.systemui:id/action0")
            .descriptionContains(action),
    )

private fun notificationTray() = UiScrollable(
    UiSelector().resourceId("com.android.systemui:id/notification_stack_scroller"),
).setAsVerticalList()

private val notificationHeader =
    mDevice.findObject(
        UiSelector()
            .resourceId("android:id/app_name_text")
            .text(appName),
    )

private fun scrollToEnd() {
    notificationTray().scrollToEnd(1)
}

private val clearButton = mDevice.findObject(UiSelector().resourceId("com.android.systemui:id/dismiss_text"))

private fun cancelAll() {
    val notificationManager: NotificationManager =
        TestHelper.appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancelAll()
}
