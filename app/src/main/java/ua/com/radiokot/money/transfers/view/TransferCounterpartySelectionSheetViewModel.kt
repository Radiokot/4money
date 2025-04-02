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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.view.ViewAccountListItem
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.CategoryStats
import ua.com.radiokot.money.categories.logic.GetCategoryStatsUseCase
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.categories.view.toSortedIncognitoViewItemList
import ua.com.radiokot.money.categories.view.toSortedViewItemList
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCounterpartySelectionSheetViewModel(
    accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val getCategoryStatsUseCase: GetCategoryStatsUseCase,
) : ViewModel() {

    private val log by lazyLogger("TransferCounterpartySelectionSheetVM")
    private val _isIncognito: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isIncognito = _isIncognito.asStateFlow()
    private val _isForSource: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isForSource = _isForSource.asStateFlow()
    private val _areAccountsVisible: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val areAccountsVisible = _areAccountsVisible.asStateFlow()
    private val areCategoriesVisible: MutableSharedFlow<Boolean> =
        MutableSharedFlow(replay = 1)
    private val alreadySelectedCounterpartyId: MutableSharedFlow<TransferCounterpartyId?> =
        MutableSharedFlow(replay = 1)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val areIncomeCategoriesVisible: StateFlow<Boolean?> =
        combine(
            areCategoriesVisible,
            isForSource,
            alreadySelectedCounterpartyId,
            transform = ::Triple
        )
            .map { (areCategoriesVisible, isForSource, alreadySelectedCounterpartyId) ->
                areCategoriesVisible && (
                        isForSource == null
                                || (isForSource == true
                                && alreadySelectedCounterpartyId !is TransferCounterpartyId.Category)
                        )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val areExpenseCategoriesVisible: StateFlow<Boolean?> =
        combine(
            areCategoriesVisible,
            isForSource,
            alreadySelectedCounterpartyId,
            transform = ::Triple
        )
            .map { (areCategoriesVisible, isForSource, alreadySelectedCounterpartyId) ->
                areCategoriesVisible && (
                        isForSource == null
                                || (isForSource == false
                                && alreadySelectedCounterpartyId !is TransferCounterpartyId.Category)
                        )
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
                            val alreadySelectedCounterpartyIdString =
                                alreadySelectedCounterpartyId.toString()

                            buildList {
                                accounts
                                    .sorted()
                                    .forEach { account ->
                                        if (account.id != alreadySelectedCounterpartyIdString) {
                                            add(
                                                ViewAccountListItem.Account(
                                                    account = account,
                                                    isIncognito = isIncognito,
                                                )
                                            )
                                        }
                                    }
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
                        getCategoryStatsUseCase(
                            isIncome = true,
                            period = HistoryPeriod.Month(),
                        )
                            .map(List<CategoryStats>::toSortedViewItemList)
                    else
                        categoryRepository
                            .getCategoriesFlow(
                                isIncome = true,
                            )
                            .map { categories ->
                                categories.toSortedIncognitoViewItemList()
                            }
                else
                    flowOf(emptyList())
            }
            .flowOn(Dispatchers.Default)
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
                        getCategoryStatsUseCase(
                            isIncome = false,
                            period = HistoryPeriod.Month(),
                        )
                            .map(List<CategoryStats>::toSortedViewItemList)
                    else
                        categoryRepository
                            .getCategoriesFlow(
                                isIncome = false,
                            )
                            .map { categories ->
                                categories.toSortedIncognitoViewItemList()
                            }
                else
                    flowOf(emptyList())
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setParameters(
        isIncognito: Boolean,
        isForSource: Boolean?,
        showAccounts: Boolean,
        showCategories: Boolean,
        alreadySelectedCounterpartyId: TransferCounterpartyId?,
    ) {
        log.debug {
            "setParameters(): setting:" +
                    "\nisIncognito=$isIncognito," +
                    "\nisForSource=$isForSource," +
                    "\nshowAccounts=$showAccounts," +
                    "\nshowCategories=$showCategories," +
                    "\nalreadySelectedCounterpartyId=$alreadySelectedCounterpartyId"
        }

        this._isIncognito.tryEmit(isIncognito)
        this._isForSource.tryEmit(isForSource)
        this._areAccountsVisible.tryEmit(showAccounts)
        this.areCategoriesVisible.tryEmit(showCategories)
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

        postResult(
            selected = TransferCounterparty.Account(account),
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

        postResult(
            selected = TransferCounterparty.Category(category),
        )
    }

    private fun postResult(selected: TransferCounterparty) = viewModelScope.launch {
        val result = TransferCounterpartySelectionResult(
            otherSelectedCounterpartyId = this@TransferCounterpartySelectionSheetViewModel
                .alreadySelectedCounterpartyId
                .first(),
            isSelectedAsSource = isForSource.value
                ?: (selected is TransferCounterparty.Account
                        || selected is TransferCounterparty.Category && selected.category.isIncome),
            selectedCounterparty = selected,
        )

        log.debug {
            "postResult(): posting:" +
                    "\nresult=$result"
        }

        _events.tryEmit(Event.Selected(result))
    }

    sealed interface Event {

        class Selected(
            val result: TransferCounterpartySelectionResult,
        ) : Event
    }
}
