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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.routeIs
import ua.com.radiokot.money.showSingle
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterparty
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

@Serializable
class TransferFlowRoute
private constructor(
    private val categoryIdJson: String?,
    private val accountIdJson: String?,
    private val isIncome: Boolean?,
    private val transferScreenRouteJson: String?,
) {
    val actualDataForGodsSake: Data by lazy {
        when {
            transferScreenRouteJson != null ->
                Data.ReadyTransfer(
                    route = Json.decodeFromString(transferScreenRouteJson),
                )

            categoryIdJson != null ->
                Data.FromToCategory(
                    id = Json.decodeFromString(categoryIdJson),
                    isIncome = isIncome!!,
                )

            accountIdJson != null ->
                Data.FromToAccount(
                    id = Json.decodeFromString(accountIdJson),
                    isIncome = isIncome,
                )

            else ->
                error("Unknown parameter configuration")
        }
    }


    constructor(
        category: Category,
    ) : this(
        categoryIdJson = Json.encodeToString(TransferCounterparty.Category(category).id),
        isIncome = category.isIncome,
        accountIdJson = null,
        transferScreenRouteJson = null,
    )

    constructor(
        accountId: TransferCounterpartyId.Account,
        isIncome: Boolean?,
    ) : this(
        accountIdJson = Json.encodeToString(accountId),
        isIncome = isIncome,
        categoryIdJson = null,
        transferScreenRouteJson = null,
    )

    constructor(
        transferToEdit: Transfer,
    ) : this(
        transferScreenRouteJson = Json.encodeToString(TransferSheetRoute(transferToEdit)),
        accountIdJson = null,
        categoryIdJson = null,
        isIncome = null,
    )

    sealed interface Data {

        class FromToCategory(
            val id: TransferCounterpartyId.Category,
            val isIncome: Boolean,
        ) : Data

        class FromToAccount(
            val id: TransferCounterpartyId.Account,
            val isIncome: Boolean?,
        ) : Data

        class ReadyTransfer(
            val route: TransferSheetRoute,
        ) : Data
    }
}

fun NavGraphBuilder.transferFlowSheet(
    isIncognito: Boolean,
    lastUsedAccountByCategory: Flow<Map<String, Account>>,
    onTransferDone: () -> Unit,
) = bottomSheet<TransferFlowRoute> { flowStartEntry ->
    val flowNavController = rememberNavController()
    val flowStartRoute = flowStartEntry.toRoute<TransferFlowRoute>()

    val startDestination: Any = remember(flowStartRoute) {
        when (val flowStartData = flowStartRoute.actualDataForGodsSake) {

            is TransferFlowRoute.Data.FromToAccount -> {
                TransferCounterpartySelectionSheetRoute(
                    isForSource = flowStartData.isIncome == true,
                    alreadySelectedCounterpartyId = flowStartData.id,
                    showCategories = flowStartData.isIncome != null,
                )
            }

            is TransferFlowRoute.Data.FromToCategory -> {
                val lastUsedAccount: Account? = runBlocking {
                    lastUsedAccountByCategory.first()[flowStartData.id.categoryId]
                }

                if (lastUsedAccount != null)
                    if (flowStartData.isIncome)
                        TransferSheetRoute(
                            sourceId = flowStartData.id,
                            destinationId = TransferCounterparty.Account(lastUsedAccount).id,
                        )
                    else
                        TransferSheetRoute(
                            sourceId = TransferCounterparty.Account(lastUsedAccount).id,
                            destinationId = flowStartData.id,
                        )
                else
                    TransferCounterpartySelectionSheetRoute(
                        isForSource = !flowStartData.isIncome,
                        alreadySelectedCounterpartyId = flowStartData.id,
                        showCategories = false,
                        isIncognito = isIncognito,
                    )
            }

            is TransferFlowRoute.Data.ReadyTransfer ->
                flowStartData.route
        }
    }

    NavHost(
        navController = flowNavController,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        startDestination = startDestination,
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

            LaunchedEffect(entry) {
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

            LaunchedEffect(entry) {
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

                                val previousBackStackEntry =
                                    flowNavController.previousBackStackEntry

                                // In case the selection was requested by the transfer sheet,
                                // pass it back through the saved state handle.
                                if (previousBackStackEntry?.destination?.routeIs<TransferSheetRoute>() == true) {
                                    event.result.setSelectedCounterpartyId(
                                        savedStateHandle = previousBackStackEntry.savedStateHandle,
                                    )
                                    flowNavController.navigateUp()
                                    return@collect
                                }

                                val popUpToCounterpartySelection = navOptions {
                                    popUpTo<TransferCounterpartySelectionSheetRoute> {
                                        inclusive = true
                                    }
                                }

                                if (event.result.otherSelectedCounterpartyId == null) {
                                    flowNavController.navigate(
                                        route = TransferCounterpartySelectionSheetRoute(
                                            isForSource = !event.result.isSelectedAsSource,
                                            alreadySelectedCounterpartyId = event.result.selectedCounterparty.id,
                                            showCategories = event.result.selectedCounterparty is TransferCounterparty.Account,
                                            showAccounts = true,
                                        ),
                                        navOptions = popUpToCounterpartySelection,
                                    )
                                    return@collect
                                }

                                flowNavController.navigate(
                                    route =
                                    if (event.result.isSelectedAsSource)
                                        TransferSheetRoute(
                                            sourceId = event.result.selectedCounterparty.id,
                                            destinationId = event.result.otherSelectedCounterpartyId,
                                        )
                                    else
                                        TransferSheetRoute(
                                            sourceId = event.result.otherSelectedCounterpartyId,
                                            destinationId = event.result.selectedCounterparty.id,
                                        ),
                                    navOptions = popUpToCounterpartySelection,
                                )
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
