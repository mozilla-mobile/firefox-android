/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.state.helpers

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.State

/**
 * A Middleware for detecting changes to a state property, and offering a callback that captures the action that changed
 * the property, the property before the change, and the property after the change.
 *
 * For example, this can be useful for debugging:
 * ```
 * val selectedTabChangedMiddleware = ChangeDetectionMiddleware<BrowserState, BrowserAction>(
 *     val selector = { it.selectedTabId }
 *     val onChange = { actionThatCausedResult, preResult, postResult ->
 *         logger.debug("$actionThatCausedResult changed selectedTabId from $preResult to $postResult")
 *     }
 * ```
 *
 * @param S The state type of the [Store] this middleware is attached to.
 * @param A The action subtype of the [Store] this middleware is attached to.
 * @param T The type of the property to watch for changes in.
 * @param selector A function to map from the State to the property that is being inspected.
 * @param onChange A callback to react to changes to the property defined by [selector].
 */
class ChangeDetectionMiddleware<T, S : State, A : Action>(
    private val selector: (S) -> T,
    private val onChange: (A, pre: T, post: T) -> Unit,
) : Middleware<S, A> {
    override fun invoke(context: MiddlewareContext<S, A>, next: (A) -> Unit, action: A) {
        val pre = selector(context.store.state)
        next(action)
        val post = selector(context.store.state)
        if (pre != post) {
            onChange(action, pre, post)
        }
    }
}
