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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import ua.com.radiokot.money.powersync.DbSchema

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

    private val currencyPairMapSharedFlow =
        database
            .watch(
                sql = SELECT_LATEST_PRICES,
                mapper = DbSchema::toPricePair,
            )
            .map { pairs ->
                // Synced pairs have USD quote.
                CurrencyPairMap(
                    quoteCode = "USD",
                    decimalPriceByBaseCode = pairs.toMap(),
                )
            }
            .flowOn(Dispatchers.Default)
            .shareIn(coroutineScope, SharingStarted.Lazily, replay = 1)

    override fun getCurrencyPairMapFlow(): Flow<CurrencyPairMap> =
        currencyPairMapSharedFlow
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
    "SELECT ${DbSchema.DAILY_PRICE_SELECT_COLUMNS} " +
            "FROM ${DbSchema.DAILY_PRICES_TABLE} " +
            "WHERE ${DbSchema.DAILY_PRICE_SELECTED_DATETIME} = " +
            "(SELECT datetime(MAX(${DbSchema.DAILY_PRICE_DAY_SUBSTRING})) FROM ${DbSchema.DAILY_PRICES_TABLE})"
