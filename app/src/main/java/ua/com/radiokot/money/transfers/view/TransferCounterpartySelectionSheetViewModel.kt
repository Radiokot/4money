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

package ua.com.radiokot.money.transfers.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.view.ViewAccountListItem
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.categories.view.viewCategoryItemListFlow
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCounterpartySelectionSheetViewModel(
    accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val historyStatsRepository: HistoryStatsRepository,
) : ViewModel() {

    private val log by lazyLogger("TransferCounterpartySelectionSheetVM")
    private val _isIncognito: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isIncognito = _isIncognito.asStateFlow()
    private val _isForSource: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isForSource = _isForSource.asStateFlow()
    private val _areAccountsVisible: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val areAccountsVisible = _areAccountsVisible.asStateFlow()
    private val alreadySelectedCounterpartyId: MutableSharedFlow<TransferCounterpartyId?> =
        MutableSharedFlow(replay = 1)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val areIncomeCategoriesVisible: StateFlow<Boolean?> =
        combine(
            isForSource,
            alreadySelectedCounterpartyId,
            transform = ::Pair
        )
            .map { (isForSource, alreadySelectedCounterpartyId) ->
                isForSource == null
                        || isForSource == true && alreadySelectedCounterpartyId !is TransferCounterpartyId.Category
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val areExpenseCategoriesVisible: StateFlow<Boolean?> =
        combine(
            isForSource,
            alreadySelectedCounterpartyId,
            transform = ::Pair
        )
            .map { (isForSource, alreadySelectedCounterpartyId) ->
                isForSource == null
                        || isForSource == false && alreadySelectedCounterpartyId !is TransferCounterpartyId.Category
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val accountListItems: StateFlow<List<ViewAccountListItem>> =
        areAccountsVisible
            .filterNotNull()
            .flatMapLatest { areAccountsVisible ->
                if (areAccountsVisible)
                    combine(
                        accountRepository.getAccountsFlow(),
                        alreadySelectedCounterpartyId,
                        _isIncognito,
                        transform = ::Triple
                    )
                        .map { (accounts, alreadySelectedCounterpartyId, isIncognito) ->
                            accounts
                                .sortedBy(Account::title)
                                .filterNot { it.id == alreadySelectedCounterpartyId.toString() }
                                .map { account ->
                                    ViewAccountListItem.Account(
                                        account = account,
                                        isIncognito = isIncognito,
                                    )
                                }
                        }
                else
                    flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val incomeCategoryListItems: StateFlow<List<ViewCategoryListItem>> =
        combine(
            areIncomeCategoriesVisible,
            _isIncognito,
            transform = ::Pair
        )
            .flatMapLatest { (areCategoriesVisible, isIncognito) ->
                if (areCategoriesVisible == true)
                    if (!isIncognito)
                        viewCategoryItemListFlow(
                            isIncome = true,
                            period = HistoryPeriod.Month(),
                            categoryRepository = categoryRepository,
                            historyStatsRepository = historyStatsRepository,
                        )
                    else
                        viewCategoryItemListFlow(
                            isIncome = true,
                            categoryRepository = categoryRepository,
                        )
                else
                    flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val expenseCategoryListItems: StateFlow<List<ViewCategoryListItem>> =
        combine(
            areExpenseCategoriesVisible,
            _isIncognito,
            transform = ::Pair
        )
            .flatMapLatest { (areCategoriesVisible, isIncognito) ->
                if (areCategoriesVisible == true)
                    if (!isIncognito)
                        viewCategoryItemListFlow(
                            isIncome = false,
                            period = HistoryPeriod.Month(),
                            categoryRepository = categoryRepository,
                            historyStatsRepository = historyStatsRepository,
                        )
                    else
                        viewCategoryItemListFlow(
                            isIncome = false,
                            categoryRepository = categoryRepository,
                        )
                else
                    flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setParameters(
        isIncognito: Boolean,
        isForSource: Boolean?,
        showAccounts: Boolean,
        alreadySelectedCounterpartyId: TransferCounterpartyId?,
    ) {
        log.debug {
            "setParameters(): setting:" +
                    "\nisIncognito=$isIncognito" +
                    "\nisForSource=$isForSource" +
                    "\nshowAccounts=$showAccounts" +
                    "\nalreadySelectedCounterpartyId=$alreadySelectedCounterpartyId"
        }

        this._isIncognito.tryEmit(isIncognito)
        this._isForSource.tryEmit(isForSource)
        this._areAccountsVisible.tryEmit(showAccounts)
        this.alreadySelectedCounterpartyId.tryEmit(alreadySelectedCounterpartyId)
    }

    fun onAccountItemClicked(item: ViewAccountListItem.Account) {
        val account = item.source
        if (account == null) {
            log.warn {
                "onAccountItemClicked(): missing account source"
            }
            return
        }

        log.debug {
            "onAccountItemClicked(): posting selected:" +
                    "\naccount=$account"
        }

        _events.tryEmit(
            Event.CounterpartySelected(TransferCounterparty.Account(account))
        )
    }

    fun onCategoryItemClicked(item: ViewCategoryListItem) {
        val category = item.source
        if (category == null) {
            log.warn {
                "onCategoryItemClicked(): missing category source"
            }
            return
        }

        log.debug {
            "onCategoryItemClicked(): posting selected:" +
                    "\ncategory=$category"
        }

        _events.tryEmit(
            Event.CounterpartySelected(TransferCounterparty.Category(category))
        )
    }

    sealed interface Event {

        class CounterpartySelected(
            val counterparty: TransferCounterparty,
        ) : Event
    }
}
