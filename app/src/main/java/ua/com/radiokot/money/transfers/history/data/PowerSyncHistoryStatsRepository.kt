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

import com.powersync.db.Queries
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class PowerSyncHistoryStatsRepository(
    private val database: Queries,
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
                    period.startInclusive.toString(),
                    period.endExclusive.toString(),
                ),
                mapper = { sqlCursor ->
                    // Sum subcategories into parent.
                    val categoryId = sqlCursor.getStringOptional("categoryParentId")
                        ?: sqlCursor.getString("transferCounterpartyId")
                    // Category ID to string amount not to store bunch of BigIntegers.
                    categoryId to sqlCursor.getString("transferAmount").trim()
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

private const val DATETIME =
    "datetime(transfers.time) AS datetime"

private const val DATETIME_IN_PERIOD =
    "datetime >= datetime(?) AND datetime < datetime(?)"

private const val SELECT_FOR_INCOME_CATEGORIES =
    "SELECT " +
            "transfers.source_id as 'transferCounterpartyId', " +
            "transfers.source_amount as 'transferAmount', " +
            "categories.parent_category_id as 'categoryParentId', " +
            "$DATETIME " +
            "FROM transfers, categories " +
            "WHERE transfers.source_id in " +
            "(SELECT categories.id FROM categories WHERE categories.is_income = 1) " +
            "AND transfers.source_id = categories.id " +
            "AND $DATETIME_IN_PERIOD"

private const val SELECT_FOR_EXPENSE_CATEGORIES =
    "SELECT " +
            "transfers.destination_id as 'transferCounterpartyId', " +
            "transfers.destination_amount as 'transferAmount', " +
            "categories.parent_category_id as 'categoryParentId', " +
            "$DATETIME " +
            "FROM transfers, categories " +
            "WHERE transfers.destination_id in " +
            "(SELECT categories.id FROM categories WHERE categories.is_income = 0) " +
            "AND transfers.destination_id = categories.id " +
            "AND $DATETIME_IN_PERIOD"
