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

import kotlinx.coroutines.flow.first
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeSearch

class UnarchiveAccountUseCase(
    private val accountRepository: AccountRepository,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
) {

    private val log by lazyLogger("UnarchiveAccountUC")

    suspend operator fun invoke(
        accountToUnrachive: Account,
    ): Result<Unit> = runCatching {

        val lastAccountOfType = getVisibleAccountsUseCase()
            .first()
            .lastOrNull { it.type == accountToUnrachive.type }

        val newPosition = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = 0.0,
                upperBound = lastAccountOfType?.position ?: Double.POSITIVE_INFINITY,
            )
            .value

        log.debug {
            "invoke(): unarchiving and updating position:" +
                    "\naccountToUnarchive=$accountToUnrachive," +
                    "\nnewPosition=$newPosition"
        }

        accountRepository.unarchive(
            accountId = accountToUnrachive.id,
            newPosition = newPosition,
        )
    }
}
