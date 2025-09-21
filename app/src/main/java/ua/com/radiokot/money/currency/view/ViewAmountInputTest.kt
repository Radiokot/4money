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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

        AmountKeyboard(
            inputState = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
    }
}
