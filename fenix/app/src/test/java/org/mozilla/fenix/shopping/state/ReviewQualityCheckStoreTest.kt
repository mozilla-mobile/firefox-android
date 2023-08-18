/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.state

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Rule
import org.junit.Test

class ReviewQualityCheckStoreTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    private val dispatcher = coroutinesTestRule.testDispatcher
    private val scope = coroutinesTestRule.scope

    @Test
    fun `GIVEN the user has not opted in the feature WHEN store is created THEN state should display not opted in UI`() =
        runTest {
            val tested = ReviewQualityCheckStore(
                reviewQualityCheckPreferences = FakeReviewQualityCheckPreferences(
                    isEnabled = false,
                ),
                scope = scope,
            )
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()
            tested.waitUntilIdle()

            val expected = ReviewQualityCheckState.NotOptedIn
            assertEquals(expected, tested.state)
        }

    @Test
    fun `GIVEN the user has not opted in the feature WHEN the user opts in THEN state should display opted in UI`() =
        runTest {
            val tested = ReviewQualityCheckStore(
                reviewQualityCheckPreferences = FakeReviewQualityCheckPreferences(
                    isEnabled = false,
                    isProductRecommendationsEnabled = false,
                ),
                scope = scope,
            )
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()
            tested.dispatch(ReviewQualityCheckAction.OptIn).joinBlocking()
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()

            val expected = ReviewQualityCheckState.OptedIn(productRecommendationsPreference = false)
            assertEquals(expected, tested.state)
        }

    @Test
    fun `GIVEN the user has opted in the feature WHEN the user opts out THEN state should display not opted in UI`() =
        runTest {
            val tested = ReviewQualityCheckStore(
                reviewQualityCheckPreferences = FakeReviewQualityCheckPreferences(
                    isEnabled = true,
                    isProductRecommendationsEnabled = true,
                ),
                scope = scope,
            )
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()
            tested.dispatch(ReviewQualityCheckAction.OptOut).joinBlocking()
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()

            val expected = ReviewQualityCheckState.NotOptedIn
            assertEquals(expected, tested.state)
        }

    @Test
    fun `GIVEN the user has opted in the feature and product recommendations are off WHEN the user turns on product recommendations THEN state should reflect that`() =
        runTest {
            val tested = ReviewQualityCheckStore(
                reviewQualityCheckPreferences = FakeReviewQualityCheckPreferences(
                    isEnabled = true,
                    isProductRecommendationsEnabled = false,
                ),
                scope = scope,
            )
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()
            tested.dispatch(ReviewQualityCheckAction.ToggleProductRecommendation).joinBlocking()
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()

            val expected = ReviewQualityCheckState.OptedIn(productRecommendationsPreference = true)
            assertEquals(expected, tested.state)
        }

    @Test
    fun `GIVEN the user has opted in the feature and product recommendations are on WHEN the user turns off product recommendations THEN state should reflect that`() =
        runTest {
            val tested = ReviewQualityCheckStore(
                reviewQualityCheckPreferences = FakeReviewQualityCheckPreferences(
                    isEnabled = true,
                    isProductRecommendationsEnabled = true,
                ),
                scope = scope,
            )
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()
            tested.dispatch(ReviewQualityCheckAction.ToggleProductRecommendation).joinBlocking()
            tested.waitUntilIdle()
            dispatcher.scheduler.advanceUntilIdle()

            val expected = ReviewQualityCheckState.OptedIn(productRecommendationsPreference = false)
            assertEquals(expected, tested.state)
        }
}

private class FakeReviewQualityCheckPreferences(
    private val isEnabled: Boolean = false,
    private val isProductRecommendationsEnabled: Boolean = false,
) : ReviewQualityCheckPreferences {
    override suspend fun enabled(): Boolean = isEnabled

    override suspend fun productRecommendationsEnabled(): Boolean = isProductRecommendationsEnabled

    override suspend fun setEnabled(isEnabled: Boolean) {
    }

    override suspend fun setProductRecommendationsEnabled(isEnabled: Boolean) {
    }
}
