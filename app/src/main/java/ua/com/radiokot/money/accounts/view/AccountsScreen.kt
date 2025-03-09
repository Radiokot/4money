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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider

@Composable
fun AccountsScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel,
) = AccountsScreen(
    accountItemList = viewModel.accountListItems.collectAsState(),
    onAccountItemClicked = viewModel::onAccountItemClicked,
    modifier = modifier,
)

@Composable
private fun AccountsScreen(
    modifier: Modifier = Modifier,
    accountItemList: State<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
) = AccountList(
    modifier = modifier
        .padding(
            horizontal = 16.dp,
        ),
    itemList = accountItemList,
    onAccountItemClicked = onAccountItemClicked,
)

@Composable
@Preview
private fun AccountsScreenPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class, limit = 1)
    amount: ViewAmount,
) {
    AccountsScreen(
        accountItemList = listOf(
            ViewAccountListItem.Account(
                title = "Account #1",
                balance = amount,
                key = "1",
            )
        ).let(::mutableStateOf),
        onAccountItemClicked = {},
    )
}
