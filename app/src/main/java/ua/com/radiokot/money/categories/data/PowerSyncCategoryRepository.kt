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

package ua.com.radiokot.money.categories.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.SqlCursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import ua.com.radiokot.money.currency.data.Currency

class PowerSyncCategoryRepository(
    private val database: PowerSyncDatabase,
) : CategoryRepository {
    override suspend fun getCategories(): List<Category> =
        database
            .getAll(
                sql = SELECT_CATEGORIES,
                mapper = ::toCategory,
            )

    override fun getCategoriesFlow(): Flow<List<Category>> =
        database
            .watch(
                sql = SELECT_CATEGORIES,
                mapper = ::toCategory,
            )

    override suspend fun getSubcategories(categoryId: String): List<Subcategory> =
        database
            .getAll(
                sql = SELECT_SUBCATEGORIES_BY_PARENT_ID,
                parameters = listOf(
                    categoryId,
                ),
                mapper = ::toSubcategory,
            )

    override fun getCategoryFlow(categoryId: String): Flow<Category> =
        database
            .watch(
                sql = SELECT_CATEGORY_BY_ID,
                parameters = listOf(
                    categoryId,
                ),
                mapper = ::toCategory
            )
            .mapNotNull(List<Category>::firstOrNull)

    private fun toCategory(sqlCursor: SqlCursor): Category = sqlCursor.run {
        var column = 0

        val currency = Currency(
            id = getString(column)!!,
            code = getString(++column)!!.trim(),
            symbol = getString(++column)!!.trim(),
            precision = getLong(++column)!!.toInt(),
        )

        Category(
            id = getString(++column)!!,
            title = getString(++column)!!.trim(),
            isIncome = getBoolean(++column) == true,
            currency = currency,
        )
    }

    private fun toSubcategory(sqlCursor: SqlCursor): Subcategory = sqlCursor.run {
        var column = 0

        Subcategory(
            id = getString(++column)!!,
            title = getString(++column)!!.trim(),
        )
    }
}

private const val SELECT_CATEGORIES =
    "SELECT currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "categories.id, categories.title, categories.is_income " +
            "FROM categories, currencies " +
            "WHERE categories.parent_category_id IS NULL AND categories.currency_id = currencies.id"

private const val SELECT_CATEGORY_BY_ID =
    "SELECT currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "categories.id, categories.title, categories.is_income " +
            "FROM categories, currencies " +
            "WHERE categories.id = ? AND categories.currency_id = currencies.id"

private const val SELECT_SUBCATEGORIES_BY_PARENT_ID =
    "SELECT categories.id, categories.title " +
            "FROM categories " +
            "WHERE category.parent_category_id = ?"
