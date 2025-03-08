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
import kotlinx.coroutines.flow.combine
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
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.categories.view.viewCategoryItemListFlow
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AccountActionSheetViewModel(
    private val accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    historyStatsRepository: HistoryStatsRepository,
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

    val incomeCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        viewCategoryItemListFlow(
            isIncome = true,
            period = HistoryPeriod.Month(),
            categoryRepository = categoryRepository,
            historyStatsRepository = historyStatsRepository,
        ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val expenseCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        viewCategoryItemListFlow(
            isIncome = false,
            period = HistoryPeriod.Month(),
            categoryRepository = categoryRepository,
            historyStatsRepository = historyStatsRepository,
        ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val accountDetails: StateFlow<ViewAccountDetails?> =
        accountSharedFlow
            .filterNotNull()
            .map(::ViewAccountDetails)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val otherAccountListItems: StateFlow<List<ViewAccountListItem>> =
        accountRepository
            .getAccountsFlow()
            .combine(accountSharedFlow, ::Pair)
            .map { (accounts, currentAccount) ->
                accounts
                    .filter { it != currentAccount }
                    .sortedBy(Account::title)
                    .map(ViewAccountListItem::Account)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

        log.debug {
            "onTransferClicked(): switching mode to transfer destination selection"
        }

        _mode.tryEmit(ViewAccountActionSheetMode.TransferDestination)
    }

    fun onTransferCounterpartyAccountItemClicked(item: ViewAccountListItem.Account) {
        val clickedAccount = item.source
        if (clickedAccount == null) {
            log.warn {
                "onTransferCounterpartyAccountItemClicked(): ignoring as account is null"
            }
            return
        }

        selectClickedCounterpartyForTransfer(
            clickedCounterparty = TransferCounterparty.Account(clickedAccount)
        )
    }

    fun onIncomeClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onIncomeClicked(): ignoring as not in actions mode"
            }
            return
        }

        log.debug {
            "onIncomeSourceClicked(): switching mode to income source selection"
        }

        _mode.tryEmit(ViewAccountActionSheetMode.IncomeSource)
    }

    fun onExpenseClicked() {
        if (mode.value != ViewAccountActionSheetMode.Actions) {
            log.debug {
                "onExpenseClicked(): ignoring as not in actions mode"
            }
            return
        }

        log.debug {
            "onExpenseDestinationClicked(): switching mode to expense destination selection"
        }

        _mode.tryEmit(ViewAccountActionSheetMode.ExpenseDestination)
    }

    fun onTransferCounterpartyCategoryItemClicked(
        item: ViewCategoryListItem,
    ) = viewModelScope.launch {

        val clickedCategory = item.source
        if (clickedCategory == null) {
            log.warn {
                "onTransferCounterpartyCategoryItemClicked(): ignoring as category is null"
            }
            return@launch
        }

        selectClickedCounterpartyForTransfer(
            clickedCounterparty = TransferCounterparty.Category(
                category = clickedCategory,
                subcategory = null,
            ),
        )
    }

    private fun selectClickedCounterpartyForTransfer(clickedCounterparty: TransferCounterparty) {
        val mode = mode.value

        val source: TransferCounterparty =
            if (mode == ViewAccountActionSheetMode.IncomeSource)
                clickedCounterparty
            else
                TransferCounterparty.Account(account)

        val destination: TransferCounterparty =
            if (mode == ViewAccountActionSheetMode.IncomeSource)
                TransferCounterparty.Account(account)
            else
                clickedCounterparty

        log.debug {
            "selectClickedCounterpartyForTransfer(): posting selection:" +
                    "\nmode=$mode," +
                    "\nsource=$source," +
                    "\ndestination=$destination"
        }

        _events.tryEmit(
            Event.TransferCounterpartiesSelected(
                source = source,
                destination = destination,
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
        class TransferCounterpartiesSelected(
            val source: TransferCounterparty,
            val destination: TransferCounterparty,
        ) : Event

        object BalanceUpdated : Event
    }
}
