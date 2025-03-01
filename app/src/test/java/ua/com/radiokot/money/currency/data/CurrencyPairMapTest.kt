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
import java.math.BigDecimal
import java.math.RoundingMode

class CurrencyPairMapTest {

    private val btc = Currency(
        code = "BTC",
        symbol = "B",
        precision = 8
    )
    private val usd = Currency(
        code = "USD",
        symbol = "$",
        precision = 2,
    )
    private val uah = Currency(
        code = "UAH",
        symbol = "г",
        precision = 2,
    )

    @Test
    fun getPair_IfHasDirectPrice() {
        val map = CurrencyPairMap(
            quoteCode = usd.code,
            decimalPriceByBaseCode = mapOf(
                btc.code to BigDecimal("84000")
            )
        )

        Assert.assertEquals(
            BigDecimal("84000"),
            map[btc, usd]?.decimalPrice
        )
    }

    @Test
    fun getPair_IfHasReversePrice() {
        val map = CurrencyPairMap(
            quoteCode = usd.code,
            decimalPriceByBaseCode = mapOf(
                uah.code to BigDecimal("0.024")
            )
        )

        Assert.assertEquals(
            BigDecimal("41.67"),
            map[usd, uah]?.decimalPrice?.setScale(2, RoundingMode.HALF_UP)
        )
    }

    @Test
    fun getPair_IfEachHasPrice() {
        val map = CurrencyPairMap(
            quoteCode = usd.code,
            decimalPriceByBaseCode = mapOf(
                uah.code to BigDecimal("0.024"),
                btc.code to BigDecimal("84000")
            )
        )

        Assert.assertEquals(
            BigDecimal("3500000.00"),
            map[btc, uah]?.decimalPrice?.setScale(2, RoundingMode.HALF_UP)
        )
        Assert.assertEquals(
            BigDecimal("0.0000002857"),
            map[uah, btc]?.decimalPrice?.setScale(10, RoundingMode.HALF_UP)
        )
    }

    @Test
    fun getPair_IfWithItself() {
        val map = CurrencyPairMap(
            quoteCode = usd.code,
            decimalPriceByBaseCode = mapOf(
            )
        )

        Assert.assertEquals(
            BigDecimal.ONE,
            map[uah, uah]?.decimalPrice
        )
    }

    @Test
    fun getNoPair_IfNoPrice() {
        val map = CurrencyPairMap(
            quoteCode = usd.code,
            decimalPriceByBaseCode = mapOf(
                btc.code to BigDecimal("84000")
            )
        )

        Assert.assertNull(
            map[btc, uah]?.decimalPrice
        )
    }
}
