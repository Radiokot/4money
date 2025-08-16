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

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
@OptIn(ExperimentalTime::class)
sealed interface HistoryPeriod {

    val startInclusive: LocalDateTime
    val endExclusive: LocalDateTime

    fun getNext(): HistoryPeriod?
    fun getPrevious(): HistoryPeriod?

    operator fun contains(dateTime: LocalDateTime): Boolean =
        dateTime >= startInclusive && dateTime < endExclusive

    @Serializable
    class Day(
        val localDay: LocalDate = Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
    ) : HistoryPeriod {

        private val nextDay = localDay.plus(1, DateTimeUnit.DAY)

        override val startInclusive: LocalDateTime =
            LocalDateTime(
                date = localDay,
                time = LocalTime.fromSecondOfDay(0),
            )

        override val endExclusive: LocalDateTime =
            LocalDateTime(
                date = localDay.plus(1, DateTimeUnit.DAY),
                time = LocalTime.fromSecondOfDay(0),
            )

        override fun getNext() = Day(
            localDay = nextDay,
        )

        override fun getPrevious() = Day(
            localDay = localDay.minus(1, DateTimeUnit.DAY),
        )
    }

    @Serializable
    class Month(
        val localMonth: LocalDate = Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
    ) : HistoryPeriod {

        private val firstDay = localMonth.minus(localMonth.day - 1, DateTimeUnit.DAY)
        private val fistDayOfNextMonth = firstDay.plus(1, DateTimeUnit.MONTH)

        override val startInclusive: LocalDateTime =
            LocalDateTime(
                date = firstDay,
                time = LocalTime.fromSecondOfDay(0),
            )

        override val endExclusive: LocalDateTime =
            LocalDateTime(
                date = fistDayOfNextMonth,
                time = LocalTime.fromSecondOfDay(0),
            )

        override fun getNext() = Month(
            localMonth = fistDayOfNextMonth,
        )

        override fun getPrevious() = Month(
            localMonth = firstDay.minus(1, DateTimeUnit.MONTH),
        )
    }

    @Serializable
    object Since70th : HistoryPeriod {

        override val startInclusive: LocalDateTime =
            LocalDateTime(
                date = LocalDate.fromEpochDays(0),
                time = LocalTime.fromSecondOfDay(0),
            )

        override val endExclusive: LocalDateTime =
            LocalDateTime(
                date = LocalDate(9999, 12, 31),
                time = LocalTime.fromSecondOfDay(0),
            )

        override fun getNext(): HistoryPeriod? =
            null

        override fun getPrevious(): HistoryPeriod? =
            null
    }
}
