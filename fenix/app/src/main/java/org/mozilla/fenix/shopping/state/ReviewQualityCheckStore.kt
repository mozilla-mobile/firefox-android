/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shopping.state

import kotlinx.coroutines.CoroutineScope
import mozilla.components.lib.state.Store

/**
 * Store for review quality check feature.
 *
 * @param reviewQualityCheckPreferences The [ReviewQualityCheckPreferences] instance to use.
 * @param scope The [CoroutineScope] to use for launching coroutines.
 */
class ReviewQualityCheckStore(
    reviewQualityCheckPreferences: ReviewQualityCheckPreferences,
    scope: CoroutineScope,
) : Store<ReviewQualityCheckState, ReviewQualityCheckAction>(
    initialState = ReviewQualityCheckState.Initial,
    middleware = listOf(
        ReviewQualityCheckPreferencesMiddleware(reviewQualityCheckPreferences, scope),
    ),
    reducer = ::reducer,
) {
    init {
        dispatch(ReviewQualityCheckAction.Init)
    }
}

private fun reducer(
    state: ReviewQualityCheckState,
    action: ReviewQualityCheckAction,
): ReviewQualityCheckState {
    if (action is ReviewQualityCheckAction.UpdateAction) {
        return mapStateForUpdateAction(state, action)
    }

    return state
}

private fun mapStateForUpdateAction(
    state: ReviewQualityCheckState,
    action: ReviewQualityCheckAction.UpdateAction,
): ReviewQualityCheckState {
    when (action) {
        is ReviewQualityCheckAction.UpdateUserPreferences -> {
            return if (action.hasUserOptedIn) {
                if (state is ReviewQualityCheckState.OptedIn) {
                    state.copy(productRecommendationsPreference = action.isProductRecommendationsEnabled)
                } else {
                    ReviewQualityCheckState.OptedIn(
                        productRecommendationsPreference = action.isProductRecommendationsEnabled,
                    )
                }
            } else {
                ReviewQualityCheckState.NotOptedIn
            }
        }

        ReviewQualityCheckAction.OptOut -> {
            return ReviewQualityCheckState.NotOptedIn
        }

        ReviewQualityCheckAction.ToggleProductRecommendation -> {
            return if (state is ReviewQualityCheckState.OptedIn) {
                state.copy(productRecommendationsPreference = !state.productRecommendationsPreference)
            } else {
                state
            }
        }
    }
}
