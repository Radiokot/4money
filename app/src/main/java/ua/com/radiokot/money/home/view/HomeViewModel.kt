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

package ua.com.radiokot.money.home.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.logic.GetLastUsedAccountsByCategoryUseCase

class HomeViewModel(
    private val getLastUsedAccountsByCategoryUseCase: GetLastUsedAccountsByCategoryUseCase,
) : ViewModel() {

    private var lastUsedAccountsByCategoryDeferred: Deferred<Map<String, Account>> =
        viewModelScope.async { getLastUsedAccountsByCategoryUseCase() }
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private var proceedToTransferWithCategoryJob: Job? = null
    fun onProceedToTransferWithCategory(category: Category) {
        proceedToTransferWithCategoryJob?.cancel()
        proceedToTransferWithCategoryJob = viewModelScope.launch {
            val lastUsedAccount = lastUsedAccountsByCategoryDeferred.await()[category.id]
            if (lastUsedAccount != null) {
                proceedToTransfer(
                    category = category,
                    account = lastUsedAccount,
                )
            } else {
                _events.tryEmit(
                    Event.ProceedToTransferCounterpartySelectionWithCategory(
                        category = category,
                    )
                )
            }
        }
    }

    private fun proceedToTransfer(
        category: Category,
        account: Account,
    ) {
        _events.tryEmit(
            if (category.isIncome)
                Event.ProceedToTransfer(
                    sourceId = TransferCounterparty.Category(category).id,
                    destinationId = TransferCounterparty.Account(account).id,
                )
            else
                Event.ProceedToTransfer(
                    sourceId = TransferCounterparty.Account(account).id,
                    destinationId = TransferCounterparty.Category(category).id,
                )
        )
    }

    sealed interface Event {

        class ProceedToTransfer(
            val sourceId: TransferCounterpartyId,
            val destinationId: TransferCounterpartyId,
        ) : Event

        class ProceedToTransferCounterpartySelectionWithCategory(
            val category: Category,
        ) : Event
    }
}
