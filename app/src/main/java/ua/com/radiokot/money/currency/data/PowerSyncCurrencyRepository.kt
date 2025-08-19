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

package ua.com.radiokot.money.currency.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import ua.com.radiokot.money.powersync.DbSchema
import ua.com.radiokot.money.powersync.DbSchema.joinToSqlSet
import ua.com.radiokot.money.powersync.DbSchema.toDbDayString
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

class PowerSyncCurrencyRepository(
    private val database: PowerSyncDatabase,
) : CurrencyRepository {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val currenciesSharedFlow =
        database
            .watch(
                sql = SELECT_CURRENCIES,
                mapper = DbSchema::toCurrency,
            )
            .flowOn(Dispatchers.Default)
            .shareIn(coroutineScope, SharingStarted.Lazily, replay = 1)

    override suspend fun getCurrencies(): List<Currency> =
        currenciesSharedFlow.first()

    override fun getCurrenciesFlow(): Flow<List<Currency>> =
        currenciesSharedFlow

    override suspend fun getCurrencyByCode(code: String): Currency? =
        database.getOptional(
            sql = SELECT_CURRENCY_BY_CODE,
            parameters = listOf(code),
            mapper = DbSchema::toCurrency,
        )

    override suspend fun getLatestPrices(
        currencyCodes: Iterable<String>,
    ): CurrencyPairMap = withContext(Dispatchers.Default) {

        database
            .getAll(
                sql = SELECT_LATEST_PRICES +
                        " AND ${DbSchema.DAILY_PRICE_SELECTED_BASE_CODE} IN " +
                        currencyCodes.joinToSqlSet(),
                mapper = DbSchema::toPricePair,
            )
            .let { pricePairs ->
                CurrencyPairMap(
                    quoteCode = "USD",
                    decimalPriceByBaseCode = pricePairs.toMap(),
                )
            }
    }

    override suspend fun getDailyPrices(
        period: HistoryPeriod,
        currencyCodes: Iterable<String>,
    ): Map<String, CurrencyPairMap> = withContext(Dispatchers.Default) {

        val pairMapsByDay = mutableMapOf<String, CurrencyPairMap>()

        database
            .getAll(
                sql = SELECT_DAILY_PRICES +
                        " AND ${DbSchema.DAILY_PRICE_SELECTED_BASE_CODE} IN " +
                        currencyCodes.joinToSqlSet(),
                parameters = listOf(
                    period.startInclusive.toDbDayString(),
                    period.endExclusive.toDbDayString(),
                ),
                mapper = { sqlCursor ->
                    val dayString = sqlCursor
                        .getString(DbSchema.DAILY_PRICE_SELECTED_DAY_STRING)

                    pairMapsByDay.getOrPut(dayString) {
                        CurrencyPairMap(
                            quoteCode = "USD",
                        )
                    } += DbSchema.toPricePair(sqlCursor)
                }
            )

        return@withContext pairMapsByDay
    }
}

private const val SELECT_CURRENCIES =
    "SELECT ${DbSchema.CURRENCY_SELECT_COLUMNS} " +
            "FROM ${DbSchema.CURRENCIES_TABLE}"

private const val SELECT_CURRENCY_BY_CODE =
    "$SELECT_CURRENCIES " +
            "WHERE ${DbSchema.CURRENCY_SELECTED_CODE} = ? " +
            "ORDER BY ${DbSchema.CURRENCY_SELECTED_ID} " +
            "LIMIT 1"

private const val SELECT_LATEST_PRICES =
    "SELECT ${DbSchema.DAILY_PRICE_ID_AS_BASE_CODE}, " +
            "${DbSchema.DAILY_PRICES_TABLE}.${DbSchema.DAILY_PRICE_PRICE} as ${DbSchema.DAILY_PRICE_SELECTED_PRICE} " +
            "FROM ${DbSchema.DAILY_PRICES_TABLE} " +
            "WHERE ${DbSchema.DAILY_PRICE_DAY_SUBSTRING} = " +
            "(SELECT MAX(${DbSchema.DAILY_PRICE_DAY_SUBSTRING}) FROM ${DbSchema.DAILY_PRICES_TABLE})"

/**
 * Params:
 * 1. Start day YYYY-MM-DD, inclusive
 * 2. End day YYYY-MM-DD, exclusive
 */
private const val SELECT_DAILY_PRICES =
    "SELECT ${DbSchema.DAILY_PRICE_ID_AS_DAY_STRING}, " +
            "${DbSchema.DAILY_PRICE_ID_AS_BASE_CODE}, " +
            "${DbSchema.DAILY_PRICES_TABLE}.${DbSchema.DAILY_PRICE_PRICE} as ${DbSchema.DAILY_PRICE_SELECTED_PRICE} " +
            "FROM ${DbSchema.DAILY_PRICES_TABLE} " +
            "WHERE ${DbSchema.DAILY_PRICE_SELECTED_DAY_STRING} >= ? " +
            "AND ${DbSchema.DAILY_PRICE_SELECTED_DAY_STRING} < ? "
