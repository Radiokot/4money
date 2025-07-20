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
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.util.SternBrocotTreeSearch

class PowerSyncAddAccountUseCase(
    private val database: PowerSyncDatabase,
    private val accountRepository: PowerSyncAccountRepository,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
) : AddAccountUseCase {

    override suspend fun invoke(
        title: String,
        currency: Currency,
        type: Account.Type,
        colorScheme: ItemColorScheme,
    ): Result<Unit> = runCatching {

        val firstAccountOfTargetType = getVisibleAccountsUseCase()
            .first()
            .firstOrNull { it.type == type }

        val position = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = firstAccountOfTargetType?.position ?: 0.0,
                upperBound = Double.POSITIVE_INFINITY,
            )
            .value

        database.writeTransaction { transaction ->

            accountRepository.addAccount(
                title = title,
                currency = currency,
                type = type,
                colorScheme = colorScheme,
                position = position,
                transaction = transaction,
            )
        }
    }
}
