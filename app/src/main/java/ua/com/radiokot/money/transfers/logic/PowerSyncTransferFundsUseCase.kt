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
import kotlinx.datetime.LocalDate
import ua.com.radiokot.money.accounts.data.PowerSyncAccountRepository
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.data.TransfersPreferences
import ua.com.radiokot.money.transfers.history.data.PowerSyncTransferHistoryRepository
import java.math.BigInteger

/**
 * A transfer implementation utilizing PowerSync database transactions.
 *
 * @param transfersPreferences if set, last used account for category is saved
 */
class PowerSyncTransferFundsUseCase(
    private val accountRepository: PowerSyncAccountRepository,
    private val transferHistoryRepository: PowerSyncTransferHistoryRepository,
    private val database: PowerSyncDatabase,
    private val transfersPreferences: TransfersPreferences?,
) : TransferFundsUseCase {

    override suspend fun invoke(
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        date: LocalDate,
    ): Result<Unit> = runCatching {

        val time = transferHistoryRepository.getTimeForTransfer(
            date = date,
        )

        database.writeTransaction { transaction ->

            // For yet unknown reasons, the balance updates must go
            // before transfer insert, otherwise they may not be applied
            // when the use case is launched from the shortcut screen,
            // when app goes to background immediately on completion.
            // https://github.com/powersync-ja/powersync-kotlin/issues/167

            if (sourceId is TransferCounterpartyId.Account) {
                accountRepository.updateAccountBalanceBy(
                    accountId = sourceId.accountId,
                    delta = -sourceAmount,
                    transaction = transaction,
                )

                if (destinationId is TransferCounterpartyId.Category) {
                    transfersPreferences?.setLastUsedAccountByCategory(
                        categoryId = destinationId.categoryId,
                        accountId = sourceId.accountId,
                    )
                }
            }

            if (destinationId is TransferCounterpartyId.Account) {
                accountRepository.updateAccountBalanceBy(
                    accountId = destinationId.accountId,
                    delta = destinationAmount,
                    transaction = transaction,
                )

                if (sourceId is TransferCounterpartyId.Category) {
                    transfersPreferences?.setLastUsedAccountByCategory(
                        categoryId = sourceId.categoryId,
                        accountId = destinationId.accountId,
                    )
                }
            }

            transferHistoryRepository.addOrUpdateTransfer(
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
