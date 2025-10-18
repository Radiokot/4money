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
import androidx.compose.ui.unit.em
import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Formats and parses view amounts.
 *
 * @param locale determining separators
 */
class ViewAmountFormat(
    private val locale: Locale,
) {
    private val decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale)
    val minusSign: Char =
        decimalFormatSymbols.minusSign
    val decimalSeparator: Char =
        decimalFormatSymbols.decimalSeparator
    val groupingSeparator: Char =
        decimalFormatSymbols.groupingSeparator
    val currencySymbolSpanStyle =
        SpanStyle(
            fontSize = 0.8.em,
        )
    private val format = NumberFormat.getNumberInstance(locale)

    operator fun invoke(
        amount: ViewAmount,
        customColor: Color? = null,
    ): AnnotatedString =
        invoke(
            value = amount.value,
            currency = amount.currency,
            customColor = customColor,
        )

    operator fun invoke(
        value: BigInteger,
        currency: ViewCurrency,
        customColor: Color? = null,
    ): AnnotatedString = buildAnnotatedString {
        val (integerPart, decimalPart) = value
            .divideAndRemainder(BigInteger.TEN.pow(currency.precision))

        pushStyle(
            style = SpanStyle(
                color = customColor ?: when (value.signum()) {
                    1 -> Color(0xff50af99)
                    -1 -> Color(0xffd85e8c)
                    else -> Color(0xff757575)
                }
            )
        )

        if (value.signum() < 0) {
            append(minusSign)
        }

        append(format.format(integerPart.abs()))

        if (decimalPart.signum() != 0) {
            append(decimalSeparator)
            append(
                decimalPart.toString()
                    .trimStart(minusSign)
                    .padStart(currency.precision, '0')
            )
        }

        pushStyle(currencySymbolSpanStyle)

        append(' ')
        append(currency.symbol)
    }

    fun formatInput(
        input: String,
    ): String {

        val splitByDecimalSeparator = input.split(decimalSeparator)

        if (splitByDecimalSeparator.isEmpty()) {
            return input
        }

        val parsedIntegerPart = runCatching {
            format.parse(splitByDecimalSeparator[0])
        }.getOrNull()

        if (parsedIntegerPart == null) {
            return input
        }

        return buildString {
            append(format.format(parsedIntegerPart))
            if (splitByDecimalSeparator.size == 2) {
                append(decimalSeparator)
                append(splitByDecimalSeparator[1])
            }
        }
    }

    fun formatInput(
        value: BigInteger,
        currency: ViewCurrency,
    ): String = buildString {
        val (integerPart, decimalPart) = value
            .divideAndRemainder(BigInteger.TEN.pow(currency.precision))

        // If the value is < 1, minus arithmetically ends up in the decimal part.
        if (value.signum() < 0) {
            append(minusSign)
        }

        append(format.format(integerPart.abs()))

        if (decimalPart.signum() != 0) {
            append(decimalSeparator)
            append(
                decimalPart
                    .toString()
                    .trimStart(minusSign)
                    .padStart(currency.precision, '0')
                    .trimEnd('0')
            )
        }
    }

    fun parseInput(
        input: String,
        currency: ViewCurrency,
    ): BigInteger? {
        if (input.isEmpty()) {
            return BigInteger.ZERO
        }

        val splitByDecimalSeparator = input
            .replace(groupingSeparator.toString(), "")
            .split(decimalSeparator)

        if (currency.precision == 0) {
            if (splitByDecimalSeparator.size > 1) {
                return null
            }

            return runCatching {
                BigInteger(splitByDecimalSeparator[0])
            }.getOrNull()
        } else {
            if (splitByDecimalSeparator.size > 2) {
                return null
            }

            val integerPart = runCatching {
                splitByDecimalSeparator[0]
                    .let { if (it == minusSign.toString() || it.isEmpty()) "0" else it }
                    .let(::BigInteger)
            }.getOrNull() ?: return null

            val decimalPart = runCatching {
                (splitByDecimalSeparator.getOrNull(1) ?: "0")
                    .trimEnd('0')
                    .padEnd(currency.precision, '0')
                    .takeIf { it.length <= currency.precision }
                    ?.let(::BigInteger)
            }.getOrNull() ?: return null

            return if (input.startsWith(minusSign))
                integerPart * BigInteger.TEN.pow(currency.precision) - decimalPart
            else
                integerPart * BigInteger.TEN.pow(currency.precision) + decimalPart
        }
    }
}
