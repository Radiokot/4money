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

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.bottomSheet
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId

@Serializable
data class AccountActionSheetRoute(
    val accountId: String,
)

fun NavGraphBuilder.accountActionSheet(
    onDone: () -> Unit,
    onProceedToIncome: (destinationAccountId: TransferCounterpartyId.Account) -> Unit = {},
    onProceedToExpense: (sourceAccountId: TransferCounterpartyId.Account) -> Unit = {},
    onProceedToTransfer: (sourceAccountId: TransferCounterpartyId.Account) -> Unit = {},
    onProceedToFilteredActivity: (accountCounterparty: TransferCounterparty.Account) -> Unit = {},
    onProceedToEdit: (accountId: String) -> Unit,
) = bottomSheet<AccountActionSheetRoute> { entry ->

    val route: AccountActionSheetRoute = entry.toRoute()
    val viewModel: AccountActionSheetViewModel = koinViewModel {
        parametersOf(
            AccountActionSheetViewModel.Parameters(
                accountId = route.accountId,
            )
        )
    }

    LaunchedEffect(route) {
        viewModel.events.collect { event ->
            when (event) {

                is AccountActionSheetViewModel.Event.ProceedToIncome ->
                    onProceedToIncome(event.destinationAccountId)

                is AccountActionSheetViewModel.Event.ProceedToExpense ->
                    onProceedToExpense(event.sourceAccountId)

                is AccountActionSheetViewModel.Event.ProceedToTransfer ->
                    onProceedToTransfer(event.sourceAccountId)

                is AccountActionSheetViewModel.Event.ProceedToEdit ->
                    onProceedToEdit(event.accountId)

                is AccountActionSheetViewModel.Event.ProceedToFilteredActivity ->
                    onProceedToFilteredActivity(event.accountCounterparty)

                AccountActionSheetViewModel.Event.Done ->
                    onDone()
            }
        }
    }

    AccountActionSheet(
        viewModel = viewModel,
    )
}
