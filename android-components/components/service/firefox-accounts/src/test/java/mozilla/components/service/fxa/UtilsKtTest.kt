/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import kotlinx.coroutines.test.runTest
import mozilla.appservices.fxaclient.FxaException
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsKtTest {
    @Test
    fun `handleFxaExceptions returns successful results`() = runTest {
        assertEquals(
            1,
            handleFxaExceptions(
                mock(),
                "test op",
                {
                    1
                },
            ),
        )
    }

    @Test
    fun `handleFxaExceptions returns null on throws`() = runTest {
        assertEquals(
            null,
            handleFxaExceptions(
                mock(),
                "test op",
                {
                    if (true) {
                        throw FxaException.Other("TestException")
                    } else {
                        2
                    }
                },
            ),
        )
    }
}
