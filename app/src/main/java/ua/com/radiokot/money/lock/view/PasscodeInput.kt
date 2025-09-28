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

package ua.com.radiokot.money.lock.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text

@Composable
fun PasscodeInput(
    modifier: Modifier,
    passcode: State<String>,
    onPasscodeChanged: (String) -> Unit,
    onBackspaceClicked: () -> Unit,
    length: Int,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier,
) {
    InputIndicator(
        passcode = passcode,
        length = length,
    )

    Spacer(modifier = Modifier.height(24.dp))

    val onNumberClicked = remember(passcode, onPasscodeChanged) {
        { number: Int ->
            val newPasscode = passcode.value + number
            if (newPasscode.length <= length) {
                onPasscodeChanged(newPasscode)
            }
        }
    }

    Keyboard(
        onNumberClicked = onNumberClicked,
        onBackspaceClicked = onBackspaceClicked,
        modifier = Modifier
            .weight(1f)
    )
}

@Composable
private fun InputIndicator(
    modifier: Modifier = Modifier,
    passcode: State<String>,
    length: Int,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(
        space = 8.dp,
        alignment = Alignment.CenterHorizontally,
    ),
    modifier = modifier,
) {
    (1..length).forEach { dotLength ->
        Box(
            modifier = Modifier
                .size(16.dp)
                .then(
                    if (passcode.value.length >= dotLength)
                        Modifier.background(
                            color = Color.DarkGray,
                            shape = CircleShape,
                        )
                    else
                        Modifier.border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = CircleShape,
                        )
                )
        )
    }
}

@Composable
private fun Keyboard(
    modifier: Modifier = Modifier,
    onNumberClicked: (Int) -> Unit,
    onBackspaceClicked: () -> Unit,
) = BoxWithConstraints(
    modifier = modifier
) {
    val buttonGap = 16.dp
    val buttonSize = min(
        (maxWidth - buttonGap * 2) / 3,
        (maxHeight - buttonGap * 3) / 4
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(buttonGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonGap),
        ) {
            (1..3).forEach { number ->
                NumberButton(
                    number = number,
                    onClick = onNumberClicked,
                    modifier = Modifier
                        .size(buttonSize)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonGap),
        ) {
            (4..6).forEach { number ->
                NumberButton(
                    number = number,
                    onClick = onNumberClicked,
                    modifier = Modifier
                        .size(buttonSize)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonGap),
        ) {
            (7..9).forEach { number ->
                NumberButton(
                    number = number,
                    onClick = onNumberClicked,
                    modifier = Modifier
                        .size(buttonSize)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonGap),
        ) {
            ActionButton(
                text = "🤷",
                onClick = {},
                modifier = Modifier
                    .size(buttonSize)
            )
            NumberButton(
                number = 0,
                onClick = onNumberClicked,
                modifier = Modifier
                    .size(buttonSize)
            )
            ActionButton(
                text = "⌫",
                onClick = onBackspaceClicked,
                modifier = Modifier
                    .size(buttonSize)
            )
        }
    }
}

@Composable
private fun NumberButton(
    modifier: Modifier,
    number: Int,
    onClick: (Int) -> Unit,
) {
    val shape = CircleShape

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .clickable(
                onClick = {
                    onClick(number)
                },
            )
            .border(
                width = 1.dp,
                color = Color.DarkGray,
                shape = shape,
            )
    ) {
        Text(
            text = number.toString(),
            fontSize = 28.sp,
        )
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit,
) {
    val shape = CircleShape

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .clickable(
                onClick = onClick,
            )
            .background(
                color = Color(0xfff3f0f6)
            )
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
        )
    }
}

@Composable
@Preview
private fun Preview() {
    val passcode = remember {
        mutableStateOf("")
    }
    val length = 4

    PasscodeInput(
        passcode = passcode,
        onPasscodeChanged = passcode::value::set,
        onBackspaceClicked = {
            passcode.value = passcode.value.dropLast(1)
        },
        length = length,
        modifier = Modifier
            .size(
                width = 250.dp,
                height = 300.dp,
            )
    )
}
