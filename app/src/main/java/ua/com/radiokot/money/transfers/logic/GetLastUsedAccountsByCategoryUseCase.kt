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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.transfers.data.TransfersPreferences

class GetLastUsedAccountsByCategoryUseCase(
    private val accountRepository: AccountRepository,
    private val transfersPreferences: TransfersPreferences,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Map<String, Account>> =
        combine(
            accountRepository.getAccountsFlow(),
            transfersPreferences.getLastUsedAccountsByCategoryFlow(),
            transform = ::Pair,
        )
            .mapLatest { (allAccounts, accountsByCategoryId) ->
                val accountById = allAccounts.associateBy(Account::id)

                buildMap {
                    accountsByCategoryId.forEach { (categoryId, accountId) ->
                        val account = accountById[accountId]
                        if (account != null) {
                            put(categoryId, account)
                        }
                    }
                }
            }
            .flowOn(Dispatchers.Default)
}
