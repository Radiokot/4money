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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.money.auth.view.UserSessionScopeFragment
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.transfers.view.TransferSheetRoot
import ua.com.radiokot.money.transfers.view.TransferSheetViewModel
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider

class AccountsScreenFragment : UserSessionScopeFragment() {

    private val viewModel: AccountsViewModel by viewModel()
    private val actionSheetViewModel: AccountActionSheetViewModel by viewModel()
    private val transferSheetViewModel: TransferSheetViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AccountsScreenRoot(
                viewModel = viewModel,
                actionSheetViewModel = actionSheetViewModel,
                transferSheetViewModel = transferSheetViewModel,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            subscribeToEvents()
        }
        lifecycleScope.launch {
            subscribeToActionSheetEvents()
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

    private suspend fun subscribeToActionSheetEvents(
    ): Unit = actionSheetViewModel.events.collect { event ->
        when (event) {
            is AccountActionSheetViewModel.Event.OpenTransfer -> {
                transferSheetViewModel.open(
                    source = event.source,
                    destination = event.destination,
                )
            }
        }
    }
}

@Composable
private fun AccountsScreenRoot(
    viewModel: AccountsViewModel,
    actionSheetViewModel: AccountActionSheetViewModel,
    transferSheetViewModel: TransferSheetViewModel,
) = Box(
    modifier = Modifier
        .fillMaxSize()
) {
    AccountsScreen(
        accountItemList = viewModel.accountListItems.collectAsState(),
        onAccountItemClicked = viewModel::onAccountItemClicked,
    )

    AccountActionSheetRoot(
        viewModel = actionSheetViewModel,
        modifier = Modifier
            .align(Alignment.BottomCenter),
    )

    TransferSheetRoot(
        viewModel = transferSheetViewModel,
        modifier = Modifier
            .align(Alignment.BottomCenter),
    )
}

@Composable
private fun AccountsScreen(
    accountItemList: State<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
) = Box(
    modifier = Modifier
        .padding(16.dp)
) {
    AccountList(
        itemList = accountItemList,
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
