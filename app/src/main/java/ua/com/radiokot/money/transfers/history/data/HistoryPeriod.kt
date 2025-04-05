/* Copyright 2025 Oleg Koretsky

   This file is part of the 4Money,
   a budget tracking Android app.

   4Money is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   4Money is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with 4Money. If not, see <http://www.gnu.org/licenses/>.
*/

package ua.com.radiokot.money.transfers.history.data

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

sealed interface HistoryPeriod {

    val startTimeInclusive: Instant
    val endTimeExclusive: Instant

    fun getNext(): HistoryPeriod?
    fun getPrevious(): HistoryPeriod?

    operator fun contains(time: Instant): Boolean =
        time >= startTimeInclusive && time < endTimeExclusive

    class Day(
        private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
        val localDay: LocalDate = Clock.System.now().toLocalDateTime(timeZone).date,
    ) : HistoryPeriod {

        private val nextDay = localDay.plus(1, DateTimeUnit.DAY)

        override val startTimeInclusive: Instant =
            localDay.atStartOfDayIn(timeZone)

        override val endTimeExclusive: Instant =
            nextDay.atStartOfDayIn(timeZone)

        override fun getNext() = Day(
            timeZone = timeZone,
            localDay = nextDay,
        )

        override fun getPrevious() = Day(
            timeZone = timeZone,
            localDay = localDay.minus(1, DateTimeUnit.DAY),
        )
    }

    class Month(
        private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
        val localMonth: LocalDate = Clock.System.now().toLocalDateTime(timeZone).date,
    ) : HistoryPeriod {

        private val firstDay = localMonth.minus(localMonth.dayOfMonth - 1, DateTimeUnit.DAY)
        private val fistDayOfNextMonth = firstDay.plus(1, DateTimeUnit.MONTH)

        override val startTimeInclusive: Instant =
            firstDay.atStartOfDayIn(timeZone)

        override val endTimeExclusive: Instant =
            fistDayOfNextMonth.atStartOfDayIn(timeZone)

        override fun getNext() = Month(
            timeZone = timeZone,
            localMonth = fistDayOfNextMonth,
        )

        override fun getPrevious() = Month(
            timeZone = timeZone,
            localMonth = firstDay.minus(1, DateTimeUnit.MONTH),
        )
    }

    object Since70th : HistoryPeriod {
        override val startTimeInclusive: Instant =
            Instant.fromEpochMilliseconds(0)

        override val endTimeExclusive: Instant =
            Instant.DISTANT_FUTURE

        override fun getNext(): HistoryPeriod? =
            null

        override fun getPrevious(): HistoryPeriod? =
            null
    }
}
