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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger
import kotlin.reflect.KMutableProperty0

@Stable
class AmountInputState(
    val currency: ViewCurrency,
    initialValue: BigInteger,
    private val format: ViewAmountFormat,
) {

    private val currencyPrecisionMultiplier =
        BigInteger.TEN.pow(currency.precision)

    private var valueA: String by mutableStateOf(
        format.formatForInput(
            value = initialValue,
            currency = currency,
        )
    )

    private var operator: Operator? by mutableStateOf(null)

    private var valueB: String by mutableStateOf("")

    val text: String by derivedStateOf {
        val operator = operator
        val value =
            if (operator != null)
                "$valueA ${operator.symbol} $valueB"
            else
                valueA

        "$value ${currency.symbol}"
    }

    val valueFlow: Flow<BigInteger> =
        snapshotFlow {
            format.parseInput(valueA, currency)!!
        }

    val decimalSeparator: Char =
        format.decimalSeparator

    fun acceptInput(symbol: Char) {
        val symbolOperator =
            runCatching { Operator.valueOfSymbol(symbol) }
                .getOrNull()
        val currentOperator = operator
        val currentValueProperty =
            if (currentOperator != null)
                this::valueB
            else
                this::valueA

        when {
            symbol == '<' -> {

                if (valueB.isNotEmpty()) {
                    valueB = valueB.dropLast(1)
                } else if (currentOperator != null) {
                    operator = null
                } else if (valueA != "0") {
                    valueA = valueA
                        .dropLast(1)
                        .orZeroIfEmpty()
                    if (valueA.startsWith(format.minusSign)) {
                        valueA = "0"
                    }
                }
            }

            symbol.isDigit() -> {

                val currentValue = currentValueProperty.get()

                if (currentValue.isEmpty() || currentValue == "0") {
                    currentValueProperty.set(symbol.toString())
                } else {
                    currentValueProperty.setIfValidInput(
                        input = currentValue + symbol,
                    )
                }
            }

            symbol == decimalSeparator -> {

                currentValueProperty.setIfValidInput(
                    input = currentValueProperty.get().orZeroIfEmpty() + symbol,
                )
            }

            symbolOperator != null -> {

                if (valueB.isEmpty()) {
                    operator = symbolOperator
                    if (valueA.endsWith(decimalSeparator)) {
                        valueA = valueA.dropLast(1)
                    }
                } else {
                    evaluate()
                    operator = symbolOperator
                }
            }

            symbol == '=' -> {
                evaluate()
            }
        }
    }

    private fun evaluate() {
        val intValueA = format.parseInput(valueA, currency)
            ?: BigInteger.ZERO
        val intValueB = format.parseInput(valueB, currency)
            ?: BigInteger.ZERO

        val result = when (this.operator) {
            null ->
                intValueA

            Operator.Divide ->
                if (intValueB == BigInteger.ZERO)
                    BigInteger.ZERO
                else
                    intValueA * currencyPrecisionMultiplier / intValueB

            Operator.Multiply ->
                intValueA * intValueB / currencyPrecisionMultiplier

            Operator.Minus ->
                intValueA - intValueB

            Operator.Plus ->
                intValueA + intValueB
        }

        valueA = format.formatForInput(
            value = result,
            currency = currency,
        )
        operator = null
        valueB = ""
    }

    private fun String.orZeroIfEmpty(): String =
        takeIf(String::isNotEmpty) ?: "0"

    private fun KMutableProperty0<String>.setIfValidInput(input: String) {
        if (format.parseInput(input, currency) != null) {
            set(input)
        }
    }

    enum class Operator(val symbol: Char) {
        Divide('÷'),
        Multiply('×'),
        Minus('−'),
        Plus('+'),
        ;

        companion object {
            fun valueOfSymbol(symbol: Char): Operator = when (symbol) {
                Divide.symbol -> Divide
                Multiply.symbol -> Multiply
                Minus.symbol -> Minus
                Plus.symbol -> Plus
                else -> error("No such operator $symbol")
            }
        }
    }
}

@Composable
fun rememberViewAmountInputState(
    currency: ViewCurrency,
    initialValue: BigInteger,
): AmountInputState {

    val locale = LocalConfiguration.current.locales.get(0)

    return remember(locale, currency) {
        AmountInputState(
            currency = currency,
            initialValue = initialValue,
            format = ViewAmountFormat(locale),
        )
    }
}
