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

import androidx.compose.runtime.Immutable
import ua.com.radiokot.money.currency.data.Amount
import ua.com.radiokot.money.currency.data.Currency
import java.math.BigInteger

/**
 * Amount presentable to the user.
 */
@Immutable
class ViewAmount(
    val value: BigInteger,
    val currency: ViewCurrency,
) {
    constructor(
        value: BigInteger,
        currency: Currency,
    ) : this(
        value = value,
        currency = ViewCurrency(currency),
    )

    constructor(
        amount: Amount,
    ) : this(
        value = amount.value,
        currency = amount.currency,
    )

    override fun toString(): String =
        "$value * 10^-${currency.precision} ${currency.symbol}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewAmount) return false

        if (value != other.value) return false
        if (currency != other.currency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + currency.hashCode()
        return result
    }
}
