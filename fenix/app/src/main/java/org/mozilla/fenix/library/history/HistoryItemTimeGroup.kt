/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import org.mozilla.fenix.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class HistoryItemTimeGroup {
    Today, Yesterday, ThisWeek, ThisMonth, Older;

    fun humanReadable(context: Context): String = when (this) {
        Today -> context.getString(R.string.history_today)
        Yesterday -> context.getString(R.string.history_yesterday)
        ThisWeek -> context.getString(R.string.history_7_days)
        ThisMonth -> context.getString(R.string.history_30_days)
        Older -> context.getString(R.string.history_older)
    }

    companion object {
        private val today = LocalDate.now()
        private val yesterday = today.minusDays(1)
        private val weekAgo = today.minusWeeks(1)
        private val monthAgo = today.minusMonths(1)
        private val lastWeekRange = weekAgo..yesterday.minusDays(1)
        private val lastMonthRange = monthAgo..weekAgo.minusDays(1)

        internal fun timeGroupForTimestamp(timestamp: Long): HistoryItemTimeGroup {
            val localDate = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toLocalDate()
            return when {
                localDate >= today -> Today // all future time is considered today
                localDate == yesterday -> Yesterday
                localDate in lastWeekRange -> ThisWeek
                localDate in lastMonthRange -> ThisMonth
                else -> Older
            }
        }
    }
}
