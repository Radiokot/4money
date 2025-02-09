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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import ua.com.radiokot.money.currency.data.Currency
import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Amount presentable to the user.
 */
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

    fun format(locale: Locale): AnnotatedString = buildAnnotatedString {
        val precision = currency.precision.toInt()
        val (integerPart, decimalPart) = value.divideAndRemainder(BigInteger.TEN.pow(precision))

        pushStyle(
            style = SpanStyle(
                color = when (value.signum()) {
                    1 -> Color.Black
                    -1 -> Color.Red
                    else -> Color.LightGray
                }
            )
        )

        append(NumberFormat.getNumberInstance(locale).format(integerPart))

        if (decimalPart.signum() != 0) {
            append(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
            append(
                decimalPart.toString()
                    .padStart(precision, '0')
            )
        }

        pushStyle(
            style = SpanStyle(
                fontSize = 13.sp,
            ),
        )

        append(" ${currency.symbol}")
    }

    override fun toString(): String =
        "$value ${currency.symbol}"
}
