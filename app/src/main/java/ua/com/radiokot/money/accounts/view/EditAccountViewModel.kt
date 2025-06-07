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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.currency.view.ViewCurrency

class EditAccountViewModel(
    private val currencyRepository: CurrencyRepository,
    private val currencyPreferences: CurrencyPreferences,
    private val itemColorSchemeRepository: ItemColorSchemeRepository,
) : ViewModel() {

    val isNewAccount: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val _name: MutableStateFlow<String> = MutableStateFlow("")
    val name = _name.asStateFlow()
    private val _colorScheme: MutableStateFlow<ItemColorScheme> = MutableStateFlow(
        itemColorSchemeRepository.getItemColorSchemesByName().getValue("Green3")
    )
    val colorScheme = _colorScheme.asStateFlow()
    private val _type: MutableStateFlow<Account.Type> = MutableStateFlow(Account.Type.Regular)
    val type = _type.asStateFlow()
    private val _currency: MutableStateFlow<Currency> = MutableStateFlow(runBlocking {
        currencyRepository
            .getCurrencyByCode(
                code = currencyPreferences.primaryCurrencyCode.value
            )
            ?: currencyRepository.getCurrencies().first()
    })

    val currency: StateFlow<ViewCurrency> =
        _currency
            .map(::ViewCurrency)
            .stateIn(viewModelScope, SharingStarted.Eagerly, ViewCurrency(_currency.value))

    val isSaveEnabled: StateFlow<Boolean> =
        _name
            .map { name ->
                name.isNotBlank()
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onNameChanged(newValue: String) {
        _name.value = newValue
    }
}
