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

import org.junit.Assert
import org.junit.Test

class SternBrocotTreeDescPositionHealerTest {

    @Test
    fun arePositionsHealthy_TrueIfTheyAre() {
        val healer = SternBrocotTreeDescPositionHealer(Item::position)

        listOf(
            Item("a", 1.0),
            Item("b", 2.0),
            Item("c", 3.0),
        ).apply {
            Assert.assertTrue(healer.arePositionsHealthy(this))
        }

        listOf(
            Item("a", 1.0),
        ).apply {
            Assert.assertTrue(healer.arePositionsHealthy(this))
        }

        listOf(
            Item("a", 1.0),
            Item("b", 20.4),
            Item("c", 20.40000001),
        ).apply {
            Assert.assertTrue(healer.arePositionsHealthy(this))
        }
    }

    @Test
    fun arePositionsHealthy_FalseIfTheyAreNot() {
        val healer = SternBrocotTreeDescPositionHealer(Item::position)

        listOf(
            Item("a", 1.0),
            Item("b", 2.0),
            Item("c", 2.0),
        ).apply {
            Assert.assertFalse(healer.arePositionsHealthy(this))
        }

        listOf(
            Item("a", 1.0),
            Item("b", 0.0),
            Item("c", 2.0),
        ).apply {
            Assert.assertFalse(healer.arePositionsHealthy(this))
        }

        listOf(
            Item("a", -1.0),
            Item("b", 3.0),
            Item("c", 2.0),
        ).apply {
            Assert.assertFalse(healer.arePositionsHealthy(this))
        }
    }

    @Test
    fun healPositions() {
        val healer = SternBrocotTreeDescPositionHealer(Item::position)
        val desiredOrder = listOf(
            Item("a", 3.0),
            Item("b", 2.0),
            Item("c", 1.0),
        )

        fun healAndCheck(items: MutableList<Item>) = with(items) {
            healer.healPositions(this) { item, newPosition ->
                Assert.assertNotEquals(
                    "The update must only be invoked if the new position is different",
                    get(indexOf(item)).position,
                    newPosition
                )
                set(indexOf(item), Item(item.name, newPosition))
            }
            sort()
            Assert.assertEquals(desiredOrder, this)
            Assert.assertTrue(healer.arePositionsHealthy(this))
        }

        mutableListOf(
            Item("a", 3.0),
            Item("b", 2.0),
            Item("c", 2.0),
        ).also(::healAndCheck)

        mutableListOf(
            Item("a", 2.0),
            Item("b", 2.0),
            Item("c", 1.0),
        ).also(::healAndCheck)

        mutableListOf(
            Item("a", 1.0),
            Item("b", 1.0),
            Item("c", 1.0),
        ).also(::healAndCheck)

        mutableListOf(
            Item("a", 1.0),
            Item("b", 0.0),
            Item("c", -1.0),
        ).also(::healAndCheck)
    }

    private class Item(
        val name: String,
        val position: Double,
    ) : Comparable<Item> {

        override fun compareTo(other: Item): Int =
            other.position.compareTo(this.position)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Item) return false

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}
