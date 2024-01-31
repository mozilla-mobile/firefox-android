/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.state.helpers

import mozilla.components.lib.state.Store
import mozilla.components.lib.state.TestAction
import mozilla.components.lib.state.TestState
import mozilla.components.lib.state.reducer
import mozilla.components.support.test.ext.joinBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeDetectionMiddlewareTest {
    @Test
    fun `GIVEN state WHEN action changes that state THEN middleware is invoked`() {
        var capturedAction: TestAction? = null
        var preCount = 0
        var postCount = 0
        val countChangedMiddleware = ChangeDetectionMiddleware<Int, TestState, TestAction>(
            selector = { it.counter },
            onChange = { action, pre, post ->
                capturedAction = action
                preCount = pre
                postCount = post
            },
        )
        val store = Store(
            TestState(counter = preCount),
            ::reducer,
            middleware = listOf(countChangedMiddleware),
        )

        store.dispatch(TestAction.IncrementAction).joinBlocking()
        assertTrue(capturedAction is TestAction.IncrementAction)
        assertEquals(0, preCount)
        assertEquals(1, postCount)

        store.dispatch(TestAction.SetValueAction(100)).joinBlocking()
        assertTrue(capturedAction is TestAction.SetValueAction)
        assertEquals(1, preCount)
        assertEquals(100, postCount)
    }
}
