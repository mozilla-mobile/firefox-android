/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics.clientdeduplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.fenix.GleanMetrics.ClientDeduplication
import org.mozilla.fenix.GleanMetrics.Pings

internal class ClientDeduplicationPingTest {
    @Test
    fun `The clientDeduplication ping is sent`() {
        // Record test data.
        ClientDeduplication.validAdvertisingId.set(true)

        // Instruct the ping API to validate the ping data.
        var validatorRun = false
        Pings.clientDeduplication.testBeforeNextSubmit { reason ->
            assertEquals(Pings.clientDeduplicationReasonCodes.active, reason)
            assertEquals(true, ClientDeduplication.validAdvertisingId.testGetValue())
            validatorRun = true
        }
        Pings.clientDeduplication.submit(Pings.clientDeduplicationReasonCodes.active)

        // Verify that the validator run.
        assertTrue(validatorRun)
    }
}
