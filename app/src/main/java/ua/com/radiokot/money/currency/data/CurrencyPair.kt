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

import java.math.BigDecimal
import java.math.BigInteger

/**
 * A price quote between two different currencies.
 */
class CurrencyPair(
    /**
     * Currency being exchanged.
     */
    val base: Currency,

    /**
     * Currency used to measure the price of the exchanged currency.
     */
    val quote: Currency,

    /**
     * Real world price of the base currency unit (exchange rate).
     * How much **one unit** (1 * 10^precision) of base currency is worth in **units** of quote currency.
     * For example, 0.0066 for JPY:USD or 97774 for BTC:USD.
     */
    val decimalPrice: BigDecimal,
) {
    init {
        require(decimalPrice.signum() == 1) {
            "Price must be positive"
        }
    }

    fun baseToQuote(baseAmount: BigInteger): BigInteger =
        baseAmount
            .multiply(
                decimalPrice.movePointRight(quote.precision * 2 + base.precision).toBigInteger()
            )
            // Keep not reduced to preserve precision.
            .divide(BigInteger.TEN.pow(base.precision * 2 + quote.precision))

    fun quoteToBase(quoteAmount: BigInteger): BigInteger =
        quoteAmount
            .multiply(BigInteger.TEN.pow(base.precision * 2 + quote.precision))
            .divide(
                // Keep not reduced to preserve precision.
                decimalPrice.movePointRight(quote.precision * 2 + base.precision).toBigInteger()
            )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurrencyPair) return false

        if (base != other.base) return false
        if (quote != other.quote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + quote.hashCode()
        return result
    }
}
