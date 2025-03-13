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

import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId

@Serializable
class TransferCounterpartySelectionSheetRoute(
    val isIncognito: Boolean,
    val isForSource: Boolean?,
    val showAccounts: Boolean,
    val showCategories: Boolean,
    private val alreadySelectedCounterpartyIdJson: String,
) {

    constructor(
        isForSource: Boolean?,
        alreadySelectedCounterpartyId: TransferCounterpartyId?,
        isIncognito: Boolean = false,
        showAccounts: Boolean = true,
        showCategories: Boolean = true,
    ) : this(
        isIncognito = isIncognito,
        isForSource = isForSource,
        showAccounts = showAccounts,
        showCategories = showCategories,
        alreadySelectedCounterpartyIdJson = Json.encodeToString(alreadySelectedCounterpartyId),
    )

    val alreadySelectedCounterpartyId: TransferCounterpartyId?
        get() = Json.decodeFromString(alreadySelectedCounterpartyIdJson)
}

data class TransferCounterpartySelectionResult(
    val selectedCounterparty: TransferCounterparty,
    val isSelectedAsSource: Boolean,
    val otherSelectedCounterpartyId: TransferCounterpartyId?,
)

fun NavGraphBuilder.transferCounterpartySelectionSheet(
    onSelected: (TransferCounterpartySelectionResult) -> Unit,
) = bottomSheet<TransferCounterpartySelectionSheetRoute> { entry ->
    val route = entry.toRoute<TransferCounterpartySelectionSheetRoute>()
    val viewModel = koinViewModel<TransferCounterpartySelectionSheetViewModel>()

    LaunchedEffect(route) {
        viewModel.setParameters(
            isIncognito = route.isIncognito,
            isForSource = route.isForSource,
            showAccounts = route.showAccounts,
            showCategories = route.showCategories,
            alreadySelectedCounterpartyId = route.alreadySelectedCounterpartyId,
        )

        launch {
            viewModel.events.collect { event ->
                when (event) {
                    is TransferCounterpartySelectionSheetViewModel.Event.Selected -> {
                        onSelected(event.result)
                    }
                }
            }
        }
    }

    TransferCounterpartySelectionSheetRoot(
        viewModel = viewModel,
    )
}
