/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import android.util.Log
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.mDevice

/**
 * Helper for querying and interacting with items based on their matchers.
 */
object MatcherHelper {

    fun itemWithResId(resourceId: String): UiObject {
        Log.i(TAG, "Looking for item with resource id: $resourceId")
        return mDevice.findObject(UiSelector().resourceId(resourceId))
    }

    fun itemContainingText(itemText: String): UiObject {
        Log.i(TAG, "Looking for item with text: $itemText")
        return mDevice.findObject(UiSelector().textContains(itemText))
    }

    fun itemWithText(itemText: String): UiObject {
        Log.i(TAG, "Looking for item with text: $itemText")
        return mDevice.findObject(UiSelector().text(itemText))
    }

    fun itemWithDescription(description: String): UiObject {
        Log.i(TAG, "Looking for item with description: $description")
        return mDevice.findObject(UiSelector().descriptionContains(description))
    }

    fun itemWithIndex(index: Int): UiObject {
        Log.i(TAG, "Looking for item with index: $index")
        return mDevice.findObject(UiSelector().index(index))
    }

    fun itemWithClassName(className: String): UiObject {
        Log.i(TAG, "Looking for item with class name: $className")
        return mDevice.findObject(UiSelector().className(className))
    }

    fun itemWithResIdAndIndex(resourceId: String, index: Int): UiObject {
        Log.i(TAG, "Looking for item with resource id: $resourceId and index: $index")
        return mDevice.findObject(UiSelector().resourceId(resourceId).index(index))
    }

    fun itemWithClassNameAndIndex(className: String, index: Int): UiObject {
        Log.i(TAG, "Looking for item with class name: $className and index: $index")
        return mDevice.findObject(UiSelector().className(className).index(index))
    }

    fun checkedItemWithResId(resourceId: String, isChecked: Boolean): UiObject {
        Log.i(TAG, "Looking for checked item with resource id: $resourceId")
        return mDevice.findObject(UiSelector().resourceId(resourceId).checked(isChecked))
    }

    fun checkedItemWithResIdAndText(resourceId: String, text: String, isChecked: Boolean): UiObject {
        Log.i(TAG, "Looking for checked item with resource id: $resourceId and text: $text")
        return mDevice.findObject(
            UiSelector()
                .resourceId(resourceId)
                .textContains(text)
                .checked(isChecked),
        )
    }

    fun itemWithResIdAndDescription(resourceId: String, description: String): UiObject {
        Log.i(TAG, "Looking for item with resource id: $resourceId and description: $description")
        return mDevice.findObject(UiSelector().resourceId(resourceId).descriptionContains(description))
    }

    fun itemWithResIdAndText(resourceId: String, text: String): UiObject {
        Log.i(TAG, "Looking for item with resource id: $resourceId and text: $text")
        return mDevice.findObject(UiSelector().resourceId(resourceId).text(text))
    }

    fun itemWithResIdContainingText(resourceId: String, text: String): UiObject {
        Log.i(TAG, "Looking for item with resource id: $resourceId and containing text: $text")
        return mDevice.findObject(UiSelector().resourceId(resourceId).textContains(text))
    }

    fun assertUIObjectExists(
        vararg appItems: UiObject,
        exists: Boolean = true,
        waitingTime: Long = TestAssetHelper.waitingTime,
    ) {
        for (appItem in appItems) {
            if (exists) {
                assertTrue("${appItem.selector} does not exist", appItem.waitForExists(waitingTime))
                Log.i(TAG, "assertUIObjectExists: Verified ${appItem.selector} exists")
            } else {
                assertFalse("${appItem.selector} exists", appItem.waitForExists(waitingTimeShort))
                Log.i(TAG, "assertUIObjectExists: Verified ${appItem.selector} does not exist")
            }
        }
    }

    fun assertUIObjectIsGone(vararg appItems: UiObject) {
        for (appItem in appItems) {
            assertTrue("${appItem.selector} is not gone", appItem.waitUntilGone(waitingTime))
            Log.i(TAG, "assertUIObjectIsGone: Verified ${appItem.selector} is gone")
        }
    }

    fun assertItemTextEquals(vararg appItems: UiObject, expectedText: String, isEqual: Boolean = true) {
        for (appItem in appItems) {
            if (isEqual) {
                assertTrue(
                    "${appItem.selector} text does not equal to $expectedText",
                    appItem.text.equals(expectedText),
                )
                Log.i(TAG, "assertItemTextEquals: Verified ${appItem.selector} text equals to $expectedText")
            } else {
                assertFalse(
                    "${appItem.selector} text equals to $expectedText",
                    appItem.text.equals(expectedText),
                )
                Log.i(TAG, "assertItemTextEquals: Verified ${appItem.selector} text does not equal to $expectedText")
            }
        }
    }

    fun assertItemTextContains(vararg appItems: UiObject, itemText: String) {
        for (appItem in appItems) {
            assertTrue(
                "${appItem.selector} text does not contain $itemText",
                appItem.text.contains(itemText),
            )
            Log.i(TAG, "assertItemTextContains: Verified ${appItem.selector} text contains $itemText")
        }
    }

    fun assertItemIsEnabledAndVisible(vararg appItems: UiObject) {
        for (appItem in appItems) {
            assertTrue(appItem.waitForExists(waitingTime) && appItem.isEnabled)
            Log.i(TAG, "assertItemIsEnabledAndVisible: Verified ${appItem.selector} is visible and enabled")
        }
    }
}
