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

package ua.com.radiokot.money.transfers.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.isSameDayAs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Immutable
class ViewDate(
    val localDate: LocalDate,
    val specificType: SpecificType?,
) {
    constructor(
        localDate: LocalDate,
        today: LocalDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
        yesterday: LocalDate = today.minus(1, DateTimeUnit.DAY),
    ) : this(
        localDate = localDate,
        specificType = when {
            localDate.isSameDayAs(today) ->
                SpecificType.Today

            localDate.isSameDayAs(yesterday) ->
                SpecificType.Yesterday

            else ->
                null
        }
    )

    constructor(
        localDateTime: LocalDateTime,
        today: LocalDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
        yesterday: LocalDate = today.minus(1, DateTimeUnit.DAY),
    ) : this(
        localDate = localDateTime.date,
        today = today,
        yesterday = yesterday
    )

    @Composable
    fun getText(): String =
        when (specificType) {
            SpecificType.Today ->
                "Today"

            SpecificType.Yesterday ->
                "Yesterday"

            null ->
                localDate.toString()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewDate) return false

        if (localDate != other.localDate) return false
        if (specificType != other.specificType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localDate.hashCode()
        result = 31 * result + (specificType?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ViewDate(localDate=$localDate, specificType=$specificType)"
    }

    enum class SpecificType {
        Today,
        Yesterday,
        ;
    }

    companion object {
        fun today() = ViewDate(
            localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        )
    }
}
