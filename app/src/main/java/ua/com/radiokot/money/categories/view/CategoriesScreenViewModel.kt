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

package ua.com.radiokot.money.categories.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.categories.data.CategoriesWithAmountAndTotal
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.logic.GetCategoriesWithAmountsAndTotalUseCase
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.view.HistoryStatsPeriodViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesScreenViewModel(
    historyStatsPeriodViewModel: HistoryStatsPeriodViewModel,
    getCategoriesWithAmountAndTotalUseCase: GetCategoriesWithAmountsAndTotalUseCase,
) : ViewModel(),
    HistoryStatsPeriodViewModel by historyStatsPeriodViewModel {

    private val log by lazyLogger("CategoriesScreenVM")
    private val _isIncome: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isIncome = _isIncome.asStateFlow()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private val incomeCategoriesWithAmountAndTotal: Flow<CategoriesWithAmountAndTotal> =
        historyStatsPeriod.flatMapLatest { period ->
            getCategoriesWithAmountAndTotalUseCase(
                isIncome = true,
                period = period,
            )
        }
    val incomeCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        incomeCategoriesWithAmountAndTotal
            .map { it.categories.toSortedViewItemList() }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val expenseCategoriesWithAmountAndTotal: Flow<CategoriesWithAmountAndTotal> =
        historyStatsPeriod.flatMapLatest { period ->
            getCategoriesWithAmountAndTotalUseCase(
                isIncome = false,
                period = period,
            )
        }
    val expenseCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        expenseCategoriesWithAmountAndTotal
            .map { it.categories.toSortedViewItemList() }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalAmount: StateFlow<ViewAmount?> =
        isIncome
            .flatMapLatest { isIncome ->
                if (isIncome)
                    incomeCategoriesWithAmountAndTotal
                else
                    expenseCategoriesWithAmountAndTotal
            }
            .mapNotNull { it.totalInPrimaryCurrency }
            .mapNotNull(::ViewAmount)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun onTitleClicked() {
        val newIsIncome = !isIncome.value

        log.debug {
            "onTitleClicked(): switching income mode:" +
                    "\nnewIsIncome=$newIsIncome"
        }

        _isIncome.tryEmit(newIsIncome)
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
            "onCategoryItemClicked(): proceeding to transfer:" +
                    "\ncategory=$category"
        }

        _events.tryEmit(
            Event.ProceedToTransfer(category)
        )
    }

    fun onCategoryItemLongClicked(
        item: ViewCategoryListItem,
    ) {
        val period = historyStatsPeriod.value
        val category = item.source
        if (category == null) {
            log.warn {
                "onCategoryItemLongClicked(): missing category source"
            }
            return
        }

        log.debug {
            "onCategoryItemLongClicked(): proceeding to actions:" +
                    "\ncategory=$category," +
                    "\nperiod=$period"
        }

        _events.tryEmit(
            Event.ProceedToCategoryActions(
                category = category,
                statsPeriod = period,
            )
        )
    }

    fun onAddClicked() {
        val isIncome = isIncome.value

        log.debug {
            "onAddClicked(): proceeding to adding:" +
                    "\nisIncome=$isIncome"
        }

        _events.tryEmit(
            Event.ProceedToCategoryAdd(
                isIncome = isIncome,
            )
        )
    }

    sealed interface Event {

        class ProceedToTransfer(
            val category: Category,
        ) : Event

        class ProceedToCategoryActions(
            val category: Category,
            val statsPeriod: HistoryPeriod,
        ) : Event

        class ProceedToCategoryAdd(
            val isIncome: Boolean,
        ) : Event
    }
}
