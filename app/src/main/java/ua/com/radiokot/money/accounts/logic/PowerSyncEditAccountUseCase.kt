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

package ua.com.radiokot.money.accounts.logic

import com.powersync.PowerSyncDatabase
import kotlinx.coroutines.flow.first
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.PowerSyncAccountRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeSearch

class PowerSyncEditAccountUseCase(
    private val database: PowerSyncDatabase,
    private val accountRepository: PowerSyncAccountRepository,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
) : EditAccountUseCase {

    private val log by lazyLogger("PowerSyncEditAccountUC")

    override suspend fun invoke(
        accountToEdit: Account,
        newTitle: String,
        newType: Account.Type,
        newColorScheme: ItemColorScheme,
    ): Result<Unit> = runCatching {

        val firstAccountOfTargetType = getVisibleAccountsUseCase()
            .first()
            .firstOrNull { it.type == newType }

        database.writeTransaction { transaction ->

            accountRepository.updateAccount(
                accountId = accountToEdit.id,
                newTitle = newTitle,
                newType = newType,
                newColorScheme = newColorScheme,
                transaction = transaction,
            )

            if (newType != accountToEdit.type) {

                val newPosition = SternBrocotTreeSearch()
                    .goBetween(
                        lowerBound = firstAccountOfTargetType?.position ?: 0.0,
                        upperBound = Double.POSITIVE_INFINITY,
                    )
                    .value

                log.debug {
                    "invoke(): updating position as the type changed:" +
                            "\nnewType=$newType," +
                            "\nnewPosition=$newPosition"
                }

                accountRepository.updatePosition(
                    accountId = accountToEdit.id,
                    newPosition = newPosition,
                    transaction = transaction,
                )
            }
        }
    }
}
