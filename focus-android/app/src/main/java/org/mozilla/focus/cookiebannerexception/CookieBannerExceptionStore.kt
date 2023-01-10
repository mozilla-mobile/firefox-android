/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.cookiebannerexception

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

/**
 * The [CookieBannerExceptionStore] holds the [CookieBannerExceptionState] (state tree).
 *
 * The only way to change the [CookieBannerExceptionState] inside
 * [CookieBannerExceptionStore] is to dispatch an [CookieBannerExceptionAction] on it.
 */
class CookieBannerExceptionStore(
    initialState: CookieBannerExceptionState,
    middlewares: List<Middleware<CookieBannerExceptionState, CookieBannerExceptionAction>> = emptyList(),
) : Store<CookieBannerExceptionState, CookieBannerExceptionAction>(
    initialState,
    ::cookieBannerExceptionStateReducer,
    middlewares,
) {
    init {
        dispatch(CookieBannerExceptionAction.InitCookieBannerException)
    }
}

/**
 * The state of the language selection
 *
 * @property isCookieBannerToggleEnabled Current status of cookie banner toggle from details exception
 * @property shouldShowCookieBannerItem Visibility of cookie banner exception item
 * @property cookieBannerExceptionStatus Current status of the site, if it has or doesn't have a cookie banner .
 * If the site has a cookie banner ,current status of cookie banner exception.
 */
data class CookieBannerExceptionState(
    val isCookieBannerToggleEnabled: Boolean = false,
    val shouldShowCookieBannerItem: Boolean = false,
    val isCookieBannerDetected: Boolean = false,
    val cookieBannerExceptionStatus: CookieBannerExceptionStatus = CookieBannerExceptionStatus.NoCookieBannerDetected,
) : State

/**
 * Action to dispatch through the `CookieBannerExceptionStore` to modify cookie banner exception item and item details
 * from Tracking protection panel through the reducer.
 */
@Suppress("UndocumentedPublicClass")
sealed class CookieBannerExceptionAction : Action {
    object InitCookieBannerException : CookieBannerExceptionAction()

    data class ToggleCookieBannerException(
        val isCookieBannerHandlingExceptionEnabled: Boolean,
    ) : CookieBannerExceptionAction()

    data class UpdateCookieBannerExceptionVisibility(
        val shouldShowCookieBannerItem: Boolean,
    ) : CookieBannerExceptionAction()

    data class CookieBannerDetected(
        val isCookieBannerDetected: Boolean,
    ) : CookieBannerExceptionAction()

    data class UpdateCookieBannerExceptionStatus(
        val cookieBannerExceptionStatus: CookieBannerExceptionStatus,
    ) : CookieBannerExceptionAction()
}

/**
 * Reduces the cookie banner exception state from the current state and an action performed on it.
 *
 * @param state the current cookie banner item state
 * @param action the action to perform
 * @return the new cookie banner exception state
 */
private fun cookieBannerExceptionStateReducer(
    state: CookieBannerExceptionState,
    action: CookieBannerExceptionAction,
): CookieBannerExceptionState {
    return when (action) {
        is CookieBannerExceptionAction.ToggleCookieBannerException -> {
            state.copy(isCookieBannerToggleEnabled = action.isCookieBannerHandlingExceptionEnabled)
        }
        is CookieBannerExceptionAction.UpdateCookieBannerExceptionVisibility -> {
            state.copy(shouldShowCookieBannerItem = action.shouldShowCookieBannerItem)
        }
        is CookieBannerExceptionAction.UpdateCookieBannerExceptionStatus -> {
            state.copy(cookieBannerExceptionStatus = action.cookieBannerExceptionStatus)
        }
        is CookieBannerExceptionAction.CookieBannerDetected -> {
            state.copy(isCookieBannerDetected = action.isCookieBannerDetected)
        }
        CookieBannerExceptionAction.InitCookieBannerException -> {
            throw IllegalStateException(
                "You need to add CookieBannerExceptionMiddleware to your CookieBannerStore. ($action)",
            )
        }
    }
}
