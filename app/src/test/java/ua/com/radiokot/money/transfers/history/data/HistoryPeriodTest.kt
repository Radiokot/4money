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

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert
import org.junit.Test

class HistoryPeriodTest {

    @Test
    fun month() {

        // Previous is the previous month.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 1, 22),
        ).apply {
            Assert.assertEquals(
                12,
                getPrevious().localMonth.monthNumber,
            )
            Assert.assertEquals(
                2023,
                getPrevious().localMonth.year,
            )
        }

        // Next is the next month.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 12, 22),
        ).apply {
            Assert.assertEquals(
                1,
                getNext().localMonth.monthNumber,
            )
            Assert.assertEquals(
                2025,
                getNext().localMonth.year,
            )
        }

        // Other months are not within it.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 2, 22),
        ).apply {
            Assert.assertFalse(
                LocalDateTime.parse(
                    "2024-01-31T23:59:59",
                    LocalDateTime.Formats.ISO,
                ) in this
            )
            Assert.assertFalse(
                LocalDateTime.parse(
                    "2024-03-01T00:00:00.00",
                    LocalDateTime.Formats.ISO,
                ) in this
            )
        }

        // The start and the end are within it.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 2, 22),
        ).apply {
            Assert.assertTrue(
                LocalDateTime.parse(
                    "2024-02-01T00:00:00.00",
                    LocalDateTime.Formats.ISO,
                ) in this
            )
            Assert.assertTrue(
                LocalDateTime.parse(
                    "2024-02-29T23:59:59.999",
                    LocalDateTime.Formats.ISO,
                ) in this
            )
        }
    }

    @Test
    fun day() {

        // Previous is the previous day.
        HistoryPeriod.Day(
            localDay = LocalDate(2024, 3, 1),
        ).apply {
            Assert.assertEquals(
                LocalDate(2024, 2, 29),
                getPrevious().localDay,
            )
        }

        // Next is the next day.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
        ).apply {
            Assert.assertEquals(
                LocalDate(2025, 2, 23),
                getNext().localDay,
            )
        }

        // Other days are not within it.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
        ).apply {
            Assert.assertFalse(
                LocalDateTime.parse(
                    "2025-02-23T00:00:00.00",
                    LocalDateTime.Formats.ISO
                ) in this
            )
            Assert.assertFalse(
                LocalDateTime.parse(
                    "2025-02-21T23:59:59.999",
                    LocalDateTime.Formats.ISO
                ) in this
            )
        }

        // The start and the end are within it.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
        ).apply {
            Assert.assertTrue(
                LocalDateTime.parse(
                    "2025-02-22T00:00:00.00",
                    LocalDateTime.Formats.ISO
                ) in this
            )
            Assert.assertTrue(
                LocalDateTime.parse(
                    "2025-02-22T23:59:59.999",
                    LocalDateTime.Formats.ISO
                ) in this
            )
        }
    }
}
