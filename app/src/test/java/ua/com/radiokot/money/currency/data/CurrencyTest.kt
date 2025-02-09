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

package ua.com.radiokot.money.currency.data

import org.junit.Assert
import org.junit.Test

class CurrencyTest {

    @Test
    fun equals_IfCodeEquals() {
        val currency1 = Currency(
            code = "USD",
            symbol = "$",
            precision = 2
        )

        val currency2 = Currency(
            code = "USD",
            symbol = "US$",
            precision = 2
        )

        Assert.assertEquals(currency1, currency2)
    }

    @Test
    fun notEquals_IfCodeNotEquals() {
        val currency1 = Currency(
            code = "USD",
            symbol = "$",
            precision = 2
        )

        val currency2 = Currency(
            code = "EUR",
            symbol = "€",
            precision = 2
        )

        Assert.assertNotEquals(currency1, currency2)
    }
}
