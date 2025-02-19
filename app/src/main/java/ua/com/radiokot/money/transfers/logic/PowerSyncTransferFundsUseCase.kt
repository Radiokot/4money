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

package ua.com.radiokot.money.transfers.logic

import com.powersync.PowerSyncDatabase
import com.powersync.db.internal.PowerSyncTransaction
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import java.math.BigInteger

/**
 * A transfer implementation utilizing PowerSync database transactions.
 */
class PowerSyncTransferFundsUseCase(
    private val database: PowerSyncDatabase,
) : TransferFundsUseCase {

    private val log by lazyLogger("PowerSyncTransferFundsUC")

    override suspend fun invoke(
        source: TransferCounterparty,
        sourceAmount: BigInteger,
        destination: TransferCounterparty,
        destinationAmount: BigInteger,
    ): Result<Unit> = runCatching {
        when {
            source is TransferCounterparty.Account
                    && destination is TransferCounterparty.Account ->
                transferBetweenAccounts(
                    source = source,
                    sourceAmount = sourceAmount,
                    destination = destination,
                    destinationAmount = destinationAmount,
                )

            source is TransferCounterparty.Category
                    && source.category.isIncome
                    && destination is TransferCounterparty.Account ->
                transferFromCategory(
                    source = source,
                    sourceAmount = sourceAmount,
                    destination = destination,
                    destinationAmount = destinationAmount,
                )

            source is TransferCounterparty.Account
                    && destination is TransferCounterparty.Category
                    && !destination.category.isIncome ->
                transferToCategory(
                    source = source,
                    sourceAmount = sourceAmount,
                    destination = destination,
                    destinationAmount = destinationAmount,
                )

            else ->
                error("There's no strategy to transfer from $source to $destination")
        }
    }

    private suspend fun transferBetweenAccounts(
        source: TransferCounterparty.Account,
        sourceAmount: BigInteger,
        destination: TransferCounterparty.Account,
        destinationAmount: BigInteger,
    ) = database.writeTransaction { transaction ->

        transaction.updateAccountBalanceBy(
            accountId = source.account.id,
            delta = -sourceAmount,
        )

        transaction.updateAccountBalanceBy(
            accountId = destination.account.id,
            delta = destinationAmount,
        )

        // TODO Log the transfer.
    }

    private suspend fun transferToCategory(
        source: TransferCounterparty.Account,
        sourceAmount: BigInteger,
        destination: TransferCounterparty.Category,
        destinationAmount: BigInteger,
    ) = database.writeTransaction { transaction ->

        transaction.updateAccountBalanceBy(
            accountId = source.account.id,
            delta = -sourceAmount,
        )

        // TODO Log the transfer.
    }

    private suspend fun transferFromCategory(
        source: TransferCounterparty.Category,
        sourceAmount: BigInteger,
        destination: TransferCounterparty.Account,
        destinationAmount: BigInteger,
    ) = database.writeTransaction { transaction ->

        transaction.updateAccountBalanceBy(
            accountId = destination.account.id,
            delta = destinationAmount,
        )

        // TODO Log the transfer.
    }

    private fun PowerSyncTransaction.updateAccountBalanceBy(
        accountId: String,
        delta: BigInteger,
    ) {
        val currentBalance = get(
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

        execute(
            sql = "UPDATE accounts SET balance = ? WHERE id = ?",
            parameters = listOf(
                newBalance,
                accountId,
            )
        )
    }
}
