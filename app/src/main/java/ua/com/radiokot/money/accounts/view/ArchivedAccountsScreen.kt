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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.uikit.TextButton
import java.math.BigInteger

@Composable
private fun ArchivedAccountsScreen(
    accountItemList: State<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onCloseClicked: () -> Unit,
) = Column(
    modifier = Modifier
        .windowInsetsPadding(
            WindowInsets.navigationBars
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
            text = "Archived accounts",
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )
    }

    AccountList(
        itemList = accountItemList,
        onAccountItemClicked = onAccountItemClicked,
    )
}

@Composable
fun ArchivedAccountsScreen(
    viewModel: ArchivedAccountsScreenViewModel,
) {
    ArchivedAccountsScreen(
        accountItemList = viewModel.accountItemList.collectAsState(),
        onAccountItemClicked = remember { viewModel::onAccountItemClicked },
        onCloseClicked = remember { viewModel::onCloseClicked },
    )
}

@Composable
@Preview(
    apiLevel = 34,
)
private fun Preview(

) {
    val colorSchemesByName = HardcodedItemColorSchemeRepository()
        .getItemColorSchemesByName()

    ArchivedAccountsScreen(
        accountItemList = listOf(
            ViewAccountListItem.Account(
                title = "Account #1",
                balance = ViewAmount(
                    value = BigInteger("7500"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    ),
                ),
                isIncognito = false,
                colorScheme = colorSchemesByName.getValue("Purple1"),
                key = "acc1",
            ),
        ).let(::mutableStateOf),
        onAccountItemClicked = {},
        onCloseClicked = {},
    )
}
