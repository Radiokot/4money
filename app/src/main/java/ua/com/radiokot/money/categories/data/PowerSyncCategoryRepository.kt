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
import com.powersync.db.getStringOptional
import com.powersync.db.internal.PowerSyncTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.powersync.DbSchema
import ua.com.radiokot.money.util.SternBrocotTreeSearch
import java.util.UUID

class PowerSyncCategoryRepository(
    colorSchemeRepository: ItemColorSchemeRepository,
    private val database: PowerSyncDatabase,
) : CategoryRepository {

    private val colorSchemesByName = colorSchemeRepository.getItemColorSchemesByName()
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
                mapper = DbSchema::toSubcategory,
            )

    override fun getSubcategoriesFlow(
        categoryId: String,
    ): Flow<List<Subcategory>> = database
        .watch(
            sql = SELECT_SUBCATEGORIES_BY_PARENT_ID,
            parameters = listOf(
                categoryId,
            ),
            mapper = DbSchema::toSubcategory,
        )

    private val subcategoriesByCategorySharedFlow = database
        .watch(
            sql = SELECT_CATEGORIES_THEN_SUBCATEGORIES,
            mapper = { sqlCursor ->
                val parentCategoryId =
                    sqlCursor.getStringOptional(DbSchema.CATEGORY_SELECTED_PARENT_ID)
                if (parentCategoryId == null)
                    toCategory(sqlCursor)
                else
                    DbSchema.toSubcategory(sqlCursor)
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

    override suspend fun archiveCategory(
        categoryId: String,
    ) {
        database.writeTransaction { transaction ->

            updateArchived(
                categoryId = categoryId,
                isArchived = true,
                transaction = transaction,
            )
        }
    }

    override suspend fun unarchiveCategory(
        categoryId: String,
    ) {
        database.writeTransaction { transaction ->

            updateArchived(
                categoryId = categoryId,
                isArchived = false,
                transaction = transaction,
            )
        }
    }

    private fun updateArchived(
        categoryId: String,
        isArchived: Boolean,
        transaction: PowerSyncTransaction,
    ) {
        transaction.execute(
            sql = UPDATE_ARCHIVED_BY_ID,
            parameters = listOf(
                isArchived,
                categoryId,
            )
        )
    }

    fun updateCategory(
        categoryId: String,
        newTitle: String,
        newColorScheme: ItemColorScheme,
        transaction: PowerSyncTransaction,
    ) {
        transaction.execute(
            sql = UPDATE_CATEGORY_BY_ID,
            parameters = listOf(
                newTitle,
                newColorScheme.name,
                categoryId,
            )
        )
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
            sql = INSERT_CATEGORY,
            parameters = listOf(
                category.id,
                category.title,
                category.currency.id,
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
                sql = INSERT_OR_REPLACE_SUBCATEGORY,
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
    ): Category =
        DbSchema.toCategory(
            sqlCursor = sqlCursor,
            colorSchemesByName = colorSchemesByName,
        )
}

private const val CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES =
    DbSchema.CATEGORY_SELECT_COLUMNS + ", " +
            DbSchema.CURRENCY_SELECT_COLUMNS +
            "FROM ${DbSchema.CATEGORIES_TABLE}, ${DbSchema.CURRENCIES_TABLE}"

private const val SUBCATEGORY_FIELDS_FROM_CATEGORIES =
    DbSchema.SUBCATEGORY_SELECT_COLUMNS +
            "FROM ${DbSchema.CATEGORIES_TABLE}"

private const val CURRENCY_MATCHES_CATEGORY =
    "${DbSchema.CATEGORY_SELECTED_CURRENCY_ID} = ${DbSchema.CURRENCY_SELECTED_ID}"

private const val SELECT_CATEGORIES =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE ${DbSchema.CATEGORY_SELECTED_PARENT_ID} IS NULL " +
            "AND $CURRENCY_MATCHES_CATEGORY "

/**
 * Params:
 * 1. Parent category ID
 */
private const val SELECT_SUBCATEGORIES_BY_PARENT_ID =
    "SELECT $SUBCATEGORY_FIELDS_FROM_CATEGORIES " +
            "WHERE ${DbSchema.CATEGORY_SELECTED_PARENT_ID} = ?"

/**
 * Params:
 * 1. Subcategory ID
 */
private const val SELECT_SUBCATEGORY_BY_ID =
    "SELECT $SUBCATEGORY_FIELDS_FROM_CATEGORIES " +
            "WHERE ${DbSchema.CATEGORY_SELECTED_ID} = ?"

private const val SELECT_CATEGORIES_THEN_SUBCATEGORIES =
    "SELECT $CATEGORY_FIELDS_FROM_CATEGORIES_AND_CURRENCIES " +
            "WHERE $CURRENCY_MATCHES_CATEGORY " +
            "ORDER BY ${DbSchema.CATEGORY_SELECTED_PARENT_ID} ASC"

/**
 * Params:
 * 1. ID
 * 2. Title
 * 3. Currency ID
 * 4. Is income boolean
 * 5. Color scheme name
 * 6. Position
 */
private const val INSERT_CATEGORY =
    "INSERT INTO ${DbSchema.CATEGORIES_TABLE} " +
            "(" +
            "${DbSchema.ID}, " +
            "${DbSchema.CATEGORY_TITLE}, " +
            "${DbSchema.CATEGORY_CURRENCY_ID}, " +
            "${DbSchema.CATEGORY_PARENT_ID}, " +
            "${DbSchema.CATEGORY_IS_INCOME}, " +
            "${DbSchema.CATEGORY_COLOR_SCHEME}, " +
            "${DbSchema.CATEGORY_POSITION}, " +
            "${DbSchema.CATEGORY_IS_ARCHIVED} " +
            ") " +
            "VALUES(?, ?, ?, NULL, ?, ?, ?, 0)"

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
private const val INSERT_OR_REPLACE_SUBCATEGORY =
    "INSERT OR REPLACE INTO ${DbSchema.CATEGORIES_TABLE} " +
            "(" +
            "${DbSchema.ID}, " +
            "${DbSchema.CATEGORY_TITLE}, " +
            "${DbSchema.CATEGORY_CURRENCY_ID}, " +
            "${DbSchema.CATEGORY_PARENT_ID}, " +
            "${DbSchema.CATEGORY_IS_INCOME}, " +
            "${DbSchema.CATEGORY_COLOR_SCHEME}, " +
            "${DbSchema.CATEGORY_POSITION}, " +
            "${DbSchema.CATEGORY_IS_ARCHIVED} " +
            ") " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, 0)"

/**
 * Params:
 * 1. Title
 * 2. Color scheme name
 * 3. ID
 */
private const val UPDATE_CATEGORY_BY_ID =
    "UPDATE ${DbSchema.CATEGORIES_TABLE} SET " +
            "${DbSchema.CATEGORY_TITLE} = ?, " +
            "${DbSchema.CATEGORY_COLOR_SCHEME} = ? " +
            "WHERE ${DbSchema.ID} = ? "

/**
 * Params:
 * 1. Is archived boolean
 * 2. ID
 */
private const val UPDATE_ARCHIVED_BY_ID =
    "UPDATE ${DbSchema.CATEGORIES_TABLE} SET " +
            "${DbSchema.CATEGORY_IS_ARCHIVED} = ? " +
            "WHERE ${DbSchema.ID} = ?"
