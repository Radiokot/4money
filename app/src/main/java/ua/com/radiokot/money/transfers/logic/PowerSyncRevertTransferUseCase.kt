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

import com.powersync.db.Queries
import com.powersync.db.internal.PowerSyncTransaction
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import java.math.BigInteger

class PowerSyncRevertTransferUseCase(
    private val transferHistoryRepository: TransferHistoryRepository,
    private val database: Queries,
) : RevertTransferUseCase {

    private val log by lazyLogger("PowerSyncRevertTransferUC")

    override suspend fun invoke(transferId: String): Result<Unit> = runCatching {

        val transfer = transferHistoryRepository.getTransfer(transferId)

        database.writeTransaction { transaction ->
            log.debug {
                "invoke(): deleting:" +
                        "\ntransferId=$transferId"
            }

            transaction.execute(
                sql = "DELETE FROM transfers WHERE transfers.id = ?",
                parameters = listOf(
                    transferId,
                )
            )

            if (transfer.source is TransferCounterparty.Account) {
                val sourceAccount = transfer.source.account
                val newSourceAccountBalance =
                    sourceAccount.balance + transfer.sourceAmount

                log.debug {
                    "invoke(): refunding spent amount:" +
                            "\naccountId=${sourceAccount.id}," +
                            "\nspentAmount=${transfer.sourceAmount}," +
                            "\nnewBalance=$newSourceAccountBalance"
                }

                transaction.updateAccountBalance(
                    accountId = sourceAccount.id,
                    newBalance = newSourceAccountBalance,
                )
            }

            if (transfer.destination is TransferCounterparty.Account) {
                val destinationAccount = transfer.destination.account
                val newDestinationAccountBalance =
                    destinationAccount.balance - transfer.sourceAmount

                log.debug {
                    "invoke(): refunding received amount:" +
                            "\naccountId=${destinationAccount.id}," +
                            "\nreceivedAmount=${transfer.destinationAmount}," +
                            "\nnewBalance=$newDestinationAccountBalance"
                }

                transaction.updateAccountBalance(
                    accountId = destinationAccount.id,
                    newBalance = newDestinationAccountBalance,
                )
            }
        }
    }

    private fun PowerSyncTransaction.updateAccountBalance(
        accountId: String,
        newBalance: BigInteger,
    ) = execute(
        sql = "UPDATE accounts SET balance = ? WHERE id = ?",
        parameters = listOf(
            newBalance,
            accountId,
        )
    )
}
