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

package ua.com.radiokot.money.accounts.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.SqlCursor
import kotlinx.coroutines.flow.Flow
import ua.com.radiokot.money.currency.data.Currency
import java.math.BigInteger

class PowerSyncAccountRepository(
    private val database: PowerSyncDatabase,
) : AccountRepository {

    override suspend fun getAccounts(): List<Account> =
        database
            .getAll(
                sql = SELECT_QUERY,
                mapper = ::toAccount,
            )

    override fun getAccountsFlow(): Flow<List<Account>> =
        database
            .watch(
                sql = SELECT_QUERY,
                mapper = ::toAccount,
            )

    private fun toAccount(sqlCursor: SqlCursor): Account = sqlCursor.run {
        var column = 0

        val currency = Currency(
            id = getString(column)!!,
            code = getString(++column)!!,
            symbol = getString(++column)!!,
            precision = getLong(++column)!!.toShort(),
        )

        Account(
            id = getString(++column)!!,
            title = getString(++column)!!,
            balance = BigInteger(getString(++column)!!),
            currency = currency,
        )
    }
}

//private const val SELECT_QUERY = "SELECT currency_id, id, title, balance FROM accounts"
private const val SELECT_QUERY =
    "SELECT currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "accounts.id, accounts.title, accounts.balance, accounts.currency_id " +
            "FROM accounts, currencies " +
            "WHERE accounts.currency_id = currencies.id"
