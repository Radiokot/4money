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

package ua.com.radiokot.money.auth.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun PhraseAuthScreen(
    modifier: Modifier = Modifier,
    phrase: State<String>,
    onPhraseChanged: (String) -> Unit,
    isSignInEnabled: State<Boolean>,
    onSignInClicked: () -> Unit,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .only(WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
        )
        .scrollable(
            state = rememberScrollState(),
            orientation = Orientation.Vertical,
        )
        .padding(
            vertical = 24.dp,
            horizontal = 16.dp,
        )
) {

    Text(
        text = "Recovery phrase",
    )

    Spacer(modifier = Modifier.height(6.dp))

    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = phrase.value,
        onValueChange = onPhraseChanged,
        singleLine = false,
        minLines = 3,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                softwareKeyboardController?.hide()
                if (isSignInEnabled.value) {
                    onSignInClicked()
                }
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.DarkGray,
            )
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    TextButton(
        text = "Sign in",
        isEnabled = isSignInEnabled.value,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isSignInEnabled.value,
                onClick = onSignInClicked,
            )
    )
}

@Composable
fun PhraseAuthScreenRoot(
    modifier: Modifier,
    viewModel: PhraseAuthViewModel,
) {
    PhraseAuthScreen(
        phrase = viewModel.phrase.collectAsState(),
        onPhraseChanged = remember { viewModel::onPhraseChanged },
        isSignInEnabled = viewModel.isSignInEnabled.collectAsState(),
        onSignInClicked = remember { viewModel::onSignInClicked },
        modifier = modifier,
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {
    PhraseAuthScreen(
        phrase = "tonight fat have keen intact happy social powder tired shaft length cram".let(::mutableStateOf),
        onPhraseChanged = {},
        isSignInEnabled = true.let(::mutableStateOf),
        onSignInClicked = {},
    )
}
