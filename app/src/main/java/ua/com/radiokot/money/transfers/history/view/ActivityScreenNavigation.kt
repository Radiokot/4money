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

package ua.com.radiokot.money.transfers.history.view

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.home.view.HomeViewModel
import ua.com.radiokot.money.transfers.data.Transfer

@Serializable
object ActivityScreenRoute

fun NavGraphBuilder.activityScreen(
    homeViewModel: HomeViewModel,
    onProceedToEditingTransfer: (transferToEdit: Transfer) -> Unit,
) = composable<ActivityScreenRoute> {

    val activity: Activity? = LocalActivity.current
    val viewModel: ActivityViewModel = koinViewModel {
        parametersOf(
            homeViewModel,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActivityViewModel.Event.ProceedToEditingTransfer -> {
                    onProceedToEditingTransfer(event.transferToEdit)
                }

                is ActivityViewModel.Event.ProceedToRevertingTransferConfirmation -> {
                    checkNotNull(activity) {
                        "The screen must have an activity to proceed"
                    }

                    AlertDialog.Builder(activity)
                        .setTitle("Revert a transfer")
                        .setMessage("Are you sure you want to revert this transfer?")
                        .setPositiveButton("Yes") { _, _ ->
                            viewModel.onTransferRevertConfirmed(
                                transferToRevertId = event.transferToRevertId,
                            )
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
        }
    }

    ActivityScreenRoot(
        viewModel = viewModel,
        modifier = Modifier
            .fillMaxSize()
    )
}
