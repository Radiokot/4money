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
import cash.z.ecc.android.bip39.toSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.auth.logic.AuthenticateWithPhraseUseCase
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger

@OptIn(ExperimentalCoroutinesApi::class)
class PhraseAuthScreenViewModel(
    private val authenticateWithPhraseUseCase: AuthenticateWithPhraseUseCase,
) : ViewModel() {

    private val log by lazyLogger("PhraseAuthScreenVM")
    private val _phrase: MutableStateFlow<String> = MutableStateFlow("")
    val phrase: StateFlow<String> = _phrase.asStateFlow()
    private val isSigningIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private val phraseSeed: StateFlow<ByteArray?> =
        _phrase
            .mapLatest { phrase ->
                runCatching { Mnemonics.MnemonicCode(phrase).toSeed() }
                    .getOrNull()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isSignInEnabled: StateFlow<Boolean> =
        combine(
            isSigningIn,
            phraseSeed,
            transform = ::Pair,
        )
            .map { (isSigningIn, entropy) ->
                entropy != null && !isSigningIn
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onPhraseChanged(newPhrase: String) {
        _phrase.value = newPhrase
    }

    fun onSignInClicked() {

        if (!isSignInEnabled.value) {
            return
        }

        signIn()
    }

    private var signInJob: Job? = null
    private fun signIn() {

        val phraseSeed = phraseSeed.value
            ?: error("Seed must be available at this moment")

        signInJob?.cancel()
        signInJob = viewModelScope.launch {

            isSigningIn.value = true

            log.debug {
                "signIn(): signing in"
            }

            authenticateWithPhraseUseCase
                .invoke(
                    phraseSeed = phraseSeed,
                )
                .onSuccess {
                    log.debug {
                        "signIn(): signed in successfully"
                    }

                    _events.tryEmit(Event.Done)
                }
                .onFailure { error ->
                    log.error(error) {
                        "signIn(): failed to sign in"
                    }

                    _events.tryEmit(
                        Event.ShowAuthError(
                            technicalReason = error::class.simpleName ?: error.toString(),
                        )
                    )
                }

            isSigningIn.value = false
        }
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    sealed interface Event {

        object Done : Event

        object Close : Event

        class ShowAuthError(
            val technicalReason: String,
        ) : Event
    }
}
