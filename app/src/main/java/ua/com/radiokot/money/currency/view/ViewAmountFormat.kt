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
    private val decimalSeparatorsRegex by lazy {
        "[.,٬٫]".toRegex()
    }
    private val decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale)
    private val allowedInputCharPredicate = { it: Char ->
        it.isDigit()
                || it == decimalFormatSymbols.minusSign
                || it == decimalFormatSymbols.decimalSeparator
    }

    operator fun invoke(amount: ViewAmount): AnnotatedString = buildAnnotatedString {
        val (integerPart, decimalPart) = amount.value
            .divideAndRemainder(BigInteger.TEN.pow(amount.currency.precision))

        pushStyle(
            style = SpanStyle(
                color = when (amount.value.signum()) {
                    1 -> Color.Black
                    -1 -> Color.Red
                    else -> Color.LightGray
                }
            )
        )

        if (amount.value.signum() < 0) {
            append(decimalFormatSymbols.minusSign)
        }

        append(NumberFormat.getNumberInstance(locale).format(integerPart.abs()))

        if (decimalPart.signum() != 0) {
            append(decimalFormatSymbols.decimalSeparator)
            append(
                decimalPart.toString()
                    .trimStart(decimalFormatSymbols.minusSign)
                    .padStart(amount.currency.precision, '0')
            )
        }

        pushStyle(
            style = SpanStyle(
                fontSize = 0.8.em,
            ),
        )

        append(" ${amount.currency.symbol}")
    }

    fun formatForInput(
        value: BigInteger,
        currency: ViewCurrency,
    ): String = buildString {
        val (integerPart, decimalPart) = value
            .divideAndRemainder(BigInteger.TEN.pow(currency.precision))

        if (value.signum() < 0) {
            append(decimalFormatSymbols.minusSign)
        }

        append(
            integerPart
                .toString()
                .trimStart(decimalFormatSymbols.minusSign)
        )

        if (decimalPart.signum() != 0) {
            append(decimalFormatSymbols.decimalSeparator)
            append(
                decimalPart
                    .toString()
                    .trimStart(decimalFormatSymbols.minusSign)
                    .padStart(currency.precision, '0')
            )
        }
    }

    fun formatForInput(
        amount: ViewAmount,
    ): String =
        formatForInput(
            value = amount.value,
            currency = amount.currency,
        )

    fun parseInput(
        input: String,
        currency: ViewCurrency,
    ): BigInteger? {
        if (input.isEmpty()) {
            return BigInteger.ZERO
        }

        if (!input.all(allowedInputCharPredicate)
            || input.lastIndexOf(decimalFormatSymbols.minusSign) > 0
        ) {
            return null
        }

        val splitByDecimalSeparator = input.split(decimalFormatSymbols.decimalSeparator)

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
                    .let { if (it == decimalFormatSymbols.minusSign.toString() || it.isEmpty()) "0" else it }
                    .let(::BigInteger)
            }.getOrNull() ?: return null

            val decimalPart = runCatching {
                (splitByDecimalSeparator.getOrNull(1) ?: "0")
                    .trimEnd('0')
                    .padEnd(currency.precision, '0')
                    .takeIf { it.length <= currency.precision }
                    ?.let(::BigInteger)
            }.getOrNull() ?: return null

            return if (input.startsWith(decimalFormatSymbols.minusSign))
                integerPart * BigInteger.TEN.pow(currency.precision) - decimalPart
            else
                integerPart * BigInteger.TEN.pow(currency.precision) + decimalPart
        }
    }

    /**
     * A method suitable when accepting input from a soft keyboard.
     *
     * @return a [rawInput] where all the possible decimal separators have been replaced
     * with the one of the current locale.
     */
    fun unifyDecimalSeparators(
        rawInput: String,
    ): String =
        rawInput.replace(decimalSeparatorsRegex, decimalFormatSymbols.decimalSeparator.toString())
}
