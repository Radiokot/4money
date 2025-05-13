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

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.showSingle
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import java.math.BigInteger

@Serializable
data class TransferSheetRoute(
    private val sourceIdJson: String,
    private val destinationIdJson: String,
    val transferToEditId: String?,
    private val sourceAmountString: String?,
    private val destinationAmountString: String?,
    val memo: String?,
    private val timeEpochSeconds: Long?,
) {
    val sourceId: TransferCounterpartyId
        get() = Json.decodeFromString(sourceIdJson)

    val destinationId: TransferCounterpartyId
        get() = Json.decodeFromString(destinationIdJson)

    val sourceAmount: BigInteger?
        get() = sourceAmountString?.toBigInteger()

    val destinationAmount: BigInteger?
        get() = destinationAmountString?.toBigInteger()

    val time: Instant?
        get() = timeEpochSeconds?.let(Instant::fromEpochSeconds)

    constructor(
        sourceId: TransferCounterpartyId,
        destinationId: TransferCounterpartyId,
    ) : this(
        sourceIdJson = Json.encodeToString(sourceId),
        destinationIdJson = Json.encodeToString(destinationId),
        transferToEditId = null,
        sourceAmountString = null,
        destinationAmountString = null,
        memo = null,
        timeEpochSeconds = null,
    )

    constructor(
        transferToEdit: Transfer,
    ) : this(
        sourceIdJson = Json.encodeToString(transferToEdit.source.id),
        destinationIdJson = Json.encodeToString(transferToEdit.destination.id),
        transferToEditId = transferToEdit.id,
        sourceAmountString = transferToEdit.sourceAmount.toString(),
        destinationAmountString = transferToEdit.destinationAmount.toString(),
        memo = transferToEdit.memo,
        timeEpochSeconds = transferToEdit.time.epochSeconds,
    )
}

fun NavGraphBuilder.transferFlowSheet(
    isIncognito: Boolean,
    onTransferDone: () -> Unit,
) = bottomSheet<TransferSheetRoute> { flowStartEntry ->
    val flowNavController = rememberNavController()

    NavHost(
        navController = flowNavController,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        startDestination = flowStartEntry.toRoute<TransferSheetRoute>(),
    ) {

        composable<TransferSheetRoute> { entry ->
            val route = entry.toRoute<TransferSheetRoute>()
            val viewModel = koinViewModel<TransferSheetViewModel>()
            val activity = checkNotNull(LocalActivity.current as? FragmentActivity) {
                "This sheet needs activity as a parent"
            }

            DisposableEffect(activity) {
                activity.supportFragmentManager.setFragmentResultListener(
                    DatePickerDialogFragment.DATE_REQUEST_KEY,
                    activity,
                ) { _, bundle ->
                    viewModel.onDatePicked(
                        newDate = DatePickerDialogFragment.getLocalDate(bundle),
                    )
                }

                onDispose {
                    activity.supportFragmentManager.clearFragmentResultListener(
                        DatePickerDialogFragment.DATE_REQUEST_KEY
                    )
                }
            }

            LaunchedEffect(route) {
                val selectedSourceId: TransferCounterpartyId? = TransferCounterpartySelectionResult
                    .getSelectedSourceCounterpartyId(
                        savedStateHandle = entry.savedStateHandle,
                    )
                val selectedDestinationId: TransferCounterpartyId? =
                    TransferCounterpartySelectionResult
                        .getSelectedDestinationCounterpartyId(
                            savedStateHandle = entry.savedStateHandle,
                        )

                if (selectedSourceId != null || selectedDestinationId != null) {
                    viewModel.onCounterpartiesSelected(
                        newSourceId = selectedSourceId,
                        newDestinationId = selectedDestinationId,
                    )
                } else {
                    viewModel.setParameters(
                        sourceId = route.sourceId,
                        destinationId = route.destinationId,
                        transferToEditId = route.transferToEditId,
                        sourceAmount = route.sourceAmount,
                        destinationAmount = route.destinationAmount,
                        memo = route.memo,
                        time = route.time,
                    )
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            TransferSheetViewModel.Event.TransferDone -> {
                                onTransferDone()
                            }

                            is TransferSheetViewModel.Event.ProceedToDatePicker -> {
                                DatePickerDialogFragment
                                    .newInstance(
                                        bundle = DatePickerDialogFragment.getBundle(
                                            currentDate = event.currentDate,
                                        )
                                    )
                                    .showSingle(
                                        activity.supportFragmentManager,
                                        DatePickerDialogFragment.TAG
                                    )
                            }

                            is TransferSheetViewModel.Event.ProceedToCounterpartySelection -> {
                                flowNavController.navigate(
                                    route = TransferCounterpartySelectionSheetRoute(
                                        isForSource = event.selectSource,
                                        alreadySelectedCounterpartyId = event.alreadySelectedCounterpartyId,
                                        showCategories = event.showCategories,
                                        showAccounts = event.showAccounts,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            TransferSheetRoot(
                viewModel = viewModel,
            )
        }

        composable<TransferCounterpartySelectionSheetRoute> { entry ->
            val route = entry.toRoute<TransferCounterpartySelectionSheetRoute>()
            val viewModel = koinViewModel<TransferCounterpartySelectionSheetViewModel>()

            LaunchedEffect(route) {
                viewModel.setParameters(
                    isIncognito = isIncognito,
                    isForSource = route.isForSource,
                    showAccounts = route.showAccounts,
                    showCategories = route.showCategories,
                    alreadySelectedCounterpartyId = route.alreadySelectedCounterpartyId,
                )

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is TransferCounterpartySelectionSheetViewModel.Event.Selected -> {
                                event.result.setSelectedCounterpartyId(
                                    savedStateHandle = flowNavController
                                        .previousBackStackEntry
                                        !!.savedStateHandle,
                                )
                                flowNavController.navigateUp()
                            }
                        }
                    }
                }
            }

            TransferCounterpartySelectionSheetRoot(
                viewModel = viewModel,
            )
        }
    }
}
