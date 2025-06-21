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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryStats
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.ViewAmount
import java.math.BigInteger
import kotlin.random.Random

@Immutable
class ViewCategoryListItem(
    val title: String,
    val amount: ViewAmount,
    val isIncognito: Boolean,
    val colorScheme: ItemColorScheme,
    val source: Category? = null,
    val key: Any = source?.hashCode() ?: Random.nextInt(),
) {

    constructor(
        category: Category,
        amount: BigInteger,
        isIncognito: Boolean,
    ) : this(
        title = category.title,
        amount = ViewAmount(
            value = amount,
            currency = category.currency,
        ),
        colorScheme = category.colorScheme,
        isIncognito = isIncognito,
        source = category,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewCategoryListItem) return false

        if (title != other.title) return false
        if (amount != other.amount) return false
        if (isIncognito != other.isIncognito) return false
        if (colorScheme != other.colorScheme) return false
        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + isIncognito.hashCode()
        result = 31 * result + colorScheme.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }
}

fun List<Category>.toSortedIncognitoViewItemList(): List<ViewCategoryListItem> =
    sorted()
        .map { category ->
            ViewCategoryListItem(
                category = category,
                amount = BigInteger.ZERO,
                isIncognito = true,
            )
        }

private val statsComparator = compareBy(CategoryStats::first)

fun List<CategoryStats>.toSortedViewItemList(): List<ViewCategoryListItem> =
    sortedWith(statsComparator)
        .map { (category, amount) ->
            ViewCategoryListItem(
                category = category,
                amount = amount,
                isIncognito = false,
            )
        }
