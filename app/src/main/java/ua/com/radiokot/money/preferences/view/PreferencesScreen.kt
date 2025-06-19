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

package ua.com.radiokot.money.preferences.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun PreferencesScreen(
    modifier: Modifier = Modifier,
    primaryCurrencyCode: State<String>,
    onPrimaryCurrencyCodeChanged: (String) -> Unit,
    isSaveCurrencyPreferencesEnabled: State<Boolean>,
    onSaveCurrencyPreferencesClicked: () -> Unit,
    userId: State<String>,
    onSignOutClicked: () -> Unit,
) = Column(
    modifier = modifier
        .verticalScroll(
            state = rememberScrollState(),
        )
) {
    BasicText(
        text = "Currency",
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight(500)
        ),
    )

    Spacer(modifier = Modifier.height(18.dp))

    BasicText(
        text = "Primary currency",
    )

    Spacer(modifier = Modifier.height(6.dp))

    BasicTextField(
        value = primaryCurrencyCode.value,
        onValueChange = onPrimaryCurrencyCodeChanged,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.DarkGray,
            )
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(18.dp))

    TextButton(
        text = "Save",
        isEnabled = isSaveCurrencyPreferencesEnabled.value,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSaveCurrencyPreferencesClicked,
                enabled = isSaveCurrencyPreferencesEnabled.value,
            )
    )

    Spacer(modifier = Modifier.height(40.dp))

    BasicText(
        text = "User",
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight(500)
        ),
    )

    Spacer(modifier = Modifier.height(18.dp))

    BasicText(
        text = "Identifier",
    )

    Spacer(modifier = Modifier.height(6.dp))

    BasicText(
        text = userId.value,
    )

    Spacer(modifier = Modifier.height(18.dp))

    TextButton(
        text = "Sign out",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSignOutClicked,
            )
    )
}

@Composable
fun PreferencesScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: PreferencesScreenViewModel,
) = PreferencesScreen(
    modifier = modifier,
    primaryCurrencyCode = viewModel.primaryCurrencyCodeValue.collectAsState(),
    onPrimaryCurrencyCodeChanged = remember { viewModel::onPrimaryCurrencyCodeChanged },
    isSaveCurrencyPreferencesEnabled = viewModel.isSaveCurrencyPreferencesEnabled.collectAsState(),
    onSaveCurrencyPreferencesClicked = remember { viewModel::onSaveCurrencyPreferencesClicked },
    onSignOutClicked = remember { viewModel::onSignOutClicked },
    userId = viewModel.userId.collectAsState(),
)

@Preview(
    apiLevel = 34,
)
@Composable
private fun PreferencesScreenPreview(
) = PreferencesScreen(
    primaryCurrencyCode = "USD".let(::mutableStateOf),
    onPrimaryCurrencyCodeChanged = {},
    isSaveCurrencyPreferencesEnabled = true.let(::mutableStateOf),
    onSaveCurrencyPreferencesClicked = {},
    userId = "uid".let(::mutableStateOf),
    onSignOutClicked = {},
)
