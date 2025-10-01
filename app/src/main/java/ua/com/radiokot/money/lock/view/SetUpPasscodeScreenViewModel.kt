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

package ua.com.radiokot.money.lock.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.lock.logic.EnableAppLockUseCase

class SetUpPasscodeScreenViewModel(
    private val enableAppLockUseCase: EnableAppLockUseCase,
) : ViewModel() {

    private val log by lazyLogger("SetUpPasscodeScreenVM")

    val passcodeLength = 4
    private val _isRepeating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRepeating = _isRepeating.asStateFlow()
    private val _passcode: MutableStateFlow<String> = MutableStateFlow("")
    val passcode = _passcode.asStateFlow()
    private var passcodeToMatch = ""
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    fun onPasscodeChanged(
        newValue: String,
    ) {
        _passcode.tryEmit(newValue)

        if (newValue.length == passcodeLength) {
            _passcode.tryEmit("")

            if (!isRepeating.value) {
                passcodeToMatch = newValue
                _isRepeating.tryEmit(true)
            } else {
                if (newValue == passcodeToMatch) {
                    enableAppLock(newValue)
                } else {
                    _isRepeating.value = false
                    _events.tryEmit(Event.ShowMismatchError)
                }
            }
        }
    }

    private var enableAppLockJob: Job? = null
    private fun enableAppLock(
        passcode: String,
    ) {
        enableAppLockJob?.cancel()
        enableAppLockJob = viewModelScope.launch {
            log.debug {
                "enableAppLock(): enabling:" +
                        "\npasscode=$passcode"
            }

            enableAppLockUseCase
                .invoke(
                    passcode = passcode,
                )
                .onSuccess {
                    log.debug {
                        "enableAppLock(): successfully enabled"
                    }

                    _events.emit(Event.Done)
                }
                .onFailure { error ->
                    log.error(error) {
                        "enableAppLock(): failed to enable"
                    }
                }
        }
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    sealed interface Event {

        object ShowMismatchError : Event

        object Close : Event

        object Done : Event
    }
}
