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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import java.math.BigInteger

class PowerSyncEditTransferUseCase(
    private val revertTransferUseCase: PowerSyncRevertTransferUseCase,
    private val transferFundsUseCase: PowerSyncTransferFundsUseCase,
    private val transferHistoryRepository: TransferHistoryRepository,
    private val database: PowerSyncDatabase,
) : EditTransferUseCase {

    override suspend fun invoke(
        transferId: String,
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        date: LocalDate,
        exactTime: Instant?,
    ): Result<Unit> = runCatching {

        val transfer = transferHistoryRepository.getTransfer(transferId)
        val time = exactTime
            ?: transferFundsUseCase.findSuitableTime(date)

        database.writeTransaction { transaction ->
            revertTransferUseCase.revertInTransaction(
                transfer = transfer,
                transaction = transaction,
            )

            transferFundsUseCase.transferInTransaction(
                sourceId = sourceId,
                sourceAmount = sourceAmount,
                destinationId = destinationId,
                destinationAmount = destinationAmount,
                memo = memo,
                time = time,
                transaction = transaction,
            )
        }
    }
}
