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

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ua.com.radiokot.money.BuildConfig
import ua.com.radiokot.money.auth.logic.SignInWithCredentialsUseCase
import ua.com.radiokot.money.eventSharedFlow

class TempAuthScreenViewModel(
    private val signInUseCase: SignInWithCredentialsUseCase,
) : ViewModel() {

    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    fun onAuthenticateClicked() {
        startAuth()
    }

    private var authStartJob: Job? = null
    private fun startAuth() {
        authStartJob?.cancel()
        authStartJob = viewModelScope.launch {
            signInUseCase
                .start(
                    // TODO Use actual credential.
                    login = BuildConfig.AUTH_TEMP_CREDENTIAL,
                    password = BuildConfig.AUTH_TEMP_CREDENTIAL,
                )
                .onSuccess { result ->
                    when (result) {
                        SignInWithCredentialsUseCase.StartResult.Done -> {
                            onAuthorizedSuccessfully()
                        }

                        SignInWithCredentialsUseCase.StartResult.Started -> {
                            // Ok.
                        }
                    }
                }
                .onFailure { error ->
                    error.printStackTrace()
                    _events.emit(
                        Event.ShowAuthError(
                            technicalReason = error::class.simpleName ?: error.toString(),
                        )
                    )
                }
        }
    }

    fun onNewIntent(intent: Intent) {
        if (authStartJob?.isCompleted == true) {
            finishAuth(intent)
        }
    }

    private var authFinishJob: Job? = null
    private fun finishAuth(resultIntent: Intent) {
        authFinishJob?.cancel()
        authFinishJob = viewModelScope.launch {
            signInUseCase
                .finish(resultIntent)
                .onSuccess { isAuthorized ->
                    if (isAuthorized) {
                        onAuthorizedSuccessfully()
                    }
                }
                .onFailure { error ->
                    error.printStackTrace()
                    _events.emit(
                        Event.ShowAuthError(
                            technicalReason = error::class.simpleName ?: error.toString()
                        )
                    )
                }
        }
    }

    private suspend fun onAuthorizedSuccessfully() {
        _events.emit(Event.Done)
    }

    fun onPhraseClicked() {
        _events.tryEmit(Event.ProceedToPhraseAuth)
    }

    sealed interface Event {
        object Done : Event

        object ProceedToPhraseAuth : Event

        class ShowAuthError(
            val technicalReason: String,
        ) : Event
    }
}
