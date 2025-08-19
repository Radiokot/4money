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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.data.AccountsOfTypeWithTotal
import ua.com.radiokot.money.accounts.data.AccountsWithTotal
import ua.com.radiokot.money.accounts.logic.GetVisibleAccountsWithTotalUseCase
import ua.com.radiokot.money.accounts.logic.MoveAccountUseCase
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModel(
    accountRepository: AccountRepository,
    getVisibleAccountsWithTotalUseCase: GetVisibleAccountsWithTotalUseCase,
    private val moveAccountUseCase: MoveAccountUseCase,
) : ViewModel() {

    private val log by lazyLogger("AccountsVM")
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private val visibleAccountsWithTotalSharedFlow: SharedFlow<AccountsWithTotal> =
        getVisibleAccountsWithTotalUseCase()
            .shareIn(viewModelScope, SharingStarted.Eagerly)

    val accountListItems: StateFlow<List<ViewAccountListItem>> =
        visibleAccountsWithTotalSharedFlow
            .map { it.accountsOfTypes }
            .map { accountsOfTypes ->
                accountsOfTypes
                    .flatMap { (type, accountsOfType, totalInPrimaryCurrency) ->
                        listOf(
                            ViewAccountListItem.Header(
                                title = type.name,
                                amount = totalInPrimaryCurrency
                                    ?.let(::ViewAmount),
                                key = type.slug,
                            )
                        ) + accountsOfType.map(ViewAccountListItem::Account)
                    }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalAmountsPerCurrency: StateFlow<List<ViewAmount>> =
        visibleAccountsWithTotalSharedFlow
            .map { it.accountsOfTypes }
            .map { accountsOfType ->
                accountsOfType
                    .flatMap(AccountsOfTypeWithTotal::accountsOfType)
                    .groupBy(Account::currency)
                    .map { (currency, accountsInThisCurrency) ->
                        ViewAmount(
                            value = accountsInThisCurrency.sumOf { it.balance.value },
                            currency = currency,
                        )
                    }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalAmount: StateFlow<ViewAmount?> =
        visibleAccountsWithTotalSharedFlow
            .mapNotNull { accountsWithTotal ->
                accountsWithTotal
                    .totalInPrimaryCurrency
                    ?.let(::ViewAmount)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isArchiveVisible: StateFlow<Boolean> =
        accountRepository
            .getAccountsFlow()
            .map { it.any(Account::isArchived) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onAccountItemClicked(item: ViewAccountListItem.Account) {
        val account = item.source
        if (account == null) {
            log.warn {
                "onAccountItemClicked(): missing account source"
            }
            return
        }

        log.debug {
            "onAccountItemClicked(): proceeding to account actions:" +
                    "\naccount=$account"
        }

        _events.tryEmit(
            Event.ProceedToAccountActions(account)
        )
    }

    private var updatePositionJob: Job? = null
    fun onAccountItemMoved(
        itemToMove: ViewAccountListItem.Account,
        itemToPlaceBefore: ViewAccountListItem.Account?,
        itemToPlaceAfter: ViewAccountListItem.Account?,
    ) {
        val accountToMove = itemToMove.source
        if (accountToMove == null) {
            log.warn {
                "onAccountItemMoved(): missing moved account source"
            }
            return
        }

        val accountToPlaceBefore: Account? = itemToPlaceBefore?.source
        if (accountToPlaceBefore == null && itemToPlaceBefore != null) {
            log.warn {
                "onAccountItemMoved(): missing account to place before source"
            }
            return
        }

        val accountToPlaceAfter: Account? = itemToPlaceAfter?.source
        if (accountToPlaceAfter == null && itemToPlaceAfter != null) {
            log.warn {
                "onAccountItemMoved(): missing account to place after source"
            }
            return
        }

        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch {
            log.debug {
                "onAccountItemMoved(): moving:" +
                        "\naccount=$accountToMove," +
                        "\nbefore=$accountToPlaceBefore," +
                        "\nafter=$accountToPlaceAfter"
            }

            moveAccountUseCase(
                accountToMove = accountToMove,
                accountToPlaceBefore = accountToPlaceBefore,
                accountToPlaceAfter = accountToPlaceAfter,
            )
                .onFailure { error ->
                    log.error(error) {
                        "onAccountItemMoved(): failed to move"
                    }
                }
                .onSuccess {
                    log.info {
                        "Moved account $accountToMove " +
                                "to be between $accountToPlaceAfter and $accountToPlaceBefore"
                    }

                    log.debug {
                        "onAccountItemMoved(): moved successfully"
                    }
                }
        }
    }

    fun onAddClicked() {
        log.debug {
            "onAddClicked(): proceeding to adding new account"
        }

        _events.tryEmit(Event.ProceedToAccountAdd)
    }

    fun onArchiveClicked() {
        log.debug {
            "onArchiveClicked(): proceeding to archive"
        }

        _events.tryEmit(Event.ProceedToArchivedAccounts)
    }

    sealed interface Event {

        class ProceedToAccountActions(
            val account: Account,
        ) : Event

        object ProceedToAccountAdd : Event

        object ProceedToArchivedAccounts : Event
    }
}
