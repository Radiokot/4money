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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.stableClickable
import ua.com.radiokot.money.uikit.TextButton

class EditAccountActivity : UserSessionScopeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            UserSessionScope {

                EditAccountScreenRoot(
                    viewModel = koinViewModel(),
                )
            }
        }
    }
}

@Composable
private fun EditAccountScreenRoot(
    viewModel: EditAccountViewModel,
) {
    EditAccountScreen(
        isNewAccount = viewModel.isNewAccount.collectAsState(),
        isSaveEnabled = viewModel.isSaveEnabled.collectAsState(),
        onSaveClicked = viewModel::onSaveClicked,
        title = viewModel.title.collectAsState(),
        onTitleChanged = viewModel::onTitleChanged,
        colorScheme = viewModel.colorScheme.collectAsState(),
        onColorClicked = viewModel::onColorClicked,
        currencyCode = viewModel.currencyCode.collectAsState(),
        isCurrencyChangeEnabled = viewModel.isCurrencyChangeEnabled.collectAsState(),
        onCurrencyClicked = viewModel::onCurrencyClicked,
        type = viewModel.type.collectAsState(),
        onTypeClicked = viewModel::onTypeClicked,
    )
}

@Composable
private fun EditAccountScreen(
    isNewAccount: State<Boolean>,
    isSaveEnabled: State<Boolean>,
    onSaveClicked: () -> Unit,
    title: State<String>,
    onTitleChanged: (String) -> Unit,
    colorScheme: State<ItemColorScheme>,
    onColorClicked: () -> Unit,
    currencyCode: State<String>,
    isCurrencyChangeEnabled: State<Boolean>,
    onCurrencyClicked: () -> Unit,
    type: State<Account.Type>,
    onTypeClicked: () -> Unit,
) = Column(
    modifier = Modifier
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
        )

        Text(
            text =
            if (isNewAccount.value)
                "New account"
            else
                "Edit account",
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )

        TextButton(
            text = "✔️",
            isEnabled = isSaveEnabled.value,
            padding = buttonPadding,
            modifier = Modifier
                .stableClickable(
                    onClick = onSaveClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    AccountLogoAndTitleRow(
        title = title,
        onTitleChanged = onTitleChanged,
        colorScheme = colorScheme,
        onLogoClicked = onColorClicked,
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Currency",
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color =
                if (isCurrencyChangeEnabled.value)
                    Color.DarkGray
                else
                    Color.Gray,
            )
            .stableClickable(
                onClick = onCurrencyClicked,
            )
            .padding(12.dp)
    ) {
        Text(
            text = currencyCode.value,
            color =
            if (isCurrencyChangeEnabled.value)
                Color.Unspecified
            else
                Color.Gray,
            modifier = Modifier
                .weight(1f)
        )

        if (isCurrencyChangeEnabled.value) {
            Text(text = "🔽")
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Type",
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.DarkGray,
            )
            .stableClickable(
                onClick = onTypeClicked,
            )
            .padding(12.dp)
    ) {
        Text(
            text = type.value.name,
            modifier = Modifier
                .weight(1f)
        )

        Text(text = "🔽")
    }

    Spacer(modifier = Modifier.height(24.dp))

    TextButton(
        text = "Save",
        isEnabled = isSaveEnabled.value,
        modifier = Modifier
            .fillMaxWidth(1f)
            .stableClickable(
                onClick = onSaveClicked,
            )
    )
}

@Composable
private fun AccountLogoAndTitleRow(
    modifier: Modifier = Modifier,
    title: State<String>,
    onTitleChanged: (String) -> Unit,
    colorScheme: State<ItemColorScheme>,
    onLogoClicked: () -> Unit,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.Bottom,
) {
    Column(
        modifier = Modifier
            .weight(1f)
    ) {
        Text(
            text = "Title",
        )

        Spacer(modifier = Modifier.height(6.dp))

        BasicTextField(
            value = title.value,
            onValueChange = onTitleChanged,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
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
    }

    AccountLogo(
        accountTitle = title.value,
        colorScheme = colorScheme.value,
        modifier = Modifier
            .padding(
                start = 16.dp,
            )
            .size(42.dp)
            .stableClickable(
                onClick = onLogoClicked,
            )
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun EditAccountScreenPreview(

) {
    EditAccountScreen(
        isNewAccount = true.let(::mutableStateOf),
        isSaveEnabled = false.let(::mutableStateOf),
        onSaveClicked = {},
        title = "Vault".let(::mutableStateOf),
        onTitleChanged = {},
        colorScheme = HardcodedItemColorSchemeRepository()
            .getItemColorSchemesByName()
            .getValue("Purple2")
            .let(::mutableStateOf),
        onColorClicked = {},
        currencyCode = "PLN".let(::mutableStateOf),
        isCurrencyChangeEnabled = true.let(::mutableStateOf),
        onCurrencyClicked = {},
        type = Account.Type.Savings.let(::mutableStateOf),
        onTypeClicked = {},
    )
}
