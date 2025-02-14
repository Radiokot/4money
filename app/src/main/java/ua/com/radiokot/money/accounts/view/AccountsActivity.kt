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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider

class AccountsActivity : UserSessionScopeActivity() {

    private val viewModel: AccountsViewModel by viewModel()
    private val actionSheetViewModel: AccountActionSheetViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        setContent {
            AccountsScreenRoot(
                viewModel = viewModel,
                actionSheetViewModel = actionSheetViewModel,
            )
        }

        lifecycleScope.launch {
            subscribeToEvents()
        }
    }

    private suspend fun subscribeToEvents(): Unit = viewModel.events.collect { event ->
        when (event) {
            is AccountsViewModel.Event.OpenAccountActions ->
                actionSheetViewModel.open(
                    account = event.account,
                )
        }
    }
}

@Composable
private fun AccountsScreenRoot(
    viewModel: AccountsViewModel,
    actionSheetViewModel: AccountActionSheetViewModel,
) = Box(
    modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
) {
    AccountsScreen(
        listItemsFlow = viewModel.accountListItems,
        onAccountItemClicked = viewModel::onAccountItemClicked,
    )

    val isSheetOpened by actionSheetViewModel.isOpened.collectAsState()
    AnimatedVisibility(
        visible = isSheetOpened,
        enter = slideInVertically(
            initialOffsetY = Int::unaryPlus,
        ),
        exit = slideOutVertically(
            targetOffsetY = Int::unaryPlus,
        ),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .widthIn(
                max = 400.dp,
            )
    ) {
        val accountDetailsState = actionSheetViewModel.accountDetails.collectAsState()
        val modeState = actionSheetViewModel.mode.collectAsState()

        AccountActionSheet(
            accountDetails = accountDetailsState.value
                ?: return@AnimatedVisibility,
            mode = modeState.value,
            balanceInputValueFlow = actionSheetViewModel.balanceInputValue,
            onBalanceClicked = actionSheetViewModel::onBalanceClicked,
            onBackPressed = actionSheetViewModel::onBackPressed,
            onBalanceInputValueUpdated = actionSheetViewModel::onBalanceInputValueUpdated,
            onBalanceInputSubmit = actionSheetViewModel::onBalanceInputSubmit,
        )
    }
}

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
                    key = "1",
                )
            )
        ),
        onAccountItemClicked = {},
    )
}
