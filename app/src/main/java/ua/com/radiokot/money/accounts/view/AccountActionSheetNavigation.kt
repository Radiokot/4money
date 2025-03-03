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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.transfers.data.TransferCounterparty

@Serializable
data class AccountActionSheetRoute(
    val accountId: String,
)

fun NavGraphBuilder.accountActionSheet(
    close: () -> Unit,
    onTransferCounterpartiesSelected: (
        source: TransferCounterparty,
        destination: TransferCounterparty,
    ) -> Unit,
) = dialog<AccountActionSheetRoute> { entry ->

    val accountId = entry.toRoute<AccountActionSheetRoute>()
        .accountId
    val viewModel = koinViewModel<AccountActionSheetViewModel>()

    LaunchedEffect(accountId) {
        viewModel.setAccount(
            accountId = accountId,
        )

        launch {
            viewModel.events.collect { event ->
                when (event) {
                    AccountActionSheetViewModel.Event.Close -> {
                        close()
                    }

                    is AccountActionSheetViewModel.Event.TransferCounterpartiesSelected -> {
                        onTransferCounterpartiesSelected(
                            event.source,
                            event.destination,
                        )
                    }
                }
            }
        }
    }

    AccountActionSheetRoot(
        viewModel = viewModel,
        modifier = Modifier
            .fillMaxWidth()
    )
}
