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

package ua.com.radiokot.money.accounts.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.view.ColorSchemePicker
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun AccountLogoScreen(
    modifier: Modifier = Modifier,
    accountTitle: String,
    colorSchemeList: State<List<ItemColorScheme>>,
    selectedColorScheme: State<ItemColorScheme>,
    onColorSchemeClicked: (ItemColorScheme) -> Unit,
    onCloseClicked: () -> Unit,
    onSaveClicked: () -> Unit,
) = Column(
    modifier = modifier
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .only(WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
        )
        .padding(
            horizontal = 16.dp,
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

        Text(
            text = "Account logo",
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )

        TextButton(
            text = "✔️",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onSaveClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    AccountLogo(
        accountTitle = accountTitle, colorScheme = selectedColorScheme.value,
        modifier = Modifier
            .size(72.dp)
            .align(Alignment.CenterHorizontally)
    )

    Spacer(modifier = Modifier.height(8.dp))

    ColorSchemePicker(
        colorSchemeList = colorSchemeList,
        selectedColorScheme = selectedColorScheme,
        onColorSchemeClicked = onColorSchemeClicked,
        contentPadding = PaddingValues(
            horizontal = 0.dp,
            vertical = 16.dp,
        )
    )
}

@Composable
fun AccountLogoScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: AccountLogoScreenViewModel,
) {
    AccountLogoScreen(
        accountTitle = viewModel.accountTitle,
        colorSchemeList = viewModel.colorSchemeList.collectAsState(),
        selectedColorScheme = viewModel.selectedColorScheme.collectAsState(),
        onColorSchemeClicked = remember { viewModel::onColorSchemeClicked },
        onCloseClicked = remember { viewModel::onCloseClicked },
        onSaveClicked = remember { viewModel::onSaveClicked },
        modifier = modifier,
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {
    AccountLogoScreen(
        accountTitle = "🤗",
        colorSchemeList = HardcodedItemColorSchemeRepository()
            .getItemColorSchemes()
            .let(::mutableStateOf),
        selectedColorScheme = HardcodedItemColorSchemeRepository()
            .getItemColorSchemesByName()
            .getValue("Turquoise3")
            .let(::mutableStateOf),
        onColorSchemeClicked = {},
        onCloseClicked = { },
        onSaveClicked = {},
    )
}
