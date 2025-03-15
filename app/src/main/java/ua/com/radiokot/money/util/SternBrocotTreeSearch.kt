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

/**
 * A binary search for Stern–Brocot tree.
 * Right direction is for bigger numbers, left – for smaller.
 *
 * @see goBetween
 *
 * @see <a href="https://en.wikipedia.org/wiki/Stern%E2%80%93Brocot_tree">Wikipedia</a>
 * @see <a href="https://begriffs.com/posts/2018-03-20-user-defined-order.html">Use case</a>
 */
class SternBrocotTreeSearch {
    /**
     * Current fraction numerator.
     */
    var numerator = 1L
        private set

    /**
     * Current fraction denominator.
     */
    var denominator = 1L
        private set

    private var closestRightN = 1L
    private var closestRightD = 0L

    private var closestLeftN = 0L
    private var closestLeftD = 1L

    /**
     * Current fraction decimal value.
     */
    val value: Double
        get() = numerator.toDouble() / denominator

    /**
     * Current node depth, starting from 0.
     */
    var depth: Int = 0
        private set

    /**
     * Goes deeper in the right direction (for bigger numbers).
     */
    fun goRight() = apply {
        val newN = numerator + closestRightN
        val newD = denominator + closestRightD

        closestLeftN = numerator
        closestLeftD = denominator

        numerator = newN
        denominator = newD

        depth++
    }

    /**
     * Goes deeper in the left direction (for small numbers).
     */
    fun goLeft() = apply {
        val newN = numerator + closestLeftN
        val newD = denominator + closestLeftD

        closestRightN = numerator
        closestRightD = denominator

        numerator = newN
        denominator = newD

        depth++
    }

    /**
     * Goes to the fraction laying within the given bounds (exclusively),
     * but not exceeding maximum allowed depth.
     * Minimal lower bound is 0.0
     * Maximal upper bound is [Double.POSITIVE_INFINITY]
     */
    fun goBetween(
        lowerBound: Double,
        upperBound: Double,
        maxDepth: Int = 2000,
    ) = apply {
        require(lowerBound < upperBound) {
            "Lower bound must be smaller than the upper one"
        }

        require(lowerBound >= 0.0) {
            "Lower bound can't be smaller than 0"
        }

        while (!(value > lowerBound && value < upperBound) && depth < maxDepth) {
            if (value <= lowerBound && value <= upperBound) {
                goRight()
            } else {
                goLeft()
            }
        }
    }

    override fun toString(): String {
        return "SternBrocotTreeSearch($numerator/$denominator, $value, depth=$depth)"
    }
}
