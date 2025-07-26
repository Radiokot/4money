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

package ua.com.radiokot.money.uikit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import java.math.BigInteger

@Composable
fun AmountInputField(
    modifier: Modifier = Modifier,
    value: State<BigInteger>,
    currency: ViewCurrency,
    amountFormat: ViewAmountFormat,
    onNewValueParsed: (BigInteger) -> Unit,
    onKeyboardSubmit: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Done,
) {
    var lastEnteredTextFieldValue by remember {
        mutableStateOf(TextFieldValue())
    }

    var lastParsedValue by remember {
        mutableStateOf(BigInteger.ZERO)
    }

    val textFieldValue by remember {
        derivedStateOf {
            val outerValue = value.value

            // Discard user's input if the new value
            // doesn't match what they entered.
            // Otherwise, use the last entered value
            // which may be something like "0." or "-",
            // so it is technically 0 but actually not.
            if (outerValue != lastParsedValue) {
                val text = amountFormat
                    .formatForInput(
                        value = outerValue,
                        currency = currency,
                    )
                TextFieldValue(
                    text = text,
                    selection = TextRange(text.length),
                )
            } else {
                lastEnteredTextFieldValue
            }
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val cleanedUpText = amountFormat
                .unifyDecimalSeparators(newValue.text)
            val parsedValue = amountFormat.parseInput(
                input = cleanedUpText,
                currency = currency,
            )

            if (parsedValue != null) {
                lastParsedValue = parsedValue
                lastEnteredTextFieldValue = newValue.copy(
                    text = cleanedUpText,
                )
                onNewValueParsed(parsedValue)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions {
            onKeyboardSubmit()
        },
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.DarkGray,
            )
            .padding(12.dp)
    )
}

@Composable
@Preview
private fun AmountInputFieldPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class) amount: ViewAmount,
) = Column(
    modifier = Modifier
        .padding(16.dp)
) {
    BasicText(
        text = "$amount: ",
        modifier = Modifier
            .padding(vertical = 12.dp)
    )
    AmountInputField(
        value = amount.value.let(::mutableStateOf),
        currency = amount.currency,
        amountFormat = ViewAmountFormat(
            locale = LocalConfiguration.current.locales.get(0),
        ),
        onNewValueParsed = {},
        onKeyboardSubmit = {}
    )
}
