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
import com.powersync.db.getBooleanOptional
import com.powersync.db.getDouble
import com.powersync.db.getLong
import com.powersync.db.getString
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
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.colors.data.ItemColorScheme
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

    private val accountsSharedFlow = database
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

    override suspend fun getAccount(
        accountId: String,
    ): Account? = database
        .getOptional(
            sql = SELECT_ACCOUNT_BY_ID,
            parameters = listOf(accountId),
            mapper = ::toAccount,
        )

    override fun getAccountFlow(
        accountId: String,
    ): Flow<Account> = database
        .watch(
            sql = SELECT_ACCOUNT_BY_ID,
            parameters = listOf(accountId),
            mapper = ::toAccount,
        )
        .mapNotNull(List<Account>::firstOrNull)

    override suspend fun updateBalance(
        accountId: String,
        newValue: BigInteger,
    ) {
        database
            .execute(
                sql = "UPDATE accounts SET balance = ? WHERE id = ?",
                parameters = listOf(
                    newValue.toString(),
                    accountId,
                )
            )
    }

    override suspend fun move(
        accountToMove: Account,
        accountToPlaceBefore: Account?,
        accountToPlaceAfter: Account?,
    ) {
        val sortedAccounts = getAccounts()
            .sorted()

        val targetType = accountToPlaceBefore?.type
            ?: accountToPlaceAfter?.type
            ?: accountToMove.type

        val isChangingType = accountToMove.type != targetType

        val sortedAccountsWithinTargetType = sortedAccounts
            .filter { it.type == accountToMove.type }

        if (!isChangingType) {

            val accountToMoveIndexWithinTargetType =
                sortedAccountsWithinTargetType.indexOf(accountToMove)

            val accountToPlaceBeforeIndexWithinTargetType =
                sortedAccountsWithinTargetType.indexOf(accountToPlaceBefore)

            val accountToPlaceAfterIndexWithinTargetType =
                sortedAccountsWithinTargetType.indexOf(accountToPlaceAfter)

            if (accountToPlaceBeforeIndexWithinTargetType == accountToMoveIndexWithinTargetType + 1
                || accountToPlaceAfterIndexWithinTargetType == accountToMoveIndexWithinTargetType - 1
            ) {
                log.debug {
                    "move(): skipping as the account is already in place"
                }

                return
            }

            // Avoid position recalculation when swapping neighbours within the same type,
            // otherwise the fraction quickly becomes tiny (after tens of swaps).
            if (accountToPlaceBeforeIndexWithinTargetType == accountToMoveIndexWithinTargetType + 2
                || accountToPlaceAfterIndexWithinTargetType == accountToMoveIndexWithinTargetType - 2
            ) {
                val accountToSwapWith =
                    if (accountToPlaceBeforeIndexWithinTargetType == accountToMoveIndexWithinTargetType + 2)
                        sortedAccountsWithinTargetType[accountToMoveIndexWithinTargetType + 1]
                    else
                        sortedAccountsWithinTargetType[accountToMoveIndexWithinTargetType - 1]

                log.debug {
                    "move(): swapping positions within the same type:" +
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
                    "move(): swapped successfully"
                }

                return
            }
        }

        log.debug {
            "move(): calculating new position" +
                    "\nfor=$accountToMove," +
                    "\ntoPlaceBefore=${accountToPlaceBefore}," +
                    "\nwithinType=$targetType"
        }

        // As accounts are ordered by descending position,
        // placing before means assigning greater position value.
        // The end (bottom) is 0.0.
        val lowerBound = accountToPlaceBefore?.position ?: 0.0
        // The start (top) is +∞.
        val upperBound = accountToPlaceAfter?.position ?: Double.POSITIVE_INFINITY

        log.debug {
            "move(): got bounds for new position:" +
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
            "move(): updating:" +
                    "\naccount=$accountToMove," +
                    "\nnewPosition=$newPosition," +
                    "\ntargetType=$targetType," +
                    "\nisChangingType=$isChangingType"
        }

        database.writeTransaction { transaction ->
            transaction.execute(
                sql = UPDATE_POSITION_BY_ID,
                parameters = listOf(
                    newPosition.toString(),
                    accountToMove.id,
                )
            )

            if (isChangingType) {
                transaction.execute(
                    sql = UPDATE_TYPE_BY_ID,
                    parameters = listOf(
                        targetType.slug,
                        accountToMove.id,
                    )
                )
            }
        }

        log.debug {
            "move(): moved successfully"
        }
    }

    private suspend fun healPositionsIfNeeded(
        accounts: List<Account>,
    ): Flow<List<Account>> = accounts
        .groupBy(Account::type)
        .map { (type, accountsOfType) ->
            if (positionHealer.arePositionsHealthy(accountsOfType)) {
                return@map true
            }

            log.debug {
                "healPositionsIfNeeded(): start healing:" +
                        "\nwithinType=$type"
            }

            var updateCount = 0
            database.writeTransaction { transaction ->
                positionHealer.healPositions(
                    items = accountsOfType,
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
                        "\nupdates=$updateCount," +
                        "\nwithinType=$type"
            }

            return@map false
        }
        .let { positionHealth ->
            if (positionHealth.any { !it })
                emptyFlow()
            else
                flowOf(accounts)
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

    fun addAccount(
        title: String,
        currency: Currency,
        type: Account.Type,
        colorScheme: ItemColorScheme,
        transaction: PowerSyncTransaction,
    ): Account {

        val accountToPlaceBefore = runBlocking {
            getAccounts()
                .sorted()
                .firstOrNull { it.type == type }
        }

        val position = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = accountToPlaceBefore?.position ?: 0.0,
                upperBound = Double.POSITIVE_INFINITY,
            )
            .value

        val account = Account(
            title = title,
            balance = BigInteger.ZERO,
            currency = currency,
            colorScheme = colorScheme,
            type = type,
            isArchived = false,
            position = position,
        )

        transaction.execute(
            sql = INSERT_ACCOUNT,
            parameters = listOf(
                account.id,
                account.title,
                account.balance.toString(),
                account.currency.id,
                account.position,
                account.colorScheme.name,
                account.type.slug,
            )
        )

        log.debug {
            "addAccount(): added:" +
                    "\naccount=$account"
        }

        return account
    }

    fun updateAccount(
        accountId: String,
        newTitle: String,
        newType: Account.Type,
        newColorScheme: ItemColorScheme,
        transaction: PowerSyncTransaction,
    ) {
        val accountToUpdate = runBlocking {
            getAccount(accountId)
                ?: error("Account to update not found")
        }

        transaction.execute(
            sql = UPDATE_ACCOUNT_BY_ID,
            parameters = listOf(
                newTitle,
                newType.slug,
                newColorScheme.name,
                accountToUpdate.id,
            )
        )

        if (newType != accountToUpdate.type) {

            val accountToPlaceBefore = runBlocking {
                getAccounts()
                    .sorted()
                    .firstOrNull { it.type == newType }
            }

            val newPosition = SternBrocotTreeSearch()
                .goBetween(
                    lowerBound = accountToPlaceBefore?.position ?: 0.0,
                    upperBound = Double.POSITIVE_INFINITY,
                )
                .value

            transaction.execute(
                sql = UPDATE_POSITION_BY_ID,
                parameters = listOf(
                    newPosition.toString(),
                    accountToUpdate.id,
                )
            )

            log.debug {
                "updateAccount(): also updated position on type change:" +
                        "\noldPosition=${accountToUpdate.position}," +
                        "\noldType=${accountToUpdate.type}," +
                        "\nnewPosition=$newPosition," +
                        "\nnewType=$newType"
            }
        }
    }

    private fun toAccount(
        sqlCursor: SqlCursor,
    ): Account = with(sqlCursor) {
        Account(
            id = getString("accountId"),
            title = getString("accountTitle").trim(),
            balance = BigInteger(getString("accountBalance").trim()),
            position = getDouble("accountPosition"),
            colorScheme = getString("accountColorScheme")
                .trim()
                .let { colorSchemeName ->
                    colorSchemesByName[colorSchemeName]
                        ?: error("Can't find '$colorSchemeName' color scheme")
                },
            type = getString("accountType")
                .trim()
                .let(Account.Type::fromSlug),
            isArchived = getBooleanOptional("accountArchived") == true,
            currency = Currency(
                id = getString("currencyId"),
                code = getString("currencyCode").trim(),
                symbol = getString("currencySymbol").trim(),
                precision = getLong("currencyPrecision").toInt(),
            ),
        )
    }
}

