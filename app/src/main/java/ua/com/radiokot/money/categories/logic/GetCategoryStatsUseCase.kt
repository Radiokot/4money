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

package ua.com.radiokot.money.categories.logic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.CategoryStats
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository
import java.math.BigInteger

class GetCategoryStatsUseCase(
    private val categoryRepository: CategoryRepository,
    private val historyStatsRepository: HistoryStatsRepository,
) {
    operator fun invoke(
        isIncome: Boolean,
        period: HistoryPeriod,
    ): Flow<List<CategoryStats>> =
        combine(
            categoryRepository.getCategoriesFlow(
                isIncome = isIncome,
            ),
            historyStatsRepository.getCategoryStatsFlow(
                isIncome = isIncome,
                period = period,
            ),
            ::Pair,
        )
            .map { (categories, amountsByCategoryId) ->
                buildList {
                    categories.forEach { category ->
                        add(
                            category to amountsByCategoryId.getOrDefault(
                                category.id,
                                BigInteger.ZERO
                            )
                        )
                    }
                }
            }
}
