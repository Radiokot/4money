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
import com.powersync.db.getBooleanOptional
import com.powersync.db.getDouble
import com.powersync.db.getLong
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.powersync.db.internal.PowerSyncTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeDescPositionHealer
import ua.com.radiokot.money.util.SternBrocotTreeSearch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PowerSyncCategoryRepository(
    colorSchemeRepository: ItemColorSchemeRepository,
    private val database: PowerSyncDatabase,
) : CategoryRepository {

    private val log by lazyLogger("PowerSyncCategoryRepo")
    private val colorSchemesByName = colorSchemeRepository.getItemColorSchemesByName()
    private val categoryPositionHealer = SternBrocotTreeDescPositionHealer(Category::position)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val categoriesSharedFlow = database
        .watch(
            sql = SELECT_CATEGORIES,
            mapper = ::toCategory,
        )
        .flowOn(Dispatchers.Default)
        .shareIn(coroutineScope, SharingStarted.Lazily, replay = 1)

    override suspend fun getCategories(
        isIncome: Boolean,
    ): List<Category> =
        categoriesSharedFlow
            .first()
            .filter { it.isIncome == isIncome }

    override fun getCategoriesFlow(
        isIncome: Boolean,
    ): Flow<List<Category>> =
        categoriesSharedFlow
            .map { allCategories ->
                allCategories.filter { it.isIncome == isIncome }
            }
            .flatMapLatest(::healPositionsIfNeeded)

    override suspend fun getCategory(
        categoryId: String,
    ): Category? =
        categoriesSharedFlow
            .first()
            .find { it.id == categoryId }

    override suspend fun getSubcategory(
        subcategoryId: String,
    ): Subcategory? =
        database
            .getOptional(
                sql = SELECT_SUBCATEGORY_BY_ID,
                parameters = listOf(
                    subcategoryId,
                ),
                mapper = ::toSubcategory,
            )

    override fun getSubcategoriesFlow(
        categoryId: String,
    ): Flow<List<Subcategory>> = database
        .watch(
            sql = SELECT_SUBCATEGORIES_BY_PARENT_ID,
            parameters = listOf(
                categoryId,
            ),
            mapper = ::toSubcategory,
        )

    private val subcategoriesByCategorySharedFlow = database
        .watch(
            sql = SELECT_CATEGORIES_THEN_SUBCATEGORIES,
            mapper = { sqlCursor ->
                val parentCategoryId = sqlCursor.getStringOptional("categoryParentId")
                if (parentCategoryId == null)
                    toCategory(sqlCursor)
                else
                    toSubcategory(sqlCursor)
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
        .shareIn(coroutineScope, SharingStarted.Lazily, replay = 1)

    override fun getSubcategoriesByCategoriesFlow(): Flow<Map<Category, List<Subcategory>>> =
        subcategoriesByCategorySharedFlow

    private suspend fun healPositionsIfNeeded(
        categories: List<Category>,
    ): Flow<List<Category>> {

        if (categoryPositionHealer.arePositionsHealthy(categories)) {
            return flowOf(categories)
        }

        log.debug {
            "healPositionsIfNeeded(): start healing"
        }

        var updateCount = 0
        database.writeTransaction { transaction ->
            categoryPositionHealer.healPositions(
                items = categories,
                updatePosition = { item, newPosition ->
                    transaction.execute(
                        sql = "UPDATE categories SET position = ? WHERE id = ?",
                        parameters = listOf(
                            newPosition.toString(),
                            item.id,
                        )
                    )
                    updateCount++
                }
            )
        }

        log.debug {
            "healPositionsIfNeeded(): healed successfully:" +
                    "\nupdates=$updateCount"
        }

        return emptyFlow()
    }

    fun updateCategory(
        categoryId: String,
        newTitle: String,
        newColorScheme: ItemColorScheme,
        transaction: PowerSyncTransaction,
    ): Category {
        val categoryToUpdate = runBlocking {
            getCategory(categoryId)
                ?: error("Category to update not found")
        }

        transaction.execute(
            sql = INSERT_OR_REPLACE_CATEGORY,
            parameters = listOf(
                categoryToUpdate.id,
                newTitle,
                categoryToUpdate.currency.id,
                null,
                categoryToUpdate.isIncome,
                newColorScheme.name,
                categoryToUpdate.position,
            )
        )

        return categoryToUpdate
    }

    fun addCategory(
        title: String,
        currency: Currency,
        isIncome: Boolean,
        colorScheme: ItemColorScheme,
        transaction: PowerSyncTransaction,
    ): Category {
        val categoryToPlaceBefore: Category? = runBlocking {
            getCategories(isIncome)
                .minOrNull()
        }

        val position = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = categoryToPlaceBefore?.position ?: 0.0,
                upperBound = Double.POSITIVE_INFINITY,
            )
            .value

        val category = Category(
            title = title,
            currency = currency,
            isIncome = isIncome,
            colorScheme = colorScheme,
            isArchived = false,
            position = position,
        )

        transaction.execute(
            sql = INSERT_OR_REPLACE_CATEGORY,
            parameters = listOf(
                category.id,
                category.title,
                category.currency.id,
                null,
                category.isIncome,
                category.colorScheme.name,
                category.position,
            )
        )

        return category
    }

    fun updateSubcategories(
        parentCategory: Category,
        subcategories: List<SubcategoryToUpdate>,
        transaction: PowerSyncTransaction,
    ) {
        val sternBrocotTree = SternBrocotTreeSearch()

        subcategories.forEach { subcategoryToUpdate ->

            sternBrocotTree.goRight()

            val id =
                if (subcategoryToUpdate.isNew)
                    UUID.randomUUID().toString()
                else
                    subcategoryToUpdate.id

            transaction.execute(
                sql = INSERT_OR_REPLACE_CATEGORY,
                parameters = listOf(
                    id,
                    subcategoryToUpdate.title,
                    parentCategory.currency.id,
                    parentCategory.id,
                    parentCategory.isIncome,
                    parentCategory.colorScheme.name,
                    sternBrocotTree.value,
                )
            )
        }
    }

    private fun toCategory(
        sqlCursor: SqlCursor,
    ): Category = with(sqlCursor) {
        Category(
            id = getString("categoryId"),
            title = getString("categoryTitle").trim(),
            isIncome = getBooleanOptional("categoryIsIncome") == true,
            colorScheme = getString("categoryColorScheme")
                .trim()
                .let { colorSchemeName ->
                    colorSchemesByName[colorSchemeName]
                        ?: error("Can't find '$colorSchemeName' color scheme")
                },
            isArchived = getBooleanOptional("categoryArchived") == true,
            position = getDouble("categoryPosition"),
            currency = Currency(
                id = getString("currencyId"),
                code = getString("currencyCode").trim(),
                symbol = getString("currencySymbol").trim(),
                precision = getLong("currencyPrecision").toInt(),
            ),
        )
    }

    private fun toSubcategory(
        sqlCursor: SqlCursor,
    ): Subcategory = with(sqlCursor) {
        Subcategory(
            id = getString("categoryId"),
            title = getString("categoryTitle").trim(),
            position = getDouble("categoryPosition"),
            categoryId = getString("categoryParentId"),
        )
    }
}

private const val CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES =
    "currencies.id as 'currencyId', " +
            "currencies.code as 'currencyCode', " +
            "currencies.symbol as 'currencySymbol', " +
            "currencies.precision as 'currencyPrecision', " +
            "categories.id as 'categoryId', " +
            "categories.title as 'categoryTitle', " +
            "categories.is_income as 'categoryIsIncome', " +
            "categories.color_scheme as 'categoryColorScheme', " +
            "categories.archived as 'categoryArchived', " +
            "categories.position as 'categoryPosition', " +
            "categories.parent_category_id as 'categoryParentId' " +
            "FROM categories, currencies"

private const val SUBCATEGORY_FIELDS_FROM_CATEGORIES =
    "categories.id as 'categoryId', " +
            "categories.title as 'categoryTitle', " +
            "categories.position as 'categoryPosition', " +
            "categories.parent_category_id as 'categoryParentId' " +
            "FROM categories"

private const val CURRENCY_MATCHES_CATEGORY =
    "categories.currency_id = currencies.id"

private const val SELECT_CATEGORIES =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE categoryParentId IS NULL " +
            "AND $CURRENCY_MATCHES_CATEGORY "

/**
 * Params:
 * 1. Parent category ID
 */
private const val SELECT_SUBCATEGORIES_BY_PARENT_ID =
    "SELECT $SUBCATEGORY_FIELDS_FROM_CATEGORIES " +
            "WHERE categoryParentId = ?"

/**
 * Params:
 * 1. Subcategory ID
 */
private const val SELECT_SUBCATEGORY_BY_ID =
    "SELECT $SUBCATEGORY_FIELDS_FROM_CATEGORIES " +
            "WHERE categoryId = ?"

private const val SELECT_CATEGORIES_THEN_SUBCATEGORIES =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE $CURRENCY_MATCHES_CATEGORY " +
            "ORDER BY categoryParentId ASC"

/**
 * Params:
 * 1. ID
 * 2. Title
 * 3. Currency ID
 * 4. Parent category ID
 * 5. Is income boolean
 * 6. Color scheme name
 * 7. Position
 */
private const val INSERT_OR_REPLACE_CATEGORY =
    "INSERT OR REPLACE INTO categories " +
            "(id, title, currency_id, parent_category_id, is_income, color_scheme, position) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?)"
