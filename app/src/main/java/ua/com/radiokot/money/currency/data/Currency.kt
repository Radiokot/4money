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

class Currency(
    /**
     * Identifier of the currency.
     * An [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217#Active_codes_(list_one)) code,
     * or a custom one code.
     */
    val code: String,

    /**
     * Short symbol, like ₿ or $.
     */
    val symbol: String,

    /**
     * Non-negative number of digits after the decimal point,
     * not greater than 18.
     *
     * @see getDecimalAmount
     */
    val precision: Int,
) {
    init {
        require(code.isNotEmpty()) {
            "Code must not be empty"
        }

        require(precision >= 0) {
            "Precision must not be negative"
        }

        require(precision <= 18) {
            "Precision must not be greater than 18"
        }
    }

    /**
     * @return decimal representation of the given [amount]
     * considering currency's [precision].
     */
    fun getDecimalAmount(amount: BigInteger): BigDecimal =
        BigDecimal(amount, precision)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Currency) return false

        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}
