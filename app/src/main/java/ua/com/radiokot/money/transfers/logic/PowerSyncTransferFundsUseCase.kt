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
import kotlinx.datetime.Instant
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import java.math.BigInteger
import java.util.UUID

/**
 * A transfer implementation utilizing PowerSync database transactions.
 * Saves time with second precision.
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
        time: Instant,
    ): Result<Unit> = runCatching {
        database.writeTransaction { transaction ->
            if (source is TransferCounterparty.Account) {
                transaction.updateAccountBalanceBy(
                    accountId = source.account.id,
                    delta = -sourceAmount,
                )
            }

            if (destination is TransferCounterparty.Account) {
                transaction.updateAccountBalanceBy(
                    accountId = destination.account.id,
                    delta = destinationAmount,
                )
            }

            transaction.logTransfer(
                sourceId = source.id,
                sourceAmount = sourceAmount,
                destinationId = destination.id,
                destinationAmount = destinationAmount,
                time = time,
            )
        }
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

    private fun PowerSyncTransaction.logTransfer(
        sourceId: String,
        sourceAmount: BigInteger,
        destinationId: String,
        destinationAmount: BigInteger,
        time: Instant,
    ) {
        val id = UUID.randomUUID().toString()

        // ISO-8601 datetime with T, without millis,
        // with explicitly specified UTC timezone (Z).
        // For example, 2025-02-22T08:37:23Z
        val timeString = Instant.fromEpochSeconds(time.epochSeconds).toString()

        log.debug {
            "logTransfer(): logging transfer:" +
                    "\nid=$id," +
                    "\ntime=$timeString," +
                    "\nsourceId=$sourceId," +
                    "\nsourceAmount=$sourceAmount," +
                    "\ndestinationId=$destinationId," +
                    "\ndestinationAmount=$destinationAmount"
        }

        execute(
            sql = "INSERT INTO transfers " +
                    "(id, time, source_id, source_amount, destination_id, destination_amount) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
            parameters = listOf(
                id,
                timeString,
                sourceId,
                sourceAmount.toString(),
                destinationId,
                destinationAmount.toString(),
            )
        )
    }
}
