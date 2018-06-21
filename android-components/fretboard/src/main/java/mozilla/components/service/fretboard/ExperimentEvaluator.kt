/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard

import android.content.Context
import android.os.Build
import android.text.TextUtils
import java.util.Locale
import java.util.zip.CRC32

internal class ExperimentEvaluator(private val regionProvider: RegionProvider? = null) {
    fun evaluate(
        context: Context,
        experimentDescriptor: ExperimentDescriptor,
        experiments: List<Experiment>,
        userBucket: Int = getUserBucket(context)
    ): Experiment? {
        val experiment = getExperiment(experimentDescriptor, experiments) ?: return null
        return if (isInBucket(userBucket, experiment) && matches(context, experiment)) {
            experiment
        } else {
            null
        }
    }

    fun getExperiment(descriptor: ExperimentDescriptor, experiments: List<Experiment>): Experiment? {
        return experiments.firstOrNull { it.id == descriptor.id }
    }

    private fun matches(context: Context, experiment: Experiment): Boolean {
        if (experiment.match != null) {
            val region = regionProvider?.getRegion()
            val matchesRegion = !(region != null &&
                                        experiment.match.regions != null &&
                                        experiment.match.regions.isNotEmpty() &&
                                        experiment.match.regions.none { it == region })
            val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            return matchesRegion &&
                matchesExperiment(experiment.match.appId, context.packageName) &&
                matchesExperiment(experiment.match.language, Locale.getDefault().isO3Language) &&
                matchesExperiment(experiment.match.country, Locale.getDefault().isO3Country) &&
                matchesExperiment(experiment.match.version, appVersion) &&
                matchesExperiment(experiment.match.manufacturer, Build.MANUFACTURER) &&
                matchesExperiment(experiment.match.device, Build.DEVICE)
        }
        return true
    }

    private fun matchesExperiment(experimentValue: String?, deviceValue: String): Boolean {
        return !(experimentValue != null &&
            !TextUtils.isEmpty(experimentValue) &&
            !deviceValue.matches(experimentValue.toRegex()))
    }

    private fun isInBucket(userBucket: Int, experiment: Experiment): Boolean {
        return !(experiment.bucket?.min == null ||
            userBucket < experiment.bucket.min ||
            experiment.bucket.max == null ||
            userBucket >= experiment.bucket.max)
    }

    private fun getUserBucket(context: Context): Int {
        val uuid = DeviceUuidFactory(context).uuid
        val crc = CRC32()
        crc.update(uuid.toByteArray())
        val checksum = crc.value
        return (checksum % MAX_BUCKET).toInt()
    }

    companion object {
        private const val MAX_BUCKET = 100L
    }
}
