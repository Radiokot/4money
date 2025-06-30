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

class PowerSyncEditCategoryUseCase(
    private val categoryRepository: PowerSyncCategoryRepository,
    private val database: PowerSyncDatabase,
) : EditCategoryUseCase {

    override suspend fun invoke(
        categoryId: String,
        newTitle: String,
        newColorScheme: ItemColorScheme,
        subcategories: List<SubcategoryToUpdate>,
    ): Result<Unit> = runCatching {

        database.writeTransaction { transaction ->

            val updatedCategory = categoryRepository.updateCategory(
                categoryId = categoryId,
                newTitle = newTitle,
                newColorScheme = newColorScheme,
                transaction = transaction,
            )

            categoryRepository.updateSubcategories(
                parentCategory = updatedCategory,
                subcategories = subcategories,
                transaction = transaction,
            )
        }
    }
}
