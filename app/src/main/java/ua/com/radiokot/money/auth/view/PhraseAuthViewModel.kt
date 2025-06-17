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

package ua.com.radiokot.money.auth.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.map

@OptIn(ExperimentalCoroutinesApi::class)
class PhraseAuthViewModel(

) : ViewModel() {

    private val _phrase: MutableStateFlow<String> = MutableStateFlow("")
    val phrase: StateFlow<String> = _phrase.asStateFlow()

    private val entropy: StateFlow<ByteArray?> =
        _phrase
            .mapLatest { phrase ->
                runCatching { Mnemonics.MnemonicCode(phrase).toEntropy() }
                    .getOrNull()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isSignInEnabled: StateFlow<Boolean> =
        entropy
            .map(viewModelScope) { entropy ->
                entropy != null
            }

    fun onPhraseChanged(newPhrase: String) {
        _phrase.value = newPhrase
    }

    fun onSignInClicked() {

        if (!isSignInEnabled.value) {
            return
        }
    }
}
