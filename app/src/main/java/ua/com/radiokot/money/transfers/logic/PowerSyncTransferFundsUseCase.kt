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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import ua.com.radiokot.money.accounts.data.PowerSyncAccountRepository
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransfersPreferences
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
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
        source: TransferCounterparty,
        sourceAmount: BigInteger,
        destination: TransferCounterparty,
        destinationAmount: BigInteger,
        memo: String?,
        date: LocalDate,
    ): Result<Unit> = runCatching {

        val lastTransferOfTheDay: Transfer? = transferHistoryRepository
            .getTransferHistoryPage(
                period = HistoryPeriod.Day(
                    localDay = date,
                ),
                pageLimit = 1,
                pageBefore = null,
                sourceId = null,
                destinationId = null,
            )
            .firstOrNull()

        val thisTransferTime =
            (lastTransferOfTheDay?.time ?: date.atStartOfDayIn(TimeZone.currentSystemDefault()))
                .plus(1, DateTimeUnit.SECOND)

        database.writeTransaction { transaction ->
            if (source is TransferCounterparty.Account) {
                accountRepository.updateAccountBalanceBy(
                    accountId = source.account.id,
                    delta = -sourceAmount,
                    transaction = transaction,
                )

                if (destination is TransferCounterparty.Category) {
                    transfersPreferences?.setLastUsedAccountByCategory(
                        categoryId = destination.category.id,
                        accountId = source.account.id,
                    )
                }
            }

            if (destination is TransferCounterparty.Account) {
                accountRepository.updateAccountBalanceBy(
                    accountId = destination.account.id,
                    delta = destinationAmount,
                    transaction = transaction,
                )

                if (source is TransferCounterparty.Category) {
                    transfersPreferences?.setLastUsedAccountByCategory(
                        categoryId = source.category.id,
                        accountId = destination.account.id,
                    )
                }
            }

            transferHistoryRepository.logTransfer(
                sourceId = source.id,
                sourceAmount = sourceAmount,
                destinationId = destination.id,
                destinationAmount = destinationAmount,
                memo = memo,
                time = thisTransferTime,
                transaction = transaction,
            )
        }
    }
}
