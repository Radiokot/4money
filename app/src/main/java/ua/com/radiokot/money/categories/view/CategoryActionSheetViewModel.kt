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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryStats
import ua.com.radiokot.money.categories.logic.GetCategoryStatsUseCase
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

class CategoryActionSheetViewModel(
    parameters: Parameters,
    private val getCategoryStatsUseCase: GetCategoryStatsUseCase,
) : ViewModel() {

    private val log by lazyLogger("CategoryActionSheetVM")

    private val categoryStats: StateFlow<CategoryStats> = runBlocking {
        getCategoryStatsUseCase(
            isIncome = parameters.isIncome,
            period = parameters.statsPeriod,
        )
            .mapNotNull { categoryStats ->
                categoryStats.find { it.first.id == parameters.categoryId }
            }
            .run { stateIn(viewModelScope, SharingStarted.Eagerly, first()) }
    }

    private val category: StateFlow<Category> =
        categoryStats
            .map(viewModelScope, CategoryStats::first)

    val title: StateFlow<String> =
        category
            .map(viewModelScope, Category::title)

    val statsPeriod: HistoryPeriod = parameters.statsPeriod

    val statsAmount: StateFlow<ViewAmount> =
        categoryStats
            .map(viewModelScope) { (category, amount) ->
                ViewAmount(
                    value = amount,
                    currency = category.currency,
                )
            }

    val colorScheme: StateFlow<ItemColorScheme> =
        category
            .map(viewModelScope, Category::colorScheme)

    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    fun onEditClicked() {
        _events.tryEmit(
            Event.ProceedToEdit(
                category = category.value,
            )
        )
    }

    fun onActivityClicked() {
        _events.tryEmit(
            Event.ProceedToFilteredActivity(
                categoryCounterparty = TransferCounterparty.Category(
                    category = category.value
                )
            )
        )
    }

    sealed interface Event {

        class ProceedToEdit(
            val category: Category,
        ) : Event

        class ProceedToFilteredActivity(
            val categoryCounterparty: TransferCounterparty.Category,
        ) : Event
    }

    class Parameters(
        val categoryId: String,
        val isIncome: Boolean,
        val statsPeriod: HistoryPeriod,
    )
}
