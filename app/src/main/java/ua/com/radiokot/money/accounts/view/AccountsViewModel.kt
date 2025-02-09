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

package ua.com.radiokot.money.accounts.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.uikit.ViewAccountListItem

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val log by lazyLogger("AccountsVM")
    private val _accountListItems = MutableStateFlow<List<ViewAccountListItem>>(emptyList())
    val accountListItems = _accountListItems.asStateFlow()

    init {
        subscribeToAccounts()

        viewModelScope.launch {
            currencyRepository.getCurrencies().forEach { currency ->
                log.debug {
                    "init(): loaded currency:" +
                            "\ncode=${currency.code}"
                }
            }
        }
    }

    private fun subscribeToAccounts() = viewModelScope.launch {
        accountRepository.getAccountsFlow()
            .flowOn(Dispatchers.Default)
            .map { accounts ->
                accounts.map(ViewAccountListItem::Account)
            }
            .collect(_accountListItems)
    }
}
