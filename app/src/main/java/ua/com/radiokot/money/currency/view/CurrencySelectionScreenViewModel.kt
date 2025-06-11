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

package ua.com.radiokot.money.currency.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger

class CurrencySelectionScreenViewModel(
    private val parameters: Parameters,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val log by lazyLogger("CurrencySelectionScreenVM")
    private val selectedCurrency: MutableStateFlow<Currency?> = MutableStateFlow(null)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val itemList: StateFlow<List<CurrencySelectionListItem>> =
        combine(
            currencyRepository.getCurrenciesFlow(),
            selectedCurrency,
            transform = ::Pair,
        )
            .map { (currencies, selectedCurrency) ->

                currencies.map { currency ->
                    CurrencySelectionListItem(
                        currency = currency,
                        isSelected = currency == selectedCurrency,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            currencyRepository
                .getCurrencyByCode(parameters.selectedCurrencyCode)
                .also(selectedCurrency::tryEmit)
        }
    }

    fun onItemClicked(clickedItem: CurrencySelectionListItem) {
        clickedItem
            .source
            ?.also(selectedCurrency::tryEmit)
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    fun onSaveClicked() {
        val selectedCurrency = selectedCurrency.value
        if (selectedCurrency == null) {
            log.warn {
                "onSaveClicked(): missing selected currency"
            }
            return
        }

        _events.tryEmit(
            Event.Done(
                selectedCurrency = selectedCurrency,
            )
        )
    }

    sealed interface Event {

        object Close : Event

        class Done(
            val selectedCurrency: Currency,
        ) : Event
    }

    class Parameters(
        val selectedCurrencyCode: String,
    )
}
