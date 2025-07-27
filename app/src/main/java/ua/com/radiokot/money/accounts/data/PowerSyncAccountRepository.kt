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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.powersync.DbSchema
import java.math.BigInteger

class PowerSyncAccountRepository(
    colorSchemeRepository: ItemColorSchemeRepository,
    private val database: PowerSyncDatabase,
) : AccountRepository {

    private val log by lazyLogger("PowerSyncAccountRepo")
    private val colorSchemesByName = colorSchemeRepository.getItemColorSchemesByName()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val accountsSharedFlow = database
        .watch(
            sql = SELECT_ACCOUNTS,
            mapper = ::toAccount,
        )
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
        position: Double,
        transaction: PowerSyncTransaction,
    ): Account {

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
        transaction.execute(
            sql = UPDATE_ACCOUNT_BY_ID,
            parameters = listOf(
                newTitle,
                newType.slug,
                newColorScheme.name,
                accountId,
            )
        )
    }

    fun updatePosition(
        accountId: String,
        newPosition: Double,
        transaction: PowerSyncTransaction,
    ) {
        transaction.execute(
            sql = UPDATE_POSITION_BY_ID,
            parameters = listOf(
                newPosition.toString(),
                accountId,
            )
        )
    }

    fun updateType(
        accountId: String,
        newType: Account.Type,
        transaction: PowerSyncTransaction,
    ) {
        transaction.execute(
            sql = UPDATE_TYPE_BY_ID,
            parameters = listOf(
                newType.slug,
                accountId,
            )
        )
    }

    private fun updateArchived(
        accountId: String,
        isArchived: Boolean,
        transaction: PowerSyncTransaction,
    ) {
        transaction.execute(
            sql = UPDATE_ARCHIVED_BY_ID,
            parameters = listOf(
                isArchived,
                accountId,
            )
        )
    }

    override suspend fun archive(
        accountId: String,
    ) {
        database.writeTransaction { transaction ->

            updateArchived(
                accountId = accountId,
                isArchived = true,
                transaction = transaction,
            )
        }
    }

    override suspend fun unarchive(
        accountId: String,
        newPosition: Double,
    ) {
        database.writeTransaction { transaction ->

            updateArchived(
                accountId = accountId,
                isArchived = false,
                transaction = transaction,
            )

            updatePosition(
                accountId = accountId,
                newPosition = newPosition,
                transaction = transaction,
            )
        }
    }

    private fun toAccount(
        sqlCursor: SqlCursor,
    ): Account =
        DbSchema.toAccount(
            sqlCursor = sqlCursor,
            colorSchemesByName = colorSchemesByName,
        )
}

private const val SELECT_ACCOUNTS =
    "SELECT " +
            DbSchema.CURRENCY_SELECT_COLUMNS + ", " +
            DbSchema.ACCOUNT_SELECT_COLUMNS +
            "FROM ${DbSchema.ACCOUNTS_TABLE}, ${DbSchema.CURRENCIES_TABLE} " +
            "WHERE ${DbSchema.ACCOUNT_SELECTED_CURRENCY_ID} = ${DbSchema.CURRENCY_SELECTED_ID}"

private const val SELECT_ACCOUNT_BY_ID =
    "$SELECT_ACCOUNTS AND ${DbSchema.ACCOUNT_SELECTED_ID} = ?"

/**
 * Params:
 * 1. New position
 * 2. ID
 */
private const val UPDATE_POSITION_BY_ID =
    "UPDATE ${DbSchema.ACCOUNTS_TABLE} " +
            "SET ${DbSchema.ACCOUNT_POSITION} = ? " +
            "WHERE ${DbSchema.ID} = ?"

/**
 * Params:
 * 1. New type slug
 * 2. ID
 */
private const val UPDATE_TYPE_BY_ID =
    "UPDATE ${DbSchema.ACCOUNTS_TABLE} SET " +
            "${DbSchema.ACCOUNT_TYPE} = ? " +
            "WHERE ${DbSchema.ID} = ?"

/**
 * Params:
 * 1. Title
 * 2. Type slug
 * 3. Color scheme name
 * 4. ID
 */
private const val UPDATE_ACCOUNT_BY_ID =
    "UPDATE ${DbSchema.ACCOUNTS_TABLE} SET " +
            "${DbSchema.ACCOUNT_TITLE} = ?, " +
            "${DbSchema.ACCOUNT_TYPE} = ?, " +
            "${DbSchema.ACCOUNT_COLOR_SCHEME} = ? " +
            "WHERE ${DbSchema.ID} = ? "

/**
 * Params:
 * 1. Is archived boolean
 * 2. ID
 */
private const val UPDATE_ARCHIVED_BY_ID =
    "UPDATE ${DbSchema.ACCOUNTS_TABLE} SET " +
            "${DbSchema.ACCOUNT_IS_ARCHIVED} = ? " +
            "WHERE ${DbSchema.ID} = ?"

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
    "INSERT INTO ${DbSchema.ACCOUNTS_TABLE} " +
            "(" +
            "${DbSchema.ID}, " +
            "${DbSchema.ACCOUNT_TITLE}, " +
            "${DbSchema.ACCOUNT_BALANCE}, " +
            "${DbSchema.ACCOUNT_CURRENCY_ID}, " +
            "${DbSchema.ACCOUNT_POSITION}, " +
            "${DbSchema.ACCOUNT_COLOR_SCHEME}, " +
            "${DbSchema.ACCOUNT_TYPE}, " +
            "${DbSchema.ACCOUNT_IS_ARCHIVED} " +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, 0)"
