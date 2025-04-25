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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeSearch

@OptIn(ExperimentalCoroutinesApi::class)
class PowerSyncCategoryRepository(
    colorSchemeRepository: ItemColorSchemeRepository,
    private val database: PowerSyncDatabase,
) : CategoryRepository {

    private val log by lazyLogger("PowerSyncCategoryRepo")
    private val colorSchemesByName = colorSchemeRepository.getItemColorSchemesByName()

    override suspend fun getCategories(isIncome: Boolean): List<Category> =
        database
            .getAll(
                sql = SELECT_CATEGORIES_BY_INCOME,
                parameters = listOf(
                    if (isIncome)
                        "1"
                    else
                        "0"
                ),
                mapper = ::toCategory,
            )

    override fun getCategoriesFlow(isIncome: Boolean): Flow<List<Category>> =
        database
            .watch(
                sql = SELECT_CATEGORIES_BY_INCOME,
                parameters = listOf(
                    if (isIncome)
                        "1"
                    else
                        "0"
                ),
                mapper = ::toCategory,
            )
            .flatMapLatest(::healPositionsIfNeeded)

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

    override fun getSubcategoriesFlow(categoryId: String): Flow<List<Subcategory>> =
        database
            .watch(
                sql = SELECT_SUBCATEGORIES_BY_PARENT_ID,
                parameters = listOf(
                    categoryId,
                ),
                mapper = ::toSubcategory,
            )

    override fun getSubcategoriesByCategoriesFlow(): Flow<Map<Category, List<Subcategory>>> =
        database
            .watch(
                sql = SELECT_CATEGORIES_THEN_SUBCATEGORIES,
                mapper = { sqlCursor ->
                    val parentCategoryId = sqlCursor.getString(9)
                    if (parentCategoryId == null)
                        toCategory(sqlCursor)
                    else
                        Subcategory(
                            id = sqlCursor.getString(4)!!, // Hell.
                            title = sqlCursor.getString(5)!!.trim(),
                            categoryId = parentCategoryId,
                        )
                }
            )
            .map { categoriesAndSubcategories ->
                buildMap<Category, MutableList<Subcategory>> {
                    val categoriesById = mutableMapOf<String, Category>()
                    categoriesAndSubcategories.forEach { categoryOrSubcategory ->
                        if (categoryOrSubcategory is Category) {
                            categoriesById[categoryOrSubcategory.id] = categoryOrSubcategory
                            put(categoryOrSubcategory, mutableListOf())
                        } else if (categoryOrSubcategory is Subcategory) {
                            getValue(categoriesById.getValue(categoryOrSubcategory.categoryId))
                                .add(categoryOrSubcategory)
                        }
                    }
                }
            }
            .flowOn(Dispatchers.Default)

    private suspend fun healPositionsIfNeeded(
        categories: List<Category>,
    ): Flow<List<Category>> {
        val distinctPositions = categories.mapTo(mutableSetOf(), Category::position)
        if (distinctPositions.isNotEmpty()
            && (distinctPositions.size < categories.size || distinctPositions.any { it <= 0 })
        ) {
            healPositions(categories)
            return emptyFlow()
        } else {
            return flowOf(categories)
        }
    }

    private suspend fun healPositions(
        categories: List<Category>,
    ) {
        log.debug {
            "healPositions(): re-assigning positions:" +
                    "\ncount=${categories.size}"
        }

        // Assign a new position to each category preserving the current order.
        val sortedCategories = categories.sorted()
        val params = mutableListOf<String>()
        val sql = buildString {
            append("UPDATE categories SET position = CASE ")
            val sternBrocotTree = SternBrocotTreeSearch()
            sortedCategories.indices.reversed().forEach { categoryIndex ->
                append("\nWHEN id = ? THEN ? ")
                params += sortedCategories[categoryIndex].id
                params += sternBrocotTree.value.toString()
                sternBrocotTree.goRight()
            }
            append("ELSE position END")
        }

        database.execute(
            sql = sql,
            parameters = params,
        )

        log.debug {
            "healPositions(): re-assigned successfully"
        }
    }

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
            colorScheme = getString(++column)!!
                .trim()
                .let { colorSchemeName ->
                    colorSchemesByName[colorSchemeName]
                        ?: error("Can't find '$colorSchemeName' color scheme")
                },
            position = getDouble(++column)!!,
            currency = currency,
        )
    }

    private fun toSubcategory(sqlCursor: SqlCursor): Subcategory = sqlCursor.run {
        var column = 0

        Subcategory(
            id = getString(column)!!,
            title = getString(++column)!!.trim(),
            categoryId = getString(++column)!!,
        )
    }
}

private const val CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES =
    "currencies.id, currencies.code, currencies.symbol, currencies.precision, " +
            "categories.id, categories.title, categories.is_income, categories.color_scheme, " +
            "categories.position, " +
            "categories.parent_category_id " +
            "FROM categories, currencies"

private const val SUBCATEGORY_FIELDS_FROM_CATEGORIES =
    "categories.id, categories.title, categories.parent_category_id " +
            "FROM categories"

private const val CURRENCY_MATCHES_CATEGORY =
    "categories.currency_id = currencies.id"

/**
 * Params:
 * 1. 1 for income, 0 for expense
 */
private const val SELECT_CATEGORIES_BY_INCOME =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE categories.parent_category_id IS NULL " +
            "AND $CURRENCY_MATCHES_CATEGORY " +
            "AND categories.is_income = ?"

/**
 * Params:
 * 1. Category ID
 */
private const val SELECT_CATEGORY_BY_ID =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE categories.id = ? " +
            "AND $CURRENCY_MATCHES_CATEGORY"

/**
 * Params:
 * 1. Parent category ID
 */
private const val SELECT_SUBCATEGORIES_BY_PARENT_ID =
    "SELECT $SUBCATEGORY_FIELDS_FROM_CATEGORIES " +
            "WHERE categories.parent_category_id = ?"

private const val SELECT_CATEGORIES_THEN_SUBCATEGORIES =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE $CURRENCY_MATCHES_CATEGORY " +
            "ORDER BY categories.parent_category_id ASC"
