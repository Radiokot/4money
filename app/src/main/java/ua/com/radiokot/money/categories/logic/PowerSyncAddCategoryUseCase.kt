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

import com.powersync.PowerSyncDatabase
import ua.com.radiokot.money.categories.data.PowerSyncCategoryRepository
import ua.com.radiokot.money.categories.data.SubcategoryToUpdate
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.util.SternBrocotTreeSearch

class PowerSyncAddCategoryUseCase(
    private val database: PowerSyncDatabase,
    private val categoryRepository: PowerSyncCategoryRepository,
) : AddCategoryUseCase {

    override suspend fun invoke(
        title: String,
        currency: Currency,
        isIncome: Boolean,
        colorScheme: ItemColorScheme,
        icon: ItemIcon?,
        subcategories: List<SubcategoryToUpdate>,
    ): Result<Unit> = runCatching {

        val firstCategoryOfTargetType = categoryRepository
            .getCategories(
                isIncome = isIncome,
            )
            .firstOrNull { !it.isArchived }

        val position = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = firstCategoryOfTargetType?.position ?: 0.0,
                upperBound = Double.POSITIVE_INFINITY,
            )
            .value

        database.writeTransaction { transaction ->

            val addedCategory = categoryRepository.addCategory(
                title = title,
                currency = currency,
                isIncome = isIncome,
                colorScheme = colorScheme,
                icon = icon,
                position = position,
                transaction = transaction,
            )

            categoryRepository.updateSubcategories(
                parentCategory = addedCategory,
                subcategories = subcategories,
                transaction = transaction,
            )
        }
    }
}
