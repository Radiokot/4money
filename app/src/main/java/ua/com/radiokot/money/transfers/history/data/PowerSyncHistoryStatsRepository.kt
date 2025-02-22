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

import com.powersync.PowerSyncDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class PowerSyncHistoryStatsRepository(
    private val database: PowerSyncDatabase,
) : HistoryStatsRepository {

    override fun getCategoryStatsFlow(
        isIncome: Boolean,
        period: HistoryPeriod,
    ): Flow<Map<String, BigInteger>> =
        database
            .watch(
                sql =
                if (isIncome)
                    SELECT_FOR_INCOME_CATEGORIES
                else
                    SELECT_FOR_EXPENSE_CATEGORIES,
                parameters = listOf(
                    period.startTimeInclusive.toString(),
                    period.endTimeExclusive.toString(),
                ),
                mapper = { sqlCursor ->
                    // Sum subcategories into parent.
                    val categoryId = sqlCursor.getString(2)
                        ?: sqlCursor.getString(0)!!
                    // Category ID to string amount not to store bunch of BigIntegers.
                    categoryId to sqlCursor.getString(1)!!.trim()
                }
            )
            .map { transfersToSum ->
                buildMap<String, BigInteger> {
                    transfersToSum.forEach { (categoryId, stringAmount) ->
                        set(
                            categoryId,
                            getOrDefault(categoryId, BigInteger.ZERO) + BigInteger(stringAmount)
                        )
                    }
                }
            }
            .flowOn(Dispatchers.Default)
}

private const val PARSED_TIME =
    "datetime(transfers.time, 'subsecond') AS parsed_time"

private const val PARSED_TIME_IN_PERIOD =
    "parsed_time >= datetime(?, 'subsecond') AND parsed_time < datetime(?, 'subsecond')"

private const val SELECT_FOR_INCOME_CATEGORIES =
    "SELECT transfers.source_id, transfers.source_amount, " +
            "categories.parent_category_id, " +
            "$PARSED_TIME " +
            "FROM transfers, categories " +
            "WHERE transfers.source_id in " +
            "(SELECT categories.id FROM categories WHERE categories.is_income = 1) " +
            "AND transfers.source_id = categories.id " +
            "AND $PARSED_TIME_IN_PERIOD"

private const val SELECT_FOR_EXPENSE_CATEGORIES =
    "SELECT transfers.destination_id, transfers.destination_amount, " +
            "categories.parent_category_id, " +
            "$PARSED_TIME " +
            "FROM transfers, categories " +
            "WHERE transfers.destination_id in " +
            "(SELECT categories.id FROM categories WHERE categories.is_income = 0) " +
            "AND transfers.destination_id = categories.id " +
            "AND $PARSED_TIME_IN_PERIOD"
