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
import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.showSingle
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId

@Serializable
data class TransferSheetRoute(
    val sourceAccountId: String? = null,
    val sourceCategoryId: String? = null,
    val sourceSubcategoryId: String? = null,
    val destinationAccountId: String? = null,
    val destinationCategoryId: String? = null,
    val destinationSubcategoryId: String? = null,
) {
    constructor(
        sourceId: TransferCounterpartyId,
        destinationId: TransferCounterpartyId,
    ) : this(
        sourceAccountId = (sourceId as? TransferCounterpartyId.Account)?.accountId,
        sourceCategoryId = (sourceId as? TransferCounterpartyId.Category)?.categoryId,
        sourceSubcategoryId = (sourceId as? TransferCounterpartyId.Category)?.subcategoryId,
        destinationAccountId = (destinationId as? TransferCounterpartyId.Account)?.accountId,
        destinationCategoryId = (destinationId as? TransferCounterpartyId.Category)?.categoryId,
        destinationSubcategoryId = (destinationId as? TransferCounterpartyId.Category)?.subcategoryId,
    )

    val sourceId: TransferCounterpartyId
        get() = when {
            sourceAccountId != null ->
                TransferCounterpartyId.Account(sourceAccountId)

            sourceCategoryId != null ->
                TransferCounterpartyId.Category(
                    categoryId = sourceCategoryId,
                    subcategoryId = sourceSubcategoryId,
                )

            else ->
                error("All the source IDs are missing")
        }

    val destinationId: TransferCounterpartyId
        get() = when {
            destinationAccountId != null ->
                TransferCounterpartyId.Account(destinationAccountId)

            destinationCategoryId != null ->
                TransferCounterpartyId.Category(
                    categoryId = destinationCategoryId,
                    subcategoryId = destinationSubcategoryId,
                )

            else ->
                error("All the destination IDs are missing")
        }
}

fun NavGraphBuilder.transferSheet(
    onTransferDone: () -> Unit,
) = bottomSheet<TransferSheetRoute> { entry ->
    val arguments = entry.toRoute<TransferSheetRoute>()
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

    LaunchedEffect(arguments) {
        viewModel.setSourceAndDestination(
            sourceId = arguments.sourceId,
            destinationId = arguments.destinationId,
        )

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
                }
            }
        }
    }

    TransferSheetRoot(
        viewModel = viewModel,
    )
}
