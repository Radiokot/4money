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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeSearch
import java.math.BigInteger

class PowerSyncAccountRepository(
    private val database: PowerSyncDatabase,
) : AccountRepository {

    private val log by lazyLogger("PowerSyncAccountRepo")

    override suspend fun getAccounts(): List<Account> =
        database
            .getAll(
                sql = SELECT_ALL,
                mapper = ::toAccount,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccountsFlow(): Flow<List<Account>> =
        database
            .watch(
                sql = SELECT_ALL,
                mapper = ::toAccount,
            )
            .flatMapLatest { accounts ->
                val distinctPositions = accounts.mapTo(mutableSetOf(), Account::position)
                if (distinctPositions.isNotEmpty()
                    && (distinctPositions.size < accounts.size || distinctPositions.any { it <= 0 })
                ) {
                    healPositions(accounts)
                    emptyFlow()
                } else {
                    flowOf(accounts)
                }
            }

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

    override suspend fun updatePosition(accountToMove: Account, accountToPlaceBefore: Account?) {
        // As accounts are ordered by descending position,
        // placing before means assigning greater value.

        log.debug {
            "updatePosition(): calculating new position" +
                    "\nfor=$accountToMove," +
                    "\ntoPlaceBefore=${accountToPlaceBefore}"
        }

        val newPosition = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = accountToPlaceBefore?.position ?: 0.0,
                upperBound =
                if (accountToPlaceBefore == null)
                    database.get(
                        sql = SELECT_MIN_POSITION,
                        mapper = { it.getDouble(0)!! },
                    )
                else
                    database.get(
                        sql = SELECT_MIN_POSITION_AFTER,
                        parameters = listOf(
                            accountToPlaceBefore.position.toString(),
                        ),
                        mapper = { it.getDouble(0) ?: Double.POSITIVE_INFINITY },
                    )
            )
            .value

        log.debug {
            "updatePosition(): updating:" +
                    "\naccount=$accountToMove," +
                    "\nnewPosition=$newPosition"
        }

        database.execute(
            sql = UPDATE_POSITION_BY_ID,
            parameters = listOf(
                newPosition.toString(),
                accountToMove.id,
            )
        )

        log.debug {
            "updatePosition(): updated successfully"
        }
    }

    private suspend fun healPositions(
        accounts: List<Account>,
    ) {

        log.debug {
            "healPositions(): re-assigning positions:" +
                    "\ncount=${accounts.size}"
        }

        // Assign a new position to each account preserving the current order.
        val params = mutableListOf<String>()
        val sql = buildString {
            append("UPDATE accounts SET position = CASE ")
            val sternBrocotTree = SternBrocotTreeSearch()
            accounts.indices.reversed().forEach { accountIndex ->
                append("\nWHEN id = ? THEN ? ")
                params += accounts[accountIndex].id
                params += sternBrocotTree.value.toString()
                sternBrocotTree.goRight()
            }
            append("END")
        }

        database.execute(
            sql = sql,
            parameters = params,
        )

        log.debug {
            "healPositions(): re-assigned successfully"
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
            position = getDouble(++column)!!,
            currency = currency,
        )
    }
}

private const val SELECT =
    "SELECT currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "accounts.id, accounts.title, accounts.balance, accounts.position, accounts.currency_id " +
            "FROM accounts, currencies " +
            "WHERE accounts.currency_id = currencies.id"

private const val SELECT_ALL = "$SELECT ORDER BY accounts.position DESC"

private const val SELECT_BY_ID = "$SELECT AND accounts.id = ?"

private const val SELECT_MIN_POSITION = "SELECT MIN(position) FROM accounts"

private const val SELECT_MIN_POSITION_AFTER = "$SELECT_MIN_POSITION WHERE position > ?"

private const val UPDATE_POSITION_BY_ID = "UPDATE accounts SET position = ? WHERE id = ?"
