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
import java.math.BigInteger

class CurrencyPairTest {

    @Test
    fun eurUsd() {
        val pair = CurrencyPair(
            base = Currency(
                code = "EUR",
                symbol = "E",
                precision = 2
            ),
            quote = Currency(
                code = "UAH",
                symbol = "г",
                precision = 2,
            ),
            decimalPrice = BigDecimal("43.03")
        )

        Assert.assertEquals(
            "129090",
            pair.baseToQuote(BigInteger("3000")).toString()
        )

        Assert.assertEquals(
            "116",
            pair.quoteToBase(BigInteger("5000")).toString()
        )
    }

    @Test
    fun jpyUsd() {
        val pair = CurrencyPair(
            base = Currency(
                code = "JPY",
                symbol = "Y",
                precision = 2
            ),
            quote = Currency(
                code = "USD",
                symbol = "$",
                precision = 2,
            ),
            decimalPrice = BigDecimal("0.0066")
        )

        Assert.assertEquals(
            "531",
            pair.baseToQuote(BigInteger("80500")).toString()
        )

        Assert.assertEquals(
            "4848",
            pair.quoteToBase(BigInteger("32")).toString()
        )
    }


    @Test
    fun btcUsd() {
        val pair = CurrencyPair(
            base = Currency(
                code = "BTC",
                symbol = "B",
                precision = 8
            ),
            quote = Currency(
                code = "USD",
                symbol = "$",
                precision = 2,
            ),
            decimalPrice = BigDecimal("97774")
        )

        Assert.assertEquals(
            "9",
            pair.baseToQuote(BigInteger("100")).toString()
        )

        Assert.assertEquals(
            "1022",
            pair.quoteToBase(BigInteger("100")).toString()
        )
    }

    @Test
    fun ccdUsd() {
        val pair = CurrencyPair(
            base = Currency(
                code = "CCD",
                symbol = "C",
                precision = 6,
            ),
            quote = Currency(
                code = "USD",
                symbol = "$",
                precision = 2,
            ),
            decimalPrice = BigDecimal("0.003022")
        )

        Assert.assertEquals(
            "435",
            pair.baseToQuote(BigInteger("1442444421")).toString()
        )

        Assert.assertEquals(
            "8272667107",
            pair.quoteToBase(BigInteger("2500")).toString()
        )

        Assert.assertEquals(
            "3309066",
            pair.quoteToBase(BigInteger("1")).toString()
        )
    }
}
