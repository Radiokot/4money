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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val historyStatsRepository: HistoryStatsRepository,
) : ViewModel() {

    init {
        println("OOLEG im created ${hashCode()}")
    }

    override fun onCleared() {
        println("OOLEG im dead ${hashCode()}")
        super.onCleared()
    }

    private val log by lazyLogger("CategoriesVM")
    private val _isIncome: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isIncome: StateFlow<Boolean> = _isIncome

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

    fun onTitleClicked() {
        val newIsIncome = !isIncome.value

        log.debug {
            "onTitleClicked(): switching income mode:" +
                    "\nnewIsIncome=$newIsIncome"
        }

        _isIncome.tryEmit(newIsIncome)
    }
}
