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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import java.math.BigInteger

@Composable
@Preview
fun ViewAmountInputTest() {

    val state = rememberViewAmountInputState(
        currency = ViewCurrency(
            symbol = "$",
            precision = 2,
        ),
        initialValue = BigInteger.ZERO,
    )

    Column {

        Text(
            text = state.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ButtonsRow(
            state,
            ViewAmountInputState.Operator.Divide.symbol,
            '7',
            '8',
            '9',
        )
        ButtonsRow(
            state,
            ViewAmountInputState.Operator.Multiply.symbol,
            '4',
            '5',
            '6',
        )
        ButtonsRow(
            state,
            ViewAmountInputState.Operator.Minus.symbol,
            '1',
            '2',
            '3',
        )
        ButtonsRow(
            state,
            ViewAmountInputState.Operator.Plus.symbol,
            state.decimalSeparator,
            '0',
            '<',
        )
        Row {
            Spacer(modifier = Modifier.width(ButtonSize))
            Button(
                symbol = '=',
                modifier = Modifier
                    .width(ButtonSize * 3)
                    .clickable {
                        state.acceptInput('=')
                    }
            )
        }
    }
}

@Composable
fun Button(
    modifier: Modifier = Modifier,
    symbol: Char,
) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .size(ButtonSize)
        .border(
            width = 1.dp,
            shape = RoundedCornerShape(8.dp),
            color = Color.Black,
        )
) {
    Text(text = symbol.toString())
}

@Composable
fun ButtonsRow(
    state: ViewAmountInputState,
    vararg symbols: Char,
) = Row {
    symbols.forEach { symbol ->
        key(symbol) {
            Button(
                symbol = symbol,
                modifier = Modifier
                    .clickable {
                        state.acceptInput(symbol)
                    }
            )
        }
    }
}

private val ButtonSize = 42.dp
