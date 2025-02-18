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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.logic.UpdateAccountBalanceUseCase
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import java.math.BigInteger

class AccountActionSheetViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val updateAccountBalanceUseCase: UpdateAccountBalanceUseCase,
) : ViewModel() {

    private val log by lazyLogger("AccountActionSheetVM")
    private val _accountDetails: MutableStateFlow<ViewAccountDetails?> = MutableStateFlow(null)
    val accountDetails = _accountDetails
    private val _isOpened: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOpened = _isOpened.asStateFlow()
    private val _mode: MutableStateFlow<ViewAccountActionSheetMode> =
        MutableStateFlow(ViewAccountActionSheetMode.Actions)
    val mode = _mode.asStateFlow()
    private val _balanceInputValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(BigInteger.ZERO)
    val balanceInputValue = _balanceInputValue.asStateFlow()
    private val _otherAccountListItems: MutableStateFlow<List<ViewAccountListItem>> =
        MutableStateFlow(emptyList())
    val otherAccountListItems = _otherAccountListItems.asStateFlow()
    private val _incomeCategoryListItems: MutableStateFlow<List<ViewCategoryListItem>> =
        MutableStateFlow(emptyList())
    val incomeCategoryListItems = _incomeCategoryListItems.asStateFlow()
    private val _expenseCategoryListItems: MutableStateFlow<List<ViewCategoryListItem>> =
        MutableStateFlow(emptyList())
    val expenseCategoryListItems = _expenseCategoryListItems.asStateFlow()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events
    private var accountSubscriptionJob: Job? = null
    private lateinit var account: Account

    init {
        viewModelScope.launch {
            subscribeToCategories()
        }
    }

    private suspend fun subscribeToCategories() {
        categoryRepository
            .getCategoriesFlow()
            .map { categories ->
                categories.sortedBy(Category::title)
            }
            .collect { categories ->
                _incomeCategoryListItems.tryEmit(
                    categories
                        .filter(Category::isIncome)
                        .map(::ViewCategoryListItem)
                )
                _expenseCategoryListItems.tryEmit(
                    categories
                        .filterNot(Category::isIncome)
                        .map(::ViewCategoryListItem)
                )
            }
    }

    fun open(account: Account) {
        log.debug {
            "open(): opening: " +
                    "\naccount=$account"
        }

        accountSubscriptionJob?.cancel()
        accountSubscriptionJob = viewModelScope.launch {
            // Subscribe to fresh account details.
            launch {
                accountRepository
                    .getAccountFlow(account.id)
                    .onStart { emit(account) }
                    .collect { freshAccount ->
                        this@AccountActionSheetViewModel.account = freshAccount
                        _accountDetails.emit(ViewAccountDetails(freshAccount))
                    }
            }

            // Subscribe to fresh destinations (all accounts except the current one).
            launch {
                accountRepository
                    .getAccountsFlow()
                    .map { accounts ->
                        accounts
                            .filter { it != this@AccountActionSheetViewModel.account }
                            .sortedBy(Account::title)
                            .map(ViewAccountListItem::Account)
                    }
                    .collect(_otherAccountListItems)
            }
        }

        _mode.tryEmit(ViewAccountActionSheetMode.Actions)
        _isOpened.tryEmit(true)
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

        log.debug {
            "onTransferCounterpartyAccountItemClicked(): opening transfer:" +
                    "\ncurrentAccount=$account" +
                    "\ndestinationAccount=$clickedAccount"
        }

        _events.tryEmit(
            Event.OpenTransfer(
                sourceAccount = account,
                destinationAccount = clickedAccount,
            )
        )

        close()
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

    fun onTransferCounterpartyCategoryItemClicked(item: ViewCategoryListItem) {
        val clickedCategory = item.source
        if (clickedCategory == null) {
            log.warn {
                "onTransferCounterpartyCategoryItemClicked(): ignoring as category is null"
            }
            return
        }

    }

    fun onBackPressed() {
        val isOpened = _isOpened.value
        if (!isOpened) {
            log.warn {
                "onBackPressed(): ignoring back press as the sheet is already closed"
            }
            return
        }

        close()
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
                        "updateAccountBalance(): balance updated, closing"
                    }

                    _isOpened.emit(false)
                }
        }
    }

    private fun close() {
        log.debug {
            "close(): closing"
        }

        accountSubscriptionJob?.cancel()
        _isOpened.tryEmit(false)
    }

    sealed interface Event {
        class OpenTransfer(
            val sourceAccount: Account,
            val destinationAccount: Account,
        ) : Event
    }
}
