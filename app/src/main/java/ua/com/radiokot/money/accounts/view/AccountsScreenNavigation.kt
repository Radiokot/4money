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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.accounts.data.Account

const val AccountsScreenRoute = "accounts"

fun NavGraphBuilder.accountsScreen(
    onProceedToAccountActions: (account: Account) -> Unit,
    onProceedToAccountAdd: () -> Unit,
    onProceedToArchivedAccounts: () -> Unit,
) = composable(AccountsScreenRoute) { entry ->

    val viewModel = koinViewModel<AccountsViewModel>()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountsViewModel.Event.ProceedToAccountActions ->
                    onProceedToAccountActions(event.account)

                is AccountsViewModel.Event.ProceedToAccountAdd ->
                    onProceedToAccountAdd()

                is AccountsViewModel.Event.ProceedToArchivedAccounts ->
                    onProceedToArchivedAccounts()
            }
        }
    }

    AccountsScreenRoot(
        viewModel = viewModel,
        modifier = Modifier
            .fillMaxSize()
    )
}
