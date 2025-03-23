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
import ua.com.radiokot.money.accounts.data.PowerSyncAccountRepository
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.PowerSyncTransferHistoryRepository

class PowerSyncRevertTransferUseCase(
    private val accountRepository: PowerSyncAccountRepository,
    private val transferHistoryRepository: PowerSyncTransferHistoryRepository,
    private val database: PowerSyncDatabase,
) : RevertTransferUseCase {

    override suspend fun invoke(transferId: String): Result<Unit> = runCatching {

        val transfer = transferHistoryRepository.getTransfer(transferId)

        database.writeTransaction { transaction ->

            transferHistoryRepository.deleteTransfer(
                transferId = transferId,
                transaction = transaction,
            )

            if (transfer.source is TransferCounterparty.Account) {
                accountRepository.updateAccountBalanceBy(
                    accountId = transfer.source.account.id,
                    delta = transfer.sourceAmount,
                    transaction = transaction
                )
            }

            if (transfer.destination is TransferCounterparty.Account) {
                accountRepository.updateAccountBalanceBy(
                    accountId = transfer.destination.account.id,
                    delta = -transfer.destinationAmount,
                    transaction=transaction,
                )
            }
        }
    }
}
