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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.R
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun SetUpPasscodeScreen(
    passcodeLength: Int,
    passcode: State<String>,
    onPasscodeChanged: (String) -> Unit,
    isRepeating: State<Boolean>,
    onCloseClicked: () -> Unit,
) = BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .only(WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
        )
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 56.dp,
            )
            .padding(
                horizontal = 16.dp,
            )
            .align(Alignment.TopStart)
    ) {
        val buttonPadding = remember {
            PaddingValues(6.dp)
        }

        TextButton(
            text = "❌",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onCloseClicked,
                )
        )
    }

    val inputWidth = min(maxWidth * 0.8f, 400.dp)
    val inputHeight = maxHeight * 0.6f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (this@BoxWithConstraints.maxHeight > 500.dp) {
            Image(
                painter = painterResource(R.drawable.pear_by_francesco_cesqo_stefanini_from_noun_project_cc_by_3_0),
                contentScale = ContentScale.FillHeight,
                contentDescription = null,
                modifier = Modifier
                    .size(108.dp)
            )
        }

        Text(
            text =
                if (isRepeating.value)
                    "Repeat the passcode"
                else
                    "Enter a passcode",
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(32.dp))

        PasscodeInput(
            length = passcodeLength,
            passcode = passcode,
            onPasscodeChanged = onPasscodeChanged,
            onBackspaceClicked = {
                onPasscodeChanged(passcode.value.dropLast(1))
            },
            isBiometricsButtonShown = false,
            onBiometricsClicked = { },
            modifier = Modifier
                .size(
                    width = inputWidth,
                    height = inputHeight,
                )
        )
    }
}

@Composable
fun SetUpPasscodeScreen(
    viewModel: SetUpPasscodeScreenViewModel,
) {
    SetUpPasscodeScreen(
        passcodeLength = viewModel.passcodeLength,
        passcode = viewModel.passcode.collectAsState(),
        onPasscodeChanged = remember { viewModel::onPasscodeChanged },
        isRepeating = viewModel.isRepeating.collectAsState(),
        onCloseClicked = remember { viewModel::onCloseClicked },
    )
}

@Preview
@Composable
private fun Preview() {
    SetUpPasscodeScreen(
        passcodeLength = 4,
        passcode = "12".let(::mutableStateOf),
        onPasscodeChanged = {},
        isRepeating = true.let(::mutableStateOf),
        onCloseClicked = {},
    )
}
