/* Copyright 2026 Oleg Koretsky

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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.CategoryWithAmountsBySubcategory
import ua.com.radiokot.money.categories.data.Subcategory
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class GetCategoryAmountsBySubcategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val historyStatsRepository: HistoryStatsRepository,
) {

    /**
     * @return given category and its amounts by each subcategory,
     * including no subcategory, for the given period.
     */
    operator fun invoke(
        categoryId: String,
        period: HistoryPeriod,
    ): Flow<CategoryWithAmountsBySubcategory> =
        categoryRepository
            .getSubcategoriesByCategoriesFlow()
            .map { allCategoriesWithSubcategories ->
                val (category, subcategories) = allCategoriesWithSubcategories
                    .entries
                    .first { (category, _) ->
                        category.id == categoryId
                    }
                val subcategoriesById = subcategories.associateBy(Subcategory::id)

                category to subcategoriesById
            }
            .flatMapLatest { (category, subcategoriesById) ->
                historyStatsRepository
                    .getCategoryAmountsBySubcategoryFlow(
                        categoryId = category.id,
                        isIncome = category.isIncome,
                        period = period,
                    )
                    .map { categoryAmountsBySubcategoryId ->

                        CategoryWithAmountsBySubcategory(
                            category = category,
                            amountBySubcategory =
                                categoryAmountsBySubcategoryId
                                    .mapKeys { (subcategoryId, _) ->
                                        subcategoriesById[subcategoryId]
                                    }
                        )
                    }
            }
}
