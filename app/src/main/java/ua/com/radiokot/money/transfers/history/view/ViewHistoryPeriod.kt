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

package ua.com.radiokot.money.transfers.history.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.view.ViewDate

@Immutable
sealed interface ViewHistoryPeriod {

    @Composable
    fun getText(): String

    class Day(
        val day: ViewDate,
    ) : ViewHistoryPeriod {

        @Composable
        override fun getText(): String =
            day.getText()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Day) return false

            if (day != other.day) return false

            return true
        }

        override fun hashCode(): Int {
            return day.hashCode()
        }
    }

    class Month(
        val month: LocalDate,
    ) : ViewHistoryPeriod {

        @Composable
        override fun getText(): String {

            val monthYearFormat = remember {
                LocalDate.Format {
                    monthName(MonthNames.ENGLISH_FULL)
                    char(' ')
                    year()
                }
            }

            return monthYearFormat.format(month)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Month) return false

            if (month != other.month) return false

            return true
        }

        override fun hashCode(): Int {
            return month.hashCode()
        }
    }

    object EntireTime : ViewHistoryPeriod {

        @Composable
        override fun getText(): String =
            "The entire time"
    }

    companion object {

        fun fromHistoryPeriod(
            historyPeriod: HistoryPeriod,
        ): ViewHistoryPeriod = when (historyPeriod) {

            is HistoryPeriod.Day ->
                Day(
                    day = ViewDate(
                        localDate = historyPeriod.localDay,
                    )
                )

            is HistoryPeriod.Month ->
                Month(
                    month = historyPeriod.localMonth,
                )

            HistoryPeriod.Since70th ->
                EntireTime
        }
    }
}
