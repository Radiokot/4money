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

package ua.com.radiokot.money.preferences.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.lazyLogger

class PreferencesScreenViewModel(
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    private val log by lazyLogger("PreferencesScreenVM")
    private val _primaryCurrencyCodeValue: MutableStateFlow<String> =
        MutableStateFlow(currencyPreferences.primaryCurrencyCode.value)
    val primaryCurrencyCodeValue = _primaryCurrencyCodeValue.asStateFlow()

    val isSaveCurrencyPreferencesEnabled: StateFlow<Boolean> =
        combine(
            primaryCurrencyCodeValue,
            currencyPreferences.primaryCurrencyCode,
            transform = ::Pair,
        )
            .map { (enteredPrimaryCurrencyCode, savedPrimaryCurrencyCode) ->
                enteredPrimaryCurrencyCode.isNotBlank()
                        && enteredPrimaryCurrencyCode != savedPrimaryCurrencyCode
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onPrimaryCurrencyCodeChanged(newValue: String) {
        _primaryCurrencyCodeValue.value = newValue
    }

    fun onSaveCurrencyPreferencesClicked() {
        if (!isSaveCurrencyPreferencesEnabled.value) {
            log.warn {
                "onSaveCurrencyPreferencesClicked(): ignoring as save is not enabled"
            }
            return
        }

        currencyPreferences.primaryCurrencyCode.value = primaryCurrencyCodeValue.value
    }
}
