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
import ua.com.radiokot.money.categories.data.SubcategoryToUpdate
import kotlin.random.Random

@Immutable
class ViewSubcategoryToUpdateListItem(
    val title: String,
    val source: SubcategoryToUpdate?,
    val key: Any = source?.id ?: Random.nextInt(),
) {

    constructor(subcategoryToUpdate: SubcategoryToUpdate) : this(
        title = subcategoryToUpdate.title,
        source = subcategoryToUpdate,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewSubcategoryToUpdateListItem) return false

        if (title != other.title) return false
        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }
}
