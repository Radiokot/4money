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

package ua.com.radiokot.money.transfers.view

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId

@Serializable
data class TransferCounterpartySelectionSheetRoute(
    val isIncognito: Boolean,
    val isForSource: Boolean,
    val alreadySelectedCounterpartyId: TransferCounterpartyId?,
)

fun NavGraphBuilder.transferCounterpartySelectionSheet(
    onSelected: (TransferCounterparty) -> Unit,
) = dialog<TransferCounterpartySelectionSheetRoute> { entry ->
    val route = entry.toRoute<TransferCounterpartySelectionSheetRoute>()
    val viewModel = koinViewModel<TransferCounterpartySelectionSheetViewModel>()

    LaunchedEffect(route) {
        viewModel.setParameters(
            isForSource = route.isForSource,
            alreadySelectedCounterpartyId = route.alreadySelectedCounterpartyId,
        )

        launch {
            viewModel.events.collect { event ->
                when (event) {
                    is TransferCounterpartySelectionSheetViewModel.Event.CounterpartySelected -> {
                        onSelected(event.counterparty)
                    }
                }
            }
        }
    }

    TransferCounterpartySelectionSheetRoot(
        viewModel = viewModel,
    )
}
