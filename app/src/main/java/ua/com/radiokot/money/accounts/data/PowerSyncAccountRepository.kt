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
import com.powersync.db.internal.PowerSyncTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeDescPositionHealer
import ua.com.radiokot.money.util.SternBrocotTreeSearch
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class PowerSyncAccountRepository(
    colorSchemeRepository: ItemColorSchemeRepository,
    private val database: PowerSyncDatabase,
) : AccountRepository {

    private val log by lazyLogger("PowerSyncAccountRepo")
    private val colorSchemesByName = colorSchemeRepository.getItemColorSchemesByName()
    private val positionHealer = SternBrocotTreeDescPositionHealer(Account::position)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val accountsSharedFlow =
        database
            .watch(
                sql = SELECT_ACCOUNTS,
                mapper = ::toAccount,
            )
            .flatMapLatest(::healPositionsIfNeeded)
            .flowOn(Dispatchers.Default)
            .shareIn(coroutineScope, SharingStarted.Lazily, replay = 1)

    override suspend fun getAccounts(): List<Account> =
        accountsSharedFlow.first()

    override fun getAccountsFlow(): Flow<List<Account>> =
        accountsSharedFlow

    override suspend fun getAccount(accountId: String): Account? =
        database
            .getOptional(
                sql = SELECT_ACCOUNT_BY_ID,
                parameters = listOf(accountId),
                mapper = ::toAccount,
            )

    override fun getAccountFlow(accountId: String): Flow<Account> =
        database
            .watch(
                sql = SELECT_ACCOUNT_BY_ID,
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

    override suspend fun updatePosition(
        accountToMoveId: String,
        accountToPlaceBeforeId: String?,
    ) {
        val sortedAccounts = getAccounts().sorted()

        val accountToMoveIndex = sortedAccounts
            .indexOfFirst { it.id == accountToMoveId }
        val accountToMove = sortedAccounts[accountToMoveIndex]

        val accountToPlaceBeforeIndex =
            if (accountToPlaceBeforeId != null)
                sortedAccounts
                    .indexOfFirst { it.id == accountToPlaceBeforeId }
            else
                null
        val accountToPlaceBefore = accountToPlaceBeforeIndex?.let(sortedAccounts::get)

        if (accountToPlaceBeforeIndex == accountToMoveIndex + 1
            || accountToPlaceBeforeIndex == null && accountToMoveIndex == sortedAccounts.size - 1
        ) {
            log.debug {
                "updatePosition(): skipping as the account is already in place"
            }

            return
        }

        // Avoid position recalculation when swapping neighbours,
        // otherwise the fraction quickly becomes tiny (after tens of swaps).
        if (accountToPlaceBeforeIndex == accountToMoveIndex + 2
            || accountToPlaceBeforeIndex == null && accountToMoveIndex == sortedAccounts.size - 2
            || accountToPlaceBeforeIndex == accountToMoveIndex - 1
        ) {
            val accountToSwapWith =
                if (accountToPlaceBeforeIndex == null || accountToPlaceBeforeIndex > accountToMoveIndex)
                    sortedAccounts[accountToMoveIndex + 1]
                else
                    sortedAccounts[accountToMoveIndex - 1]

            log.debug {
                "updatePosition(): swapping positions:" +
                        "\nswap=$accountToMove," +
                        "\nwith=$accountToSwapWith"
            }

            database.writeTransaction { transition ->
                transition.execute(
                    sql = UPDATE_POSITION_BY_ID,
                    parameters = listOf(
                        accountToSwapWith.position.toString(),
                        accountToMove.id,
                    )
                )
                transition.execute(
                    sql = UPDATE_POSITION_BY_ID,
                    parameters = listOf(
                        accountToMove.position.toString(),
                        accountToSwapWith.id,
                    )
                )
            }

            log.debug {
                "updatePosition(): swapped successfully"
            }

            return
        }

        log.debug {
            "updatePosition(): calculating new position" +
                    "\nfor=$accountToMove," +
                    "\ntoPlaceBefore=${accountToPlaceBefore}"
        }

        // As accounts are ordered by descending position,
        // placing before means assigning greater position value.
        // The end (bottom) has position 0.0.
        val lowerBound = accountToPlaceBefore?.position ?: 0.0
        val upperBound =
            if (accountToPlaceBefore == null)
                sortedAccounts
                    .last()
                    .position
            else
                sortedAccounts
                    .lastOrNull { it.position > accountToPlaceBefore.position }
                    ?.position
                    ?: Double.POSITIVE_INFINITY

        log.debug {
            "updatePosition(): got bounds for new position:" +
                    "\nlower=$lowerBound," +
                    "\nupper=$upperBound"
        }

        val newPosition = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = lowerBound,
                upperBound = upperBound,
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

    private suspend fun healPositionsIfNeeded(
        accounts: List<Account>,
    ): Flow<List<Account>> {
        if (positionHealer.arePositionsHealthy(accounts)) {
            return flowOf(accounts)
        }

        log.debug {
            "healPositionsIfNeeded(): start healing"
        }

        var updateCount = 0
        database.writeTransaction { transaction ->
            positionHealer.healPositions(
                items = accounts,
                updatePosition = { item, newPosition ->
                    transaction.execute(
                        sql = "UPDATE accounts SET position = ? WHERE id = ?",
                        parameters = listOf(
                            newPosition.toString(),
                            item.id,
                        )
                    )
                    updateCount++
                }
            )
        }

        log.debug {
            "healPositionsIfNeeded(): healed successfully:" +
                    "\nupdates=$updateCount"
        }

        return emptyFlow()
    }

    fun updateAccountBalanceBy(
        accountId: String,
        delta: BigInteger,
        transaction: PowerSyncTransaction,
    ) {
        val currentBalance = transaction.get(
            sql = "SELECT balance FROM accounts WHERE id = ?",
            parameters = listOf(
                accountId
            ),
            mapper = { cursor ->
                BigInteger(cursor.getString(0)!!.trim())
            }
        )

        val newBalance = (currentBalance + delta).toString()

        log.debug {
            "updateAccountBalanceBy(): updating balance:" +
                    "\naccountId=$accountId," +
                    "\ndelta=$delta" +
                    "\nnewBalance=$newBalance"
        }

        transaction.execute(
            sql = "UPDATE accounts SET balance = ? WHERE id = ?",
            parameters = listOf(
                newBalance,
                accountId,
            )
        )
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
            colorScheme = getString(++column)!!
                .trim()
                .let { colorSchemeName ->
                    colorSchemesByName[colorSchemeName]
                        ?: error("Can't find '$colorSchemeName' color scheme")
                },
            type = getString(++column)!!.trim().let(Account.Type::fromSlug),
            currency = currency,
        )
    }
}

private const val SELECT_ACCOUNTS =
    "SELECT currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "accounts.id, accounts.title, accounts.balance, accounts.position, " +
            "accounts.color_scheme, accounts.type, accounts.currency_id " +
            "FROM accounts, currencies " +
            "WHERE accounts.currency_id = currencies.id"

private const val SELECT_ACCOUNT_BY_ID = "$SELECT_ACCOUNTS AND accounts.id = ?"

private const val UPDATE_POSITION_BY_ID = "UPDATE accounts SET position = ? WHERE id = ?"
