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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map

class EditAccountScreenViewModel(
    private val parameters: Parameters,
    private val currencyRepository: CurrencyRepository,
    private val currencyPreferences: CurrencyPreferences,
    private val itemColorSchemeRepository: ItemColorSchemeRepository,
) : ViewModel() {

    private val log by lazyLogger("EditAccountScreenVM")
    val isNewAccount: Boolean = parameters.accountToEditId == null
    private val _title: MutableStateFlow<String> = MutableStateFlow("")
    val title = _title.asStateFlow()
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
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val currencyCode: StateFlow<String> =
        _currency
            .map(viewModelScope, Currency::code)

    val isCurrencyChangeEnabled: Boolean =
        isNewAccount

    val isSaveEnabled: StateFlow<Boolean> =
        _title
            .map(viewModelScope, String::isNotBlank)

    fun onTitleChanged(newValue: String) {
        _title.value = newValue
    }

    fun onTypeClicked() {
        _events.tryEmit(
            Event.ProceedToAccountTypeSelection(
                currentType = _type.value,
            )
        )
    }

    fun onLogoClicked() {
        _events.tryEmit(
            Event.ProceedToLogoCustomization(
                currentTitle = _title.value,
                currentColorScheme = _colorScheme.value,
            )
        )
    }

    fun onNewColorSchemeSelected(
        newColorSchemeName: String,
    ) {
        _colorScheme.value = itemColorSchemeRepository
            .getItemColorSchemesByName()
            .getValue(newColorSchemeName)
            .also {
                log.debug {
                    "onNewColorSchemeSelected(): changing color scheme:" +
                            "\nnewColorSchemeName = $newColorSchemeName," +
                            "\ncolorScheme=$it"
                }
            }
    }

    fun onCurrencyClicked() {
        _events.tryEmit(
            Event.ProceedToCurrencySelection(
                currentCurrency = _currency.value,
            )
        )
    }

    fun onSaveClicked() {

        if (!isSaveEnabled.value) {
            return
        }

    }

    sealed interface Event {

        class ProceedToAccountTypeSelection(
            val currentType: Account.Type,
        ) : Event

        class ProceedToLogoCustomization(
            val currentTitle: String,
            val currentColorScheme: ItemColorScheme,
        ) : Event

        class ProceedToCurrencySelection(
            val currentCurrency: Currency,
        ) : Event
    }

    class Parameters(
        val accountToEditId: String?,
    )
}
