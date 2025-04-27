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

package ua.com.radiokot.money.util

class SternBrocotTreeDescPositionHealer<T : Comparable<T>>(
    private val positionOf: (T) -> Double,
) {

    /**
     * @return **true** if there are no collisions and overflows in positions of the [items]
     */
    fun arePositionsHealthy(
        items: List<T>,
    ): Boolean {
        if (items.isEmpty()) {
            return true
        }

        return items
            .mapTo(mutableSetOf(), positionOf)
            .let { distinctPositions ->
                distinctPositions.size == items.size && distinctPositions.none { it <= 0 }
            }
    }

    /**
     * Updates positions of items preserving given order,
     * having the first item have the greatest position.
     *
     * @param items items with unhealthy positions, will be sorted internally
     * @param updatePosition called when certain item needs the position update
     */
    fun healPositions(
        items: List<T>,
        updatePosition: (item: T, newPosition: Double) -> Unit,
    ) {
        val sortedItems = items.sorted()
        val sternBrocotTree = SternBrocotTreeSearch()
        sortedItems.indices.reversed().forEach { itemIndex ->
            val item = sortedItems[itemIndex]
            val newPosition = sternBrocotTree.value
            if (newPosition != positionOf(item)) {
                updatePosition(item, newPosition)
            }
            sternBrocotTree.goRight()
        }
    }
}
