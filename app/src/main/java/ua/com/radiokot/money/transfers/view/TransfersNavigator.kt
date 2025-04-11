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

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.routeIs
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.logic.GetLastUsedAccountsByCategoryUseCase

class TransfersNavigator(
    getLastUsedAccountsByCategoryUseCase: GetLastUsedAccountsByCategoryUseCase,
    private val isIncognito: Boolean,
    private val navController: NavController,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val lastUsedAccountsByCategoryFlow = getLastUsedAccountsByCategoryUseCase()

    private var proceedToTransferWithCategoryJob: Job? = null
    fun proceedToTransfer(
        category: Category,
        navOptions: NavOptions? = null,
    ) {
        proceedToTransferWithCategoryJob?.cancel()
        proceedToTransferWithCategoryJob = coroutineScope.launch {
            val lastUsedAccount = lastUsedAccountsByCategoryFlow.first()[category.id]
            if (lastUsedAccount != null) {
                navController.navigate(
                    route =
                    if (category.isIncome)
                        TransferSheetRoute(
                            sourceId = TransferCounterparty.Category(category).id,
                            destinationId = TransferCounterparty.Account(lastUsedAccount).id,
                        )
                    else
                        TransferSheetRoute(
                            sourceId = TransferCounterparty.Account(lastUsedAccount).id,
                            destinationId = TransferCounterparty.Category(category).id,
                        ),
                    navOptions = navOptions,
                )
            } else {
                navController.navigate(
                    route = TransferCounterpartySelectionSheetRoute(
                        isForSource = !category.isIncome,
                        alreadySelectedCounterpartyId = TransferCounterparty.Category(category).id,
                        showCategories = false,
                        isIncognito = isIncognito,
                    ),
                    navOptions = navOptions,
                )
            }
        }
    }

    fun proceedToTransfer(
        accountId: TransferCounterpartyId.Account,
        isIncome: Boolean?,
        navOptions: NavOptions? = null,
    ) =
        navController.navigate(
            route = TransferCounterpartySelectionSheetRoute(
                isForSource = isIncome == true,
                alreadySelectedCounterpartyId = accountId,
                showCategories = isIncome != null,
                isIncognito = isIncognito,
            ),
            navOptions = navOptions,
        )

    fun proceedToTransfer(
        counterpartySelectionResult: TransferCounterpartySelectionResult,
    ) = with(counterpartySelectionResult) {

        val previousBackStackEntry = navController.previousBackStackEntry

        // In case the selection was requested by the transfer sheet,
        // pass it back through the saved state handle.
        if (previousBackStackEntry?.destination?.routeIs<TransferSheetRoute>() == true) {
            counterpartySelectionResult.setSelectedCounterpartyId(
                savedStateHandle = previousBackStackEntry.savedStateHandle,
            )
            navController.navigateUp()
            return@with
        }

        val popUpToCounterpartySelection = navOptions {
            popUpTo<TransferCounterpartySelectionSheetRoute> {
                inclusive = true
            }
        }

        if (otherSelectedCounterpartyId == null) {
            when (selectedCounterparty) {
                is TransferCounterparty.Category -> {
                    proceedToTransfer(
                        category = selectedCounterparty.category,
                        navOptions = popUpToCounterpartySelection,
                    )
                }

                is TransferCounterparty.Account -> {
                    proceedToTransfer(
                        accountId = selectedCounterparty.id,
                        isIncome = null,
                        navOptions = popUpToCounterpartySelection,
                    )
                }
            }
            return@with
        }

        navController.navigate(
            route =
            if (isSelectedAsSource)
                TransferSheetRoute(
                    sourceId = selectedCounterparty.id,
                    destinationId = otherSelectedCounterpartyId,
                )
            else
                TransferSheetRoute(
                    sourceId = otherSelectedCounterpartyId,
                    destinationId = selectedCounterparty.id,
                ),
            navOptions = popUpToCounterpartySelection,
        )
    }

    fun proceedToTransfer(
        transferToEdit: Transfer,
        navOptions: NavOptions? = null,
    ) {
        navController.navigate(
            route = TransferSheetRoute(
                transferToEdit = transferToEdit,
            ),
            navOptions = navOptions,
        )
    }

    fun interface Factory {
        fun create(
            isIncognito: Boolean,
            navController: NavController,
        ): TransfersNavigator
    }
}
