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
import ua.com.radiokot.money.powersync.DbSchema
import java.math.BigInteger

class PowerSyncHistoryStatsRepository(
    private val database: Queries,
) : HistoryStatsRepository {

    override fun getCategoryAmountsFlow(
        isIncome: Boolean,
        period: HistoryPeriod,
    ): Flow<AmountsByCategoryId> =
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
                    val categoryId =
                        sqlCursor.getStringOptional(DbSchema.CATEGORY_SELECTED_PARENT_ID)
                            ?: sqlCursor.getString(TRANSFER_SELECTED_COUNTERPARTY_ID)
                    // Category ID to string amount not to store bunch of BigIntegers.
                    categoryId to sqlCursor.getString(TRANSFER_SELECTED_AMOUNT).trim()
                }
            )
            .map { transfersToSum ->
                val amountsByCategoryId = mutableMapOf<String, BigInteger>()

                transfersToSum.forEach { (categoryId, stringAmount) ->
                    amountsByCategoryId.compute(categoryId) { _, total ->
                        (total ?: BigInteger.ZERO) + BigInteger(stringAmount)
                    }
                }

                amountsByCategoryId
            }
            .flowOn(Dispatchers.Default)

    override fun getCategoryDailyAmountsFlow(
        isIncome: Boolean,
        period: HistoryPeriod,
    ): Flow<DailyAmountsByCategoryId> =
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
                    val categoryId =
                        sqlCursor.getStringOptional(DbSchema.CATEGORY_SELECTED_PARENT_ID)
                            ?: sqlCursor.getString(TRANSFER_SELECTED_COUNTERPARTY_ID)
                    Triple(
                        categoryId,
                        sqlCursor
                            .getString(DbSchema.TRANSFER_SELECTED_DATETIME)
                            .substring(0, 10),
                        BigInteger(sqlCursor.getString(TRANSFER_SELECTED_AMOUNT)),
                    )
                }
            )
            .map { transfersInPeriod ->
                val dailyAmountsByCategoryId =
                    mutableMapOf<String, MutableMap<String, BigInteger>>()

                transfersInPeriod.forEach { (categoryId, transferDayString, transferAmount) ->
                    dailyAmountsByCategoryId
                        .getOrPut(categoryId, ::mutableMapOf)
                        .compute(transferDayString) { _, dailyTotal ->
                            (dailyTotal ?: BigInteger.ZERO) + transferAmount
                        }
                }

                dailyAmountsByCategoryId
            }
            .flowOn(Dispatchers.Default)
}

private const val TRANSFER_DATETIME_IN_PERIOD =
    "${DbSchema.TRANSFER_SELECTED_DATETIME} >= datetime(?) " +
            "AND ${DbSchema.TRANSFER_SELECTED_DATETIME} < datetime(?)"

private const val TRANSFER_SELECTED_COUNTERPARTY_ID = "transferCounterpartyId"
private const val TRANSFER_SELECTED_AMOUNT = "transferAmount"

private const val SELECT_FOR_INCOME_CATEGORIES =
    "SELECT " +
            "${DbSchema.TRANSFERS_TABLE}.${DbSchema.TRANSFER_SOURCE_ID} as $TRANSFER_SELECTED_COUNTERPARTY_ID, " +
            "${DbSchema.TRANSFERS_TABLE}.${DbSchema.TRANSFER_SOURCE_AMOUNT} as $TRANSFER_SELECTED_AMOUNT, " +
            "${DbSchema.CATEGORIES_TABLE}.${DbSchema.CATEGORY_PARENT_ID} as ${DbSchema.CATEGORY_SELECTED_PARENT_ID}, " +
            "${DbSchema.TRANSFER_TIME_AS_DATETIME} " +
            "FROM ${DbSchema.TRANSFERS_TABLE}, ${DbSchema.CATEGORIES_TABLE} " +
            "WHERE $TRANSFER_SELECTED_COUNTERPARTY_ID in " +
            "(SELECT ${DbSchema.ID} FROM ${DbSchema.CATEGORIES_TABLE} WHERE ${DbSchema.CATEGORY_IS_INCOME} = 1) " +
            "AND $TRANSFER_SELECTED_COUNTERPARTY_ID = ${DbSchema.CATEGORIES_TABLE}.${DbSchema.ID} " +
            "AND $TRANSFER_DATETIME_IN_PERIOD"

private const val SELECT_FOR_EXPENSE_CATEGORIES =
    "SELECT " +
            "${DbSchema.TRANSFERS_TABLE}.${DbSchema.TRANSFER_DESTINATION_ID} as $TRANSFER_SELECTED_COUNTERPARTY_ID, " +
            "${DbSchema.TRANSFERS_TABLE}.${DbSchema.TRANSFER_DESTINATION_AMOUNT} as $TRANSFER_SELECTED_AMOUNT, " +
            "${DbSchema.CATEGORIES_TABLE}.${DbSchema.CATEGORY_PARENT_ID} as ${DbSchema.CATEGORY_SELECTED_PARENT_ID}, " +
            "${DbSchema.TRANSFER_TIME_AS_DATETIME} " +
            "FROM ${DbSchema.TRANSFERS_TABLE}, ${DbSchema.CATEGORIES_TABLE} " +
            "WHERE $TRANSFER_SELECTED_COUNTERPARTY_ID in " +
            "(SELECT ${DbSchema.ID} FROM ${DbSchema.CATEGORIES_TABLE} WHERE ${DbSchema.CATEGORY_IS_INCOME} = 0) " +
            "AND $TRANSFER_SELECTED_COUNTERPARTY_ID = ${DbSchema.CATEGORIES_TABLE}.${DbSchema.ID} " +
            "AND $TRANSFER_DATETIME_IN_PERIOD"