private const val SELECT_ACCOUNTS =
    "SELECT " +
            "currencies.id as 'currencyId', " +
            "currencies.code as 'currencyCode', " +
            "currencies.symbol as 'currencySymbol', " +
            "currencies.precision as 'currencyPrecision', " +
            "accounts.id as 'accountId', " +
            "accounts.title as 'accountTitle', " +
            "accounts.balance as 'accountBalance', " +
            "accounts.position as 'accountPosition', " +
            "accounts.color_scheme as 'accountColorScheme', " +
            "accounts.type as 'accountType', " +
            "accounts.archived as 'accountArchived', " +
            "accounts.currency_id as 'accountCurrencyId' " +
            "FROM accounts, currencies " +
            "WHERE accounts.currency_id = currencies.id"

private const val SELECT_ACCOUNT_BY_ID = "$SELECT_ACCOUNTS AND accounts.id = ?"

private const val UPDATE_POSITION_BY_ID = "UPDATE accounts SET position = ? WHERE id = ?"

private const val UPDATE_TYPE_BY_ID = "UPDATE accounts SET type = ? WHERE id = ?"

/**
 * Params:
 * 1. Title
 * 2. Type slug
 * 3. Color scheme name
 * 4. ID
 */
private const val UPDATE_ACCOUNT_BY_ID =
    "UPDATE accounts SET " +
            "title = ?, " +
            "type = ?, " +
            "color_scheme = ? " +
            "WHERE id = ? "

/**
 * Params:
 * 1. ID
 * 2. Title
 * 3. Balance string
 * 4. Currency ID
 * 5. Position string
 * 6. Color scheme name
 * 7. Type slug
 */
private const val INSERT_ACCOUNT =
    "INSERT INTO accounts " +
            "(id, title, balance, currency_id, position, color_scheme, type) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?)"
