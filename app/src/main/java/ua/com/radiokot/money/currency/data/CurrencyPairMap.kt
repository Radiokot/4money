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
import java.math.MathContext

/**
 * A map keeping decimal prices of different currencies in one quote currency.
 * For example, prices of every world currency in USD, so the [quoteCode] is USD
 * and the [decimalPriceByBaseCode] contains USD price by currency code.
 *
 * @see get
 */
class CurrencyPairMap(
    private val quoteCode: String,
    decimalPriceByBaseCode: Map<String, BigDecimal> = emptyMap(),
) {
    private val decimalPriceByBaseCode: MutableMap<String, BigDecimal> =
        decimalPriceByBaseCode.toMutableMap()

    operator fun plusAssign(pair: Pair<String, BigDecimal>) {
        put(pair.first, pair.second)
    }

    fun put(
        baseCode: String,
        decimalPrice: BigDecimal,
    ): BigDecimal? =
        decimalPriceByBaseCode.put(baseCode, decimalPrice)

    /**
     * @return a pair for given [base] and [quote] currencies having its price
     * found in this map or calculated through a double conversion.
     * If the price can't be calculated, null is returned.
     *
     * For example, if this is a USD map keeping BTC and EUR prices,
     * with this method you can get the following pairs:
     * - BTC:USD and vice versa
     * - EUR:USD and vice versa
     * - BTC:EUR and vice versa
     * - Also, for convenience, BTC:BTC, USD:USD and EUR:EUR with the price of 1
     */
    operator fun get(base: Currency, quote: Currency): CurrencyPair? {
        val basePrice =
            if (quoteCode == base.code || base.code == quote.code)
                BigDecimal.ONE
            else
                decimalPriceByBaseCode[base.code]
                    ?: return null

        val quotePrice =
            if (quoteCode == quote.code || base.code == quote.code)
                BigDecimal.ONE
            else
                decimalPriceByBaseCode[quote.code]
                    ?: return null

        return CurrencyPair(
            base = base,
            quote = quote,
            decimalPrice = basePrice.divide(quotePrice, MathContext.DECIMAL64)
        )
    }
}
