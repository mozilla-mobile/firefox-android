/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.private

import androidx.annotation.VisibleForTesting
import mozilla.components.service.glean.Dispatchers
import mozilla.components.service.glean.storages.TimingDistributionData
import mozilla.components.service.glean.storages.TimingDistributionsStorageEngine
import mozilla.components.service.glean.timing.GleanTimerId
import mozilla.components.service.glean.timing.TimingManager
import mozilla.components.support.base.log.logger.Logger

/**
 * This implements the developer facing API for recording timing distribution metrics.
 *
 * Instances of this class type are automatically generated by the parsers at build time,
 * allowing developers to record values that were previously registered in the metrics.yaml file.
 */
data class TimingDistributionMetricType(
    override val disabled: Boolean,
    override val category: String,
    override val lifetime: Lifetime,
    override val name: String,
    override val sendInPings: List<String>,
    val timeUnit: TimeUnit
) : CommonMetricData, HistogramBase {

    private val logger = Logger("glean/TimingDistributionMetricType")

    /**
     * Start tracking time for the provided metric and [GleanTimerId]. This
     * records an error if it’s already tracking time (i.e. start was already
     * called with no corresponding [stopAndAccumulate]): in that case the original
     * start time will be preserved.
     */
    fun start(): GleanTimerId? {
        if (!shouldRecord(logger)) {
            return null
        }

        return TimingManager.start(this)
    }

    /**
     * Stop tracking time for the provided metric and associated timer id. Add a
     * count to the corresponding bucket in the timing distribution.
     * This will record an error if no [start] was called.
     *
     * @param timerId The [GleanTimerId] to associate with this timing.  This allows
     * for concurrent timing of events associated with different ids to the
     * same timespan metric.
     */
    fun stopAndAccumulate(timerId: GleanTimerId?) {
        if (!shouldRecord(logger) || timerId == null) {
            return
        }

        TimingManager.stop(this, timerId)?.let { elapsedNanos ->
            @Suppress("EXPERIMENTAL_API_USAGE")
            Dispatchers.API.launch {
                // Delegate storing the string to the storage engine.
                TimingDistributionsStorageEngine.accumulate(
                    metricData = this@TimingDistributionMetricType,
                    sample = elapsedNanos
                )
            }
        }
    }

    /**
     * Abort a previous [start] call. No error is recorded if no [start] was called.
     *
     * @param timerId The [GleanTimerId] to associate with this timing. This allows
     * for concurrent timing of events associated with different ids to the
     * same timing distribution metric.
     */
    fun cancel(timerId: GleanTimerId?) {
        if (!shouldRecord(logger) || timerId == null) {
            return
        }

        TimingManager.cancel(this, timerId)
    }

    override fun accumulateSamples(samples: LongArray) {
        if (!shouldRecord(logger)) {
            return
        }

        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.launch {
            TimingDistributionsStorageEngine.accumulateSamples(
                metricData = this@TimingDistributionMetricType,
                samples = samples,
                timeUnit = timeUnit
            )
        }
    }

    /**
     * Tests whether a value is stored for the metric for testing purposes only. This function will
     * attempt to await the last task (if any) writing to the the metric's storage engine before
     * returning a value.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.  Defaults
     *                 to the either the first value in [defaultStorageDestinations] or the first
     *                 value in [sendInPings]
     * @return true if metric value exists, otherwise false
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun testHasValue(pingName: String = sendInPings.first()): Boolean {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return TimingDistributionsStorageEngine.getSnapshot(pingName, false)?.get(identifier) != null
    }

    /**
     * Returns the stored value for testing purposes only. This function will attempt to await the
     * last task (if any) writing to the the metric's storage engine before returning a value.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.  Defaults
     *                 to the either the first value in [defaultStorageDestinations] or the first
     *                 value in [sendInPings]
     * @return value of the stored metric
     * @throws [NullPointerException] if no value is stored
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun testGetValue(pingName: String = sendInPings.first()): TimingDistributionData {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return TimingDistributionsStorageEngine.getSnapshot(pingName, false)!![identifier]!!
    }
}
