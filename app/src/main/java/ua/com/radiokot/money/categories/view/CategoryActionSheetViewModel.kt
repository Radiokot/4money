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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryWithAmount
import ua.com.radiokot.money.categories.logic.GetCategoriesWithAmountUseCase
import ua.com.radiokot.money.categories.logic.UnarchiveCategoryUseCase
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.view.ViewHistoryPeriod

class CategoryActionSheetViewModel(
    parameters: Parameters,
    private val getCategoriesWithAmountUseCase: GetCategoriesWithAmountUseCase,
    private val unarchiveCategoryUseCase: UnarchiveCategoryUseCase,
) : ViewModel() {

    private val log by lazyLogger("CategoryActionSheetVM")
    private val categoryWithAmount: StateFlow<CategoryWithAmount> = runBlocking {
        getCategoriesWithAmountUseCase(
            isIncome = parameters.isIncome,
            period = parameters.statsPeriod,
        )
            .mapNotNull { categoriesWithAmount ->
                categoriesWithAmount.find { it.category.id == parameters.categoryId }
            }
            .run { stateIn(viewModelScope, SharingStarted.Eagerly, first()) }
    }

    private val category: StateFlow<Category> =
        categoryWithAmount
            .map(viewModelScope, CategoryWithAmount::category)

    val title: StateFlow<String> =
        category
            .map(viewModelScope, Category::title)

    val statsPeriod: ViewHistoryPeriod =
        ViewHistoryPeriod.fromHistoryPeriod(parameters.statsPeriod)

    val statsAmount: StateFlow<ViewAmount> =
        categoryWithAmount
            .map(viewModelScope) { (category, amount) ->
                ViewAmount(
                    value = amount,
                    currency = category.currency,
                )
            }

    val colorScheme: StateFlow<ItemColorScheme> =
        category
            .map(viewModelScope, Category::colorScheme)

    val isUnarchiveVisible: StateFlow<Boolean> =
        categoryWithAmount
            .map(viewModelScope) { (category, _) ->
                category.isArchived
            }

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

    fun onUnarchiveClicked() {
        unarchiveCategory()
    }

    private var unarchiveJob: Job? = null
    private fun unarchiveCategory() {
        unarchiveJob?.cancel()
        unarchiveJob = viewModelScope.launch {

            val category = category.value

            log.debug {
                "unarchiveCategory(): unarchiving:" +
                        "\naccount=$category"
            }

            unarchiveCategoryUseCase
                .invoke(
                    categoryToUnarchive = category,
                )
                .onFailure { error ->
                    log.error(error) {
                        "unarchiveCategory(): failed to unarchive category"
                    }
                }
                .onSuccess {
                    log.info {
                        "Unarchived category $category"
                    }

                    log.debug {
                        "unarchiveCategory(): category unarchived"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    sealed interface Event {

        class ProceedToEdit(
            val category: Category,
        ) : Event

        class ProceedToFilteredActivity(
            val categoryCounterparty: TransferCounterparty.Category,
        ) : Event

        object Done : Event
    }

    class Parameters(
        val categoryId: String,
        val isIncome: Boolean,
        val statsPeriod: HistoryPeriod,
    )
}
