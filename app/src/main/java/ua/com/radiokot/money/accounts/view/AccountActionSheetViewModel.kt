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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.logic.UnarchiveAccountUseCase
import ua.com.radiokot.money.accounts.logic.UpdateAccountBalanceUseCase
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import java.math.BigInteger

class AccountActionSheetViewModel(
    parameters: Parameters,
    accountRepository: AccountRepository,
    private val updateAccountBalanceUseCase: UpdateAccountBalanceUseCase,
    private val unarchiveAccountUseCase: UnarchiveAccountUseCase,
) : ViewModel() {

    private val log by lazyLogger("AccountActionSheetVM")

    private val account: Account = runBlocking {
        accountRepository
            .getAccount(
                accountId = parameters.accountId,
            )
            ?: error("Account not found")
    }

    val title: String =
        account.title

    val balance: ViewAmount =
        ViewAmount(
            value = account.balance,
            currency = account.currency,
        )

    private val _mode: MutableStateFlow<ViewAccountActionSheetMode> =
        MutableStateFlow(
            if (account.isArchived)
                ViewAccountActionSheetMode.ArchivedActions
            else
                ViewAccountActionSheetMode.DefaultActions
        )
    val mode = _mode.asStateFlow()

    private val _balanceInputValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(account.balance)
    val balanceInputValue = _balanceInputValue.asStateFlow()

    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events

    fun onBalanceClicked() {
        log.debug {
            "onBalanceClicked(): switching mode to balance editing"
        }

        _mode.tryEmit(ViewAccountActionSheetMode.Balance)
        _balanceInputValue.tryEmit(account.balance)
    }

    fun onTransferClicked() {
        _events.tryEmit(
            Event.ProceedToTransfer(
                sourceAccountId = TransferCounterpartyId.Account(account.id),
            )
        )
    }

    fun onIncomeClicked() {
        _events.tryEmit(
            Event.ProceedToIncome(
                destinationAccountId = TransferCounterpartyId.Account(account.id),
            )
        )
    }

    fun onExpenseClicked() {
        _events.tryEmit(
            Event.ProceedToExpense(
                sourceAccountId = TransferCounterpartyId.Account(account.id),
            )
        )
    }

    fun onActivityClicked() {
        _events.tryEmit(
            Event.ProceedToFilteredActivity(
                accountCounterparty = TransferCounterparty.Account(
                    account = account,
                )
            )
        )
    }

    fun onEditClicked() {
        _events.tryEmit(
            Event.ProceedToEdit(
                accountId = account.id,
            )
        )
    }

    fun onUnarchiveClicked() {
        unarchiveAccount()
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

                    _events.tryEmit(Event.Done)
                }
        }
    }

    private var unarchiveJob: Job? = null
    private fun unarchiveAccount() {
        unarchiveJob?.cancel()
        unarchiveJob = viewModelScope.launch {

            log.debug {
                "unarchiveAccount(): unarchiving:" +
                        "\naccount=$account"
            }

            unarchiveAccountUseCase
                .invoke(
                    accountToUnrachive = account,
                )
                .onFailure { error ->
                    log.error(error) {
                        "unarchiveAccount(): failed to unarchive account"
                    }
                }
                .onSuccess {
                    log.info {
                        "Unarchived account $account"
                    }

                    log.debug {
                        "unarchiveAccount(): account unarchived"
                    }

                    _events.emit(Event.Done)
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

        class ProceedToEdit(
            val accountId: String,
        ) : Event

        class ProceedToFilteredActivity(
            val accountCounterparty: TransferCounterparty.Account,
        ) : Event

        object Done : Event
    }

    class Parameters(
        val accountId: String,
    )
}
