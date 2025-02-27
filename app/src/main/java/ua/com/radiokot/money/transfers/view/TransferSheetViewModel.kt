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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.Subcategory
import ua.com.radiokot.money.categories.view.ViewSelectableSubcategoryListItem
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.logic.TransferFundsUseCase
import java.math.BigInteger

class TransferSheetViewModel(
    private val accountRepository: AccountRepository,
    private val categoriesRepository: CategoryRepository,
    private val transferFundsUseCase: TransferFundsUseCase,
) : ViewModel() {

    private val log by lazyLogger("TransferSheetVM")
    private var counterpartySubscriptionJob: Job? = null
    private val _sourceAmountValue: MutableStateFlow<BigInteger> = MutableStateFlow(BigInteger.ZERO)
    val sourceAmountValue = _sourceAmountValue.asStateFlow()
    private val _destinationAmountValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(BigInteger.ZERO)
    val destinationAmountValue = _destinationAmountValue.asStateFlow()
    private val selectedSubcategory: MutableStateFlow<Subcategory?> = MutableStateFlow(null)
    private val _subcategoryItemList: MutableStateFlow<List<ViewSelectableSubcategoryListItem>> =
        MutableStateFlow(emptyList())
    val subcategoryItemList = _subcategoryItemList.asStateFlow()
    private val sourceCounterparty: MutableStateFlow<TransferCounterparty?> =
        MutableStateFlow(null)
    private val destinationCounterparty: MutableStateFlow<TransferCounterparty?> =
        MutableStateFlow(null)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val source: StateFlow<ViewTransferCounterparty?> =
        sourceCounterparty
            .filterNotNull()
            .map(ViewTransferCounterparty::fromCounterparty)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val destination: StateFlow<ViewTransferCounterparty?> =
        destinationCounterparty
            .filterNotNull()
            .map(ViewTransferCounterparty::fromCounterparty)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isSourceInputShown: StateFlow<Boolean> =
        // Only require source input if currencies are different.
        source.combine(destination, ::Pair)
            .map { (source, destination) ->
                source?.currency != destination?.currency
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isSaveEnabled: StateFlow<Boolean> =
        // Only enable save if the input is valid.
        sourceAmountValue.combine(destinationAmountValue, ::Pair)
            .combine(isSourceInputShown, ::Pair)
            .map { (amounts, isSourceInputRequired) ->
                val (sourceAmountValue, destAmountValue) = amounts
                (!isSourceInputRequired || sourceAmountValue.signum() > 0)
                        && destAmountValue.signum() > 0
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setSourceAndDestination(
        sourceId: TransferCounterpartyId,
        destinationId: TransferCounterpartyId,
    ) {
        if (sourceCounterparty.value != null && destinationCounterparty.value != null) {
            log.debug {
                "setSourceAndDestination(): already set"
            }
            return
        }

        log.debug {
            "setSourceAndDestination(): setting: " +
                    "\nsourceId=$sourceId," +
                    "\ndestinationId=$destinationId"
        }

        counterpartySubscriptionJob?.cancel()
        counterpartySubscriptionJob = subscribeToFreshCounterparties(
            sourceId = sourceId,
            destinationId = destinationId,
        )

        _sourceAmountValue.tryEmit(BigInteger.ZERO)
        _destinationAmountValue.tryEmit(BigInteger.ZERO)
    }

    private fun subscribeToFreshCounterparties(
        sourceId: TransferCounterpartyId,
        destinationId: TransferCounterpartyId,
    ) = viewModelScope.launch {

        suspend fun subscribeToAccountCounterparty(
            accountId: TransferCounterpartyId.Account,
            collector: FlowCollector<TransferCounterparty>,
        ) = launch {
            accountRepository
                .getAccountFlow(accountId.toString())
                .map(TransferCounterparty::Account)
                .collect(collector)
        }

        suspend fun subscribeToCategoryCounterparty(
            categoryId: TransferCounterpartyId.Category,
            collector: FlowCollector<TransferCounterparty>,
        ) {
            selectedSubcategory.emit(null)

            launch {
                categoriesRepository
                    .getCategoryFlow(categoryId.categoryId)
                    .combine(selectedSubcategory, ::Pair)
                    .map { (category, selectedSubcategory) ->
                        TransferCounterparty.Category(
                            category = category,
                            subcategory = selectedSubcategory,
                        )
                    }
                    .collect(collector)
            }

            launch {
                categoriesRepository
                    .getSubcategoriesFlow(categoryId.categoryId)
                    .combine(selectedSubcategory, ::Pair)
                    .map { (subcategories, selectedSubcategory) ->
                        subcategories
                            .sortedBy(Subcategory::title)
                            .map { subcategory ->
                                ViewSelectableSubcategoryListItem(
                                    subcategory = subcategory,
                                    isSelected = subcategory == selectedSubcategory,
                                )
                            }
                    }
                    .collect(_subcategoryItemList)
            }
        }

        // Reset subcategories first.
        _subcategoryItemList.emit(emptyList())

        when (sourceId) {
            is TransferCounterpartyId.Account ->
                subscribeToAccountCounterparty(
                    accountId = sourceId,
                    collector = sourceCounterparty,
                )

            is TransferCounterpartyId.Category ->
                subscribeToCategoryCounterparty(
                    categoryId = sourceId,
                    collector = sourceCounterparty,
                )
        }

        when (destinationId) {
            is TransferCounterpartyId.Account ->
                subscribeToAccountCounterparty(
                    accountId = destinationId,
                    collector = destinationCounterparty,
                )

            is TransferCounterpartyId.Category ->
                subscribeToCategoryCounterparty(
                    categoryId = destinationId,
                    collector = destinationCounterparty,
                )
        }
    }

    private fun close() {
        log.debug {
            "close(): closing"
        }

        counterpartySubscriptionJob?.cancel()
        _events.tryEmit(Event.Close)
    }

    fun onNewSourceAmountValueParsed(value: BigInteger) {
        log.debug {
            "onNewSourceAmountValueParsed(): updating source amount:" +
                    "value=$value"
        }

        _sourceAmountValue.tryEmit(value)
    }

    fun onNewDestinationAmountValueParsed(value: BigInteger) {
        log.debug {
            "onNewDestinationAmountValueParsed(): updating dest amount:" +
                    "value=$value"
        }

        _destinationAmountValue.tryEmit(value)
    }

    fun onSubcategoryItemClicked(item: ViewSelectableSubcategoryListItem) {
        val clickedSubcategory = item.source
        if (clickedSubcategory == null) {
            log.warn {
                "onSubcategoryItemClicked(): ignoring as subcategory is null"
            }
            return
        }

        val currentSelectedSubcategory = selectedSubcategory.value
        if (currentSelectedSubcategory != clickedSubcategory) {
            log.debug {
                "onSubcategoryItemClicked(): selecting subcategory:" +
                        "\nselected=$clickedSubcategory"
            }

            selectedSubcategory.tryEmit(clickedSubcategory)
        } else {
            log.debug {
                "onSubcategoryItemClicked(): unselecting subcategory"
            }

            selectedSubcategory.tryEmit(null)
        }
    }

    fun onSaveClicked() {
        if (!isSaveEnabled.value) {
            log.warn {
                "onSaveClicked(): ignoring as save is not enabled"
            }
            return
        }

        transferFunds()
    }

    private var transferJob: Job? = null
    private fun transferFunds() {
        val source = sourceCounterparty.value
            ?: error("Source counterparty must be set at this point")
        val destination = destinationCounterparty.value
            ?: error("Destination counterparty must be set at this point")
        val destinationAmount = destinationAmountValue.value
        val sourceAmount =
            if (isSourceInputShown.value)
                sourceAmountValue.value
            else
                destinationAmount

        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            log.debug {
                "transferFunds(): transferring:" +
                        "\nsource=$source," +
                        "\nsourceAmount=$sourceAmount," +
                        "\ndestination=$destination," +
                        "\ndestinationAmount=$destinationAmount"
            }

            transferFundsUseCase(
                source = source,
                sourceAmount = sourceAmount,
                destination = destination,
                destinationAmount = destinationAmount,
            )
                .onFailure { error ->
                    log.error(error) {
                        "transferFunds(): failed to transfer funds"
                    }
                }
                .onSuccess {
                    log.debug {
                        "transferFunds(): funds transferred, closing"
                    }

                    close()
                }
        }
    }

    sealed interface Event {
        object Close : Event
    }
}
