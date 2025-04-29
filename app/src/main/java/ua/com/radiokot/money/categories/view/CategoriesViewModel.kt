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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryStats
import ua.com.radiokot.money.categories.logic.GetCategoryStatsUseCase
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.home.view.HomeViewModel
import ua.com.radiokot.money.lazyLogger
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    homeViewModel: HomeViewModel,
    getCategoryStatsUseCase: GetCategoryStatsUseCase,
    private val currencyRepository: CurrencyRepository,
    private val currencyPreferences: CurrencyPreferences,
) : ViewModel() {

    private val log by lazyLogger("CategoriesVM")
    private val _isIncome: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isIncome = _isIncome.asStateFlow()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private val incomeCategoryStats: Flow<List<CategoryStats>> =
        homeViewModel.period.flatMapLatest { period ->
            getCategoryStatsUseCase(
                isIncome = true,
                period = period,
            )
        }
    val incomeCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        incomeCategoryStats
            .map(List<CategoryStats>::toSortedViewItemList)
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val expenseCategoryStats: Flow<List<CategoryStats>> =
        homeViewModel.period.flatMapLatest { period ->
            getCategoryStatsUseCase(
                isIncome = false,
                period = period,
            )
        }
    val expenseCategoryItemList: StateFlow<List<ViewCategoryListItem>> =
        expenseCategoryStats
            .map(List<CategoryStats>::toSortedViewItemList)
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalAmount: StateFlow<ViewAmount?> =
        combine(
            isIncome.flatMapLatest { isIncome ->
                if (isIncome)
                    incomeCategoryStats
                else
                    expenseCategoryStats
            },
            currencyRepository.getCurrencyPairMapFlow(),
            currencyPreferences.primaryCurrencyCode,
            transform = ::Triple,
        )
            .map { (categoryStats, currencyPairMap, primaryCurrencyCode) ->
                val primaryCurrency = currencyRepository
                    .getCurrencyByCode(primaryCurrencyCode)
                    ?: return@map null

                val totalInPrimaryCurrency: BigInteger =
                    categoryStats.fold(BigInteger.ZERO) { sum, (category, amount) ->
                        sum + (
                                currencyPairMap
                                    .get(
                                        base = category.currency,
                                        quote = primaryCurrency,
                                    )
                                    ?.baseToQuote(amount)
                                    ?: BigInteger.ZERO
                                )
                    }

                ViewAmount(
                    value = totalInPrimaryCurrency,
                    currency = primaryCurrency,
                )
            }
            .flowOn(Dispatchers.Default)
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

    sealed interface Event {

        class ProceedToTransfer(
            val category: Category,
        ) : Event
    }
}
