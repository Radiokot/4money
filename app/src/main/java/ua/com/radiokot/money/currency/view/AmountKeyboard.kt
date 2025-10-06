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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import kotlinx.coroutines.launch
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.uikit.ScaleIndication
import java.math.BigInteger

@Composable
fun AmountKeyboard(
    modifier: Modifier = Modifier,
    inputState: AmountInputState,
    colorScheme: ItemColorScheme,
    mainAction: AmountKeyboardMainAction = AmountKeyboardMainAction.Done,
    onMainActionClicked: ((AmountKeyboardMainAction) -> Unit)? = null,
) = BoxWithConstraints(
    modifier = modifier,
) {

    val buttonGap = 8.dp
    val buttonWidth = (maxWidth - buttonGap * 4) / 5
    val buttonHeight = (maxHeight - buttonGap * 3) / 4
    val actionBackground = Modifier.background(Color(0xfff3f0f6))

    val hapticFeedback = LocalHapticFeedback.current
    val onButtonClicked = remember(inputState) {
        { symbol: Char ->
            hapticFeedback.performHapticFeedback(
                HapticFeedbackType.KeyboardTap
            )
            inputState.acceptInput(symbol)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val animateClear = remember(inputState) {
        {
            coroutineScope.launch {
                inputState.animateClear()
            }
        }
    }

    CompositionLocalProvider(
        LocalIndication provides remember(::ScaleIndication),
    ) {

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer()
                    .weight(4f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        symbol = AmountInputState.Operator.Divide.symbol,
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                            .then(actionBackground)
                    )
                    Button(
                        symbol = '7',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = '8',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = '9',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        symbol = AmountInputState.Operator.Multiply.symbol,
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                            .then(actionBackground)
                    )
                    Button(
                        symbol = '4',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = '5',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = '6',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        symbol = AmountInputState.Operator.Minus.symbol,
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                            .then(actionBackground)
                    )
                    Button(
                        symbol = '1',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = '2',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = '3',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        symbol = AmountInputState.Operator.Plus.symbol,
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                            .then(actionBackground)
                    )
                    Button(
                        symbol = '0',
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth * 2 + buttonGap,
                                height = buttonHeight,
                            )
                    )
                    Button(
                        symbol = inputState.decimalSeparator,
                        onClicked = onButtonClicked,
                        modifier = Modifier
                            .size(
                                width = buttonWidth,
                                height = buttonHeight,
                            )
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Button(
                    symbol = '⌫',
                    onClicked = onButtonClicked,
                    onLongClicked = {
                        animateClear()
                    },
                    modifier = Modifier
                        .size(
                            width = buttonWidth,
                            height = buttonHeight,
                        )
                        .then(actionBackground)
                )
                Button(
                    symbol =
                        if (inputState.isEvaluationNeeded)
                            '='
                        else
                            when (mainAction) {

                                AmountKeyboardMainAction.Done ->
                                    '✓'

                                AmountKeyboardMainAction.Next ->
                                    '❭'
                            },
                    onClicked = {
                        if (inputState.isEvaluationNeeded) {
                            onButtonClicked('=')
                        } else {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.Confirm
                            )
                            onMainActionClicked?.invoke(mainAction)
                        }
                    },
                    textColor = Color(colorScheme.onPrimary),
                    modifier = Modifier
                        .size(
                            width = buttonWidth,
                            height = buttonHeight * 3 + buttonGap * 2,
                        )
                        .background(
                            color = Color(colorScheme.primary),
                        )
                )
            }
        }
    }
}

@Composable
private fun Button(
    modifier: Modifier = Modifier,
    symbol: Char,
    textColor: Color = Color.Unspecified,
    onClicked: (Char) -> Unit,
    onLongClicked: ((Char) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .then(
                if (onLongClicked != null)
                    Modifier.combinedClickable(
                        onClick = { onClicked(symbol) },
                        onLongClick = { onLongClicked(symbol) },
                    )
                else
                    Modifier.clickable(
                        onClick = { onClicked(symbol) },
                    )
            )
            .border(
                width = 1.dp,
                color = Color.DarkGray,
                shape = shape,
            )
    ) {
        Text(
            text = symbol.toString(),
            fontSize = 28.sp,
            color = textColor,
        )
    }
}

enum class AmountKeyboardMainAction {
    Done,
    Next,
    ;
}

@Preview
@Composable
private fun Preview(

) {
    AmountKeyboard(
        inputState = rememberAmountInputState(
            currency = ViewCurrency(
                symbol = "$",
                precision = 2,
            ),
            initialValue = BigInteger.ZERO,
        ),
        colorScheme = HardcodedItemColorSchemeRepository()
            .getItemColorSchemes()[22],
        modifier = Modifier
            .size(
                width = 250.dp,
                height = 200.dp,
            )
    )
}
