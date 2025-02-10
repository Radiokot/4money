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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.uikit.AccountList
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider

class AccountsActivity : UserSessionScopeActivity() {

    private val viewModel: AccountsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        setContent {
            AccountsScreenRoot(
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun AccountsScreenRoot(
    viewModel: AccountsViewModel,
) = AccountsScreen(
    listItemsFlow = viewModel.accountListItems,
    onAccountItemClicked = viewModel::onAccountItemClicked,
)

@Composable
private fun AccountsScreen(
    listItemsFlow: StateFlow<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
) = Box(
    modifier = Modifier
        .safeDrawingPadding()
        .padding(16.dp)
) {
    AccountList(
        itemListFlow = listItemsFlow,
        onAccountItemClicked = onAccountItemClicked,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
@Preview
private fun AccountsScreenPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class, limit = 1)
    amount: ViewAmount,
) {
    AccountsScreen(
        listItemsFlow = MutableStateFlow(
            listOf(
                ViewAccountListItem.Account(
                    title = "Account #1",
                    balance = amount,
                    source = "1",
                )
            )
        ),
        onAccountItemClicked = {},
    )
}

@Composable
private fun AccountActionsSheet(
    accountDetails: ViewAccountDetails,
    onBalanceClicked: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .safeDrawingPadding()
        .fillMaxWidth()
        .border(
            width = 1.dp,
            color = Color.LightGray,
        )
        .padding(16.dp)
) {
    BasicText(
        text = accountDetails.title,
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    BasicText(
        text = accountDetails.balance.format(
            locale = LocalConfiguration.current.locales[0],
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBalanceClicked() }
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "Balance",
            modifier = Modifier
                .clickable { onBalanceClicked() }
                .border(
                    width = 1.dp,
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        )
    }
}

@Composable
@Preview
private fun AccountActionSheetPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class, limit = 1)
    amount: ViewAmount,
) = AccountActionsSheet(
    accountDetails = ViewAccountDetails(
        title = "Account #1",
        balance = amount,
    ),
    onBalanceClicked = {},
)
