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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.logic.UpdateAccountBalanceUseCase
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AccountActionSheetViewModel(
    private val accountRepository: AccountRepository,
    private val updateAccountBalanceUseCase: UpdateAccountBalanceUseCase,
) : ViewModel() {

    private val log by lazyLogger("AccountActionSheetVM")
    private val _mode: MutableStateFlow<ViewAccountActionSheetMode> =
        MutableStateFlow(ViewAccountActionSheetMode.Actions)
    val mode = _mode.asStateFlow()
    private val _balanceInputValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(BigInteger.ZERO)
    val balanceInputValue = _balanceInputValue.asStateFlow()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events
    private val requestedAccountId: MutableStateFlow<String?> = MutableStateFlow(null)

    private lateinit var account: Account
    private val accountSharedFlow: SharedFlow<Account> =
        requestedAccountId
            .filterNotNull()
            .flatMapLatest { accountId ->
                accountRepository
                    .getAccountFlow(accountId)
            }
            .onEach { freshAccount ->
                account = freshAccount
                _balanceInputValue.emit(freshAccount.balance)
            }
            .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    val accountDetails: StateFlow<ViewAccountDetails?> =
        accountSharedFlow
            .filterNotNull()
            .map(::ViewAccountDetails)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setAccount(accountId: String) {
        log.debug {
            "setAccount(): setting: " +
                    "\naccountId=$accountId"
        }

        requestedAccountId.tryEmit(accountId)
    }

    fun onBalanceClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onBalanceClicked(): ignoring as not in actions mode"
            }
            return
        }

        log.debug {
            "onBalanceClicked(): switching mode to balance editing"
        }

        _mode.tryEmit(ViewAccountActionSheetMode.Balance)
        _balanceInputValue.tryEmit(account.balance)
    }

    fun onTransferClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onTransferClicked(): ignoring as not in actions mode"
            }
            return
        }

        _events.tryEmit(
            Event.ProceedToTransfer(
                sourceAccountId = TransferCounterpartyId.Account(account.id),
            )
        )
    }

    fun onIncomeClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onIncomeClicked(): ignoring as not in actions mode"
            }
            return
        }

        _events.tryEmit(
            Event.ProceedToIncome(
                destinationAccountId = TransferCounterpartyId.Account(account.id),
            )
        )
    }

    fun onExpenseClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onExpenseClicked(): ignoring as not in actions mode"
            }
            return
        }

        _events.tryEmit(
            Event.ProceedToExpense(
                sourceAccountId = TransferCounterpartyId.Account(account.id),
            )
        )
    }

    fun onActivityClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onActivityClicked(): ignoring as not in actions mode"
            }
            return
        }

        _events.tryEmit(
            Event.ProceedToFilteredActivity(
                accountCounterparty = TransferCounterparty.Account(
                    account = account,
                )
            )
        )
    }

    fun onNewBalanceInputValueParsed(newValue: BigInteger) {
        log.debug {
            "onNewBalanceInputValueParsed(): updating balance input value: " +
                    "\nnewValue=$newValue"
        }

        _balanceInputValue.tryEmit(newValue)
    }

    fun onBalanceInputSubmit() {
        updateAccountBalance()
    }

    private var balanceUpdateJob: Job? = null
    private fun updateAccountBalance() {
        val accountId = account.id
        val newValue = _balanceInputValue.value

        balanceUpdateJob?.cancel()
        balanceUpdateJob = viewModelScope.launch {
            log.debug {
                "updateAccountBalance(): updating:" +
                        "\naccountId=$accountId," +
                        "\nnewValue=$newValue"
            }

            updateAccountBalanceUseCase(
                accountId = accountId,
                newValue = newValue,
            )
                .onFailure { error ->
                    log.error(error) {
                        "updateAccountBalance(): failed to update balance"
                    }
                }
                .onSuccess {
                    log.debug {
                        "updateAccountBalance(): balance updated, posting event"
                    }

                    _events.tryEmit(Event.BalanceUpdated)
                }
        }
    }

    sealed interface Event {

        class ProceedToIncome(
            val destinationAccountId: TransferCounterpartyId.Account,
        ) : Event

        class ProceedToExpense(
            val sourceAccountId: TransferCounterpartyId.Account,
        ) : Event

        class ProceedToTransfer(
            val sourceAccountId: TransferCounterpartyId.Account,
        ) : Event

        class ProceedToFilteredActivity(
            val accountCounterparty: TransferCounterparty.Account,
        ) : Event

        object BalanceUpdated : Event
    }
}
