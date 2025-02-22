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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.DateTimeComponents
import org.junit.Assert
import org.junit.Test

class HistoryPeriodTest {

    @Test
    fun month() {

        // Other months are not within it.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 2, 22),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertFalse(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2024-01-31T23:59:59.999+02:00")
                    .toInstantUsingOffset() in this
            )
            Assert.assertFalse(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2024-03-01T00:00:00.00+02:00")
                    .toInstantUsingOffset() in this
            )
        }

        // The start and the end are within it.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 2, 22),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertTrue(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2024-02-01T00:00:00.00+02:00")
                    .toInstantUsingOffset() in this
            )
            Assert.assertTrue(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2024-02-29T23:59:59.999+02:00")
                    .toInstantUsingOffset() in this
            )
        }

        // Switching from daylight saving time in October.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 10, 22),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertEquals(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2024-09-30T21:00:00Z")
                    .toInstantUsingOffset()
                    .toString(),
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2024-10-31T22:00:00Z") // -1h difference
                    .toInstantUsingOffset()
                    .toString(),
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "31d 1h", // Objectively October here is 1h longer.
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }

        // A long February.
        HistoryPeriod.Month(
            localMonth = LocalDate(2024, 2, 22),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertEquals(
                LocalDate(2024, 2, 1)
                    .atStartOfDayIn(TimeZone.of("EET"))
                    .toString(),
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                LocalDate(2024, 3, 1)
                    .atStartOfDayIn(TimeZone.of("EET"))
                    .toString(),
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "29d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }

        // A regular February.
        HistoryPeriod.Month(
            localMonth = LocalDate(2025, 2, 22),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertEquals(
                LocalDate(2025, 2, 1)
                    .atStartOfDayIn(TimeZone.of("EET"))
                    .toString(),
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                LocalDate(2025, 3, 1)
                    .atStartOfDayIn(TimeZone.of("EET"))
                    .toString(),
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "28d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }
    }

    @Test
    fun day() {

        // Other days are not within it.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
            timeZone = TimeZone.of("GMT+2")
        ).apply {
            Assert.assertFalse(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2025-02-23T00:00:00.00+02:00")
                    .toInstantUsingOffset() in this
            )
            Assert.assertFalse(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2025-02-21T23:59:59.999+02:00")
                    .toInstantUsingOffset() in this
            )
        }

        // The start and the end are within it.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
            timeZone = TimeZone.of("GMT+2")
        ).apply {
            Assert.assertTrue(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2025-02-22T00:00:00.00+02:00")
                    .toInstantUsingOffset() in this
            )
            Assert.assertTrue(
                DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                    .parse("2025-02-22T23:59:59.999+02:00")
                    .toInstantUsingOffset() in this
            )
        }

        // 3h EET-UTC difference in Summer.
        HistoryPeriod.Day(
            localDay = LocalDate(2024, 6, 6),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertEquals(
                "2024-06-05T21:00:00Z",
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                "2024-06-06T21:00:00Z",
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "1d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }

        // Day with a leap second is still one day.
        HistoryPeriod.Day(
            localDay = LocalDate(2017, 1, 1),
            timeZone = TimeZone.of("EET")
        ).apply {
            Assert.assertEquals(
                "2016-12-31T22:00:00Z",
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                "2017-01-01T22:00:00Z",
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "1d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }

        // Feb 29th.
        HistoryPeriod.Day(
            localDay = LocalDate(2024, 3, 1),
            timeZone = TimeZone.of("GMT+2")
        ).apply {
            Assert.assertEquals(
                "2024-02-29T22:00:00Z",
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                "2024-03-01T22:00:00Z",
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "1d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }

        // Everything's obvious in UTC.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
            timeZone = TimeZone.of("UTC")
        ).apply {
            Assert.assertEquals(
                "2025-02-22T00:00:00Z",
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                "2025-02-23T00:00:00Z",
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "1d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }

        // Same local day – different day in UTC.
        HistoryPeriod.Day(
            localDay = LocalDate(2025, 2, 22),
            timeZone = TimeZone.of("GMT+2")
        ).apply {
            Assert.assertEquals(
                "2025-02-21T22:00:00Z",
                startTimeInclusive.toString()
            )
            Assert.assertEquals(
                "2025-02-22T22:00:00Z",
                endTimeExclusive.toString()
            )
            Assert.assertEquals(
                "1d",
                (endTimeExclusive - startTimeInclusive).toString()
            )
        }
    }
}
