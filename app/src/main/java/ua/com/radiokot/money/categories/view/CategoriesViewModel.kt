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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryStats
import ua.com.radiokot.money.categories.logic.GetCategoryStatsUseCase
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

class CategoriesViewModel(
    getCategoryStatsUseCase: GetCategoryStatsUseCase,
) : ViewModel() {

    private val log by lazyLogger("CategoriesVM")
    private val _isIncome: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isIncome: StateFlow<Boolean> = _isIncome
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private val incomeCategoryStats: Flow<List<CategoryStats>> =
        getCategoryStatsUseCase(
            isIncome = true,
            period = HistoryPeriod.Month(),
        )
    val incomeCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        incomeCategoryStats
            .map(List<CategoryStats>::toSortedViewItemList)
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val expenseCategoryStats: Flow<List<CategoryStats>> =
        getCategoryStatsUseCase(
            isIncome = false,
            period = HistoryPeriod.Month(),
        )
    val expenseCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
       expenseCategoryStats
           .map(List<CategoryStats>::toSortedViewItemList)
           .flowOn(Dispatchers.Default)
           .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    sealed interface Event {

        class ProceedToTransfer(
            val category: Category,
        ) : Event
    }
}
