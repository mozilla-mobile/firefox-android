/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.metrics

import android.support.annotation.VisibleForTesting
import mozilla.components.service.glean.Dispatchers
import mozilla.components.service.glean.storages.StringListsStorageEngine
import mozilla.components.support.base.log.logger.Logger

/**
 * This implements the developer facing API for recording string list metrics.
 *
 * Instances of this class type are automatically generated by the parsers at build time,
 * allowing developers to record values that were previously registered in the metrics.yaml file.
 *
 * The string list API exposes the [add] and [set] methods, which take care of validating the input
 * data and making sure that limits are enforced.
 */
data class StringListMetricType(
    override val disabled: Boolean,
    override val category: String,
    override val lifetime: Lifetime,
    override val name: String,
    override val sendInPings: List<String>
) : CommonMetricData {

    override val defaultStorageDestinations: List<String> = listOf("metrics")

    private val logger = Logger("glean/StringListMetricType")

    /**
     * Appends a string value to one or more string list metric stores.  If the string exceeds the
     * maximum string length or if the list exceeds the maximum length it will be truncated.
     *
     * @param value This is a user defined string value. The maximum length of
     *              this string is [MAX_STRING_LENGTH].
     */
    fun add(value: String) {
        if (!shouldRecord(logger)) {
            return
        }

        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.launch {
            // Delegate storing the string to the storage engine.
            StringListsStorageEngine.add(
                metricData = this@StringListMetricType,
                value = value
            )
        }
    }

    /**
     * Sets a string list to one or more metric stores. If any string exceeds the maximum string
     * length or if the list exceeds the maximum length it will be truncated.
     *
     * @param value This is a user defined string list.
     */
    fun set(value: List<String>) {
        if (!shouldRecord(logger)) {
            return
        }

        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.launch {
            // Delegate storing the string list to the storage engine.
            StringListsStorageEngine.set(
                metricData = this@StringListMetricType,
                value = value
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
    fun testHasValue(pingName: String = getStorageNames().first()): Boolean {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return StringListsStorageEngine.getSnapshot(pingName, false)?.get(identifier) != null
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
    fun testGetValue(pingName: String = getStorageNames().first()): List<String> {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return StringListsStorageEngine.getSnapshot(pingName, false)!![identifier]!!
    }
}
