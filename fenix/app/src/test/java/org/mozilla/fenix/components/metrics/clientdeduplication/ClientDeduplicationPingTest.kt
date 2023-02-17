/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics.clientdeduplication

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

internal class ClientDeduplicationPingTest {
    @Test
    fun `checkAndSend() triggers the ping`() {
        val mockCdp = spyk(ClientDeduplicationPing(mockk()), recordPrivateCalls = true)

        mockCdp.checkAndSend()

        verify(exactly = 1) { mockCdp.triggerPing() }
    }
}
