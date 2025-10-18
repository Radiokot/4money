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

package ua.com.radiokot.money.currency.view


import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.util.Locale

class ViewAmountFormatTest {
    private val usd = ViewCurrency(
        symbol = "$",
        precision = 2,
    )
    private val btc = ViewCurrency(
        symbol = "B",
        precision = 8,
    )

    @Test
    fun formatForInput() {
        val format = ViewAmountFormat(
            locale = Locale.ENGLISH,
        )
        val formatFr = ViewAmountFormat(
            locale = Locale.FRENCH,
        )

        Assert.assertEquals(
            "-0.0000005",
            format.formatInput(
                value = BigInteger("-50"),
                currency = btc,
            )
        )
        Assert.assertEquals(
            "10 000 000,05",
            formatFr.formatInput(
                value = BigInteger("1000000005"),
                currency = usd,
            )
        )
        Assert.assertEquals(
            "10,000,000.05",
            format.formatInput(
                value = BigInteger("1000000005"),
                currency = usd,
            )
        )
        Assert.assertEquals(
            "1,05",
            formatFr.formatInput(
                value = BigInteger("105"),
                currency = usd,
            )
        )
        Assert.assertEquals(
            "1.05",
            format.formatInput(
                value = BigInteger("105"),
                currency = usd,
            )
        )
    }

    @Test
    fun parseInput_IfCorrect() {
        val format = ViewAmountFormat(
            locale = Locale.ENGLISH,
        )
        val formatFr = ViewAmountFormat(
            locale = Locale.FRENCH,
        )

        Assert.assertEquals(
            BigInteger("0"),
            format.parseInput(
                input = "-",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("0"),
            format.parseInput(
                input = "-0.",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("0"), format.parseInput(
                input = "-.",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("-50"),
            format.parseInput(
                input = "-.5",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("-50"),
            format.parseInput(
                input = "-0.5",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("-104"),
            format.parseInput(
                input = "-1.04",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("505000"),
            format.parseInput(
                input = "0.005050",
                currency = btc,
            )
        )
        Assert.assertEquals(
            BigInteger("101"),
            format.parseInput(
                input = "1.01",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("1"),
            format.parseInput(
                input = "0.01",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("1"),
            format.parseInput(
                input = ".01",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("10"),
            format.parseInput(
                input = ".1",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("5200"),
            format.parseInput(
                input = "52.",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("5234"),
            format.parseInput(
                input = "52.34",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("0"),
            format.parseInput(
                input = "0",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("0"),
            format.parseInput(
                input = "000000.00",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("0"),
            format.parseInput(
                input = "000000.0000000",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("0"),
            format.parseInput(
                input = "",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("38050202415100"),
            format.parseInput(
                input = "380,502,024,151",
                currency = usd,
            )
        )
        Assert.assertEquals(
            BigInteger("38050202415121"),
            formatFr.parseInput(
                input = "380 502 024 151,21",
                currency = usd,
            )
        )
    }

    @Test
    fun notParseInput_IfIncorrect() {
        val format = ViewAmountFormat(
            locale = Locale.ENGLISH,
        )

        Assert.assertNull(
            format.parseInput(
                input = "2.43$", // Input must not contain the currency symbol.
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "1 25",
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "1. 25",
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "1 .25",
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "O", // letter O
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "9.99.99",
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = ".999999",
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "--1.04",
                currency = usd,
            )
        )
        Assert.assertNull(
            format.parseInput(
                input = "1.-04",
                currency = usd,
            )
        )
    }
}
