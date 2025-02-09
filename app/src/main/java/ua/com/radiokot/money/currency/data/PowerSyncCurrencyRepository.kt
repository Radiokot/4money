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

class PowerSyncCurrencyRepository(
    private val database: PowerSyncDatabase,
) : CurrencyRepository {

    override suspend fun getCurrencies(): List<Currency> =
        database.getAll(
            sql = SELECT_QUERY,
            mapper = ::toCurrency,
        )

    override fun getCurrenciesFlow(): Flow<List<Currency>> =
        database.watch(
            sql = SELECT_QUERY,
            mapper = ::toCurrency,
        )

    private fun toCurrency(sqlCursor: SqlCursor) = sqlCursor.run {
        Currency(
            code = getString(0)!!,
            symbol = getString(1)!!,
            precision = getLong(2)!!.toShort(),
            id = getString(3)!!,
        )
    }
}

private const val SELECT_QUERY = "SELECT code, symbol, precision, id FROM currencies"
