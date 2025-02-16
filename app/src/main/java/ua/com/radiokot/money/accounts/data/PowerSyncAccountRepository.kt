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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import ua.com.radiokot.money.currency.data.Currency
import java.math.BigInteger

class PowerSyncAccountRepository(
    private val database: PowerSyncDatabase,
) : AccountRepository {

    override suspend fun getAccounts(): List<Account> =
        database
            .getAll(
                sql = SELECT,
                mapper = ::toAccount,
            )
            .sortedBy(Account::title)

    override fun getAccountsFlow(): Flow<List<Account>> =
        database
            .watch(
                sql = SELECT,
                mapper = ::toAccount,
            )
            .map { it.sortedBy(Account::title) }

    override suspend fun getAccount(accountId: String): Account? =
        database
            .getOptional(
                sql = SELECT_BY_ID,
                parameters = listOf(accountId),
                mapper = ::toAccount,
            )

    override fun getAccountFlow(accountId: String): Flow<Account> =
        database
            .watch(
                sql = SELECT_BY_ID,
                parameters = listOf(accountId),
                mapper = ::toAccount,
            )
            .mapNotNull(List<Account>::firstOrNull)

    override suspend fun updateBalance(accountId: String, newValue: BigInteger) {
        database
            .execute(
                sql = "UPDATE accounts SET balance = ? WHERE id = ?",
                parameters = listOf(
                    newValue.toString(),
                    accountId,
                )
            )
    }

    override suspend fun transfer(
        sourceAccountId: String,
        sourceAmount: BigInteger,
        destinationAccountId: String,
        destinationAmount: BigInteger,
    ) {
        database.writeTransaction { transaction ->
            fun getBalance(accountId: String): BigInteger =
                transaction.get(
                    sql = "SELECT balance FROM accounts WHERE id = ?",
                    parameters = listOf(
                        accountId
                    ),
                    mapper = { cursor ->
                        BigInteger(cursor.getString(0)!!.trim())
                    }
                )

            transaction.execute(
                sql = "UPDATE accounts SET balance = ? WHERE id = ?",
                parameters = listOf(
                    (getBalance(sourceAccountId) - sourceAmount).toString(),
                    sourceAccountId,
                )
            )

            transaction.execute(
                sql = "UPDATE accounts SET balance = ? WHERE id = ?",
                parameters = listOf(
                    (getBalance(destinationAccountId) + destinationAmount).toString(),
                    destinationAccountId,
                )
            )
        }
    }

    private fun toAccount(sqlCursor: SqlCursor): Account = sqlCursor.run {
        var column = 0

        val currency = Currency(
            id = getString(column)!!,
            code = getString(++column)!!.trim(),
            symbol = getString(++column)!!.trim(),
            precision = getLong(++column)!!.toInt(),
        )

        Account(
            id = getString(++column)!!,
            title = getString(++column)!!.trim(),
            balance = BigInteger(getString(++column)!!.trim()),
            currency = currency,
        )
    }
}

private const val SELECT =
    "SELECT currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "accounts.id, accounts.title, accounts.balance, accounts.currency_id " +
            "FROM accounts, currencies " +
            "WHERE accounts.currency_id = currencies.id"
private const val SELECT_BY_ID = "$SELECT AND accounts.id = ?"
