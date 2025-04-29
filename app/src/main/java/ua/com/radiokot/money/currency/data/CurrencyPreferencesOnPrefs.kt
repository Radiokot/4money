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

package ua.com.radiokot.money.currency.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class CurrencyPreferencesOnPrefs(
    private val sharedPreferences: SharedPreferences,
) : CurrencyPreferences {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        coroutineScope.launch {
            val versionKey = "version"
            sharedPreferences.edit {
                putInt(versionKey, 1)
            }
        }
    }

    override val primaryCurrencyCode: MutableStateFlow<String> by lazy {
        val key = "primary_currency_code"
        MutableStateFlow(sharedPreferences.getString(key, "USD")!!).apply {
            coroutineScope.launch {
                drop(1).collect { newValue ->
                    sharedPreferences.edit {
                        putString(key, newValue)
                    }
                }
            }
        }
    }
}
