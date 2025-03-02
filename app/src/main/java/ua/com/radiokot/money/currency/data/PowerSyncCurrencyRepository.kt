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
import com.powersync.db.SqlCursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class PowerSyncCurrencyRepository(
    private val database: PowerSyncDatabase,
) : CurrencyRepository {

    override suspend fun getCurrencies(): List<Currency> =
        database.getAll(
            sql = SELECT_CURRENCIES,
            mapper = ::toCurrency,
        )

    override fun getCurrenciesFlow(): Flow<List<Currency>> =
        database.watch(
            sql = SELECT_CURRENCIES,
            mapper = ::toCurrency,
        )

    override fun getCurrencyPairMapFlow(): Flow<CurrencyPairMap> =
        database
            .watch(
                sql = SELECT_PAIRS,
                mapper = ::toPricePair,
            )
            .map { pairs ->
                // Synced pairs have USD quote.
                CurrencyPairMap(
                    quoteCode = "USD",
                    decimalPriceByBaseCode = pairs.toMap(),
                )
            }

    private fun toCurrency(sqlCursor: SqlCursor) = sqlCursor.run {
        Currency(
            code = getString(0)!!.trim(),
            symbol = getString(1)!!.trim(),
            precision = getLong(2)!!.toInt(),
            id = getString(3)!!,
        )
    }

    private fun toPricePair(sqlCursor: SqlCursor): Pair<String, BigDecimal> = sqlCursor.run {
        getString(0)!! to BigDecimal(getString(1)!!.trim())
    }
}

private const val SELECT_CURRENCIES = "SELECT code, symbol, precision, id FROM currencies"

private const val SELECT_PAIRS = "SELECT id, price FROM pairs"
