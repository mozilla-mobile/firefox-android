/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics.clientdeduplication

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.fenix.GleanMetrics.Activation
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.components.metrics.MetricsUtils.getHashedIdentifier

class ClientDeduplicationPing(private val context: Context) {
    /**
     * Fills the metrics and triggers the 'clientDeduplication' ping.
     * This is a separate function to simplify unit-testing.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun triggerPing() {
        CoroutineScope(Dispatchers.IO).launch {
            val hashedId = getHashedIdentifier(context)
            if (hashedId != null) {
                // We have a valid, hashed Google Advertising ID.
                Activation.identifier.set(hashedId)
            }

            Pings.clientDeduplication.submit()
        }
    }

    /**
     * Trigger sending the `clientDeduplication` ping.
     */
    fun checkAndSend() {
        triggerPing()
    }
}
