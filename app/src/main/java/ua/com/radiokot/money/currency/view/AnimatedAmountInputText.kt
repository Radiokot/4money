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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import java.math.BigInteger
import kotlin.math.max

/**
 * A container displaying [AmountInputState] input with the currency sign.
 * - Centered horizontally;
 * - Having the currency sign aligned by the input baseline;
 * - Having the input animated on change.
 */
@Composable
fun AnimatedAmountInputText(
    modifier: Modifier,
    amountInputState: AmountInputState,
) {
    Layout(
        content = {
            AnimatedContent(
                targetState = amountInputState.inputText,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = EnterTransition.None,
                        initialContentExit = ExitTransition.None,
                    )
                },
                label = "amount-input-text",
            ) { amountInputText ->
                Text(
                    text = amountInputText,
                    textAlign = TextAlign.End,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    singleLine = true,
                    overflow = TextOverflow.StartEllipsis
                )
            }

            Text(
                text = amountInputState.currencySignText,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        measurePolicy = { measurables, constraints ->
            val sign = measurables[1].measure(
                constraints.copy(
                    minWidth = 0
                )
            )
            val input = measurables[0].measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth - sign.width
                )
            )
            val textWidth = sign.width + input.width
            val containerWidth = max(constraints.maxWidth, textWidth)
            layout(containerWidth, input.height) {
                val startX = (containerWidth - textWidth) / 2
                input.placeRelative(
                    x = startX,
                    y = 0,
                )
                sign.placeRelative(
                    x = startX + input.width,
                    y = input[FirstBaseline] - sign[FirstBaseline],
                )
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun Preview() {
    AnimatedAmountInputText(
        amountInputState = rememberAmountInputState(
            currency = ViewCurrency(
                symbol = "$",
                precision = 2,
            ),
            initialValue = BigInteger("1223"),
        ),
        modifier = Modifier
            .width(100.dp)
    )
}
