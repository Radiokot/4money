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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransferSheetViewModel(
    private val accountRepository: AccountRepository,
    private val categoriesRepository: CategoryRepository,
    private val transferFundsUseCase: TransferFundsUseCase,
) : ViewModel() {

    private val log by lazyLogger("TransferSheetVM")
    private val _sourceAmountValue: MutableStateFlow<BigInteger> = MutableStateFlow(BigInteger.ZERO)
    val sourceAmountValue = _sourceAmountValue.asStateFlow()
    private val _destinationAmountValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(BigInteger.ZERO)
    val destinationAmountValue = _destinationAmountValue.asStateFlow()
    private val subcategoryToSelect: MutableStateFlow<Subcategory?> = MutableStateFlow(null)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()
    private val requestedSourceCounterpartyId: MutableStateFlow<TransferCounterpartyId?> =
        MutableStateFlow(null)
    private val requestedDestinationCounterpartyId: MutableStateFlow<TransferCounterpartyId?> =
        MutableStateFlow(null)

    private val sourceCounterparty: StateFlow<TransferCounterparty?> =
        requestedSourceCounterpartyId
            .filterNotNull()
            .flatMapLatestToCounterparty()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val destinationCounterparty: StateFlow<TransferCounterparty?> =
        requestedDestinationCounterpartyId
            .filterNotNull()
            .flatMapLatestToCounterparty()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

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

    val subcategoryItemList: StateFlow<List<ViewSelectableSubcategoryListItem>> =
        combine(
            sourceCounterparty,
            destinationCounterparty,
            transform = ::Pair
        )
            .flatMapLatest { (source, destination) ->
                val categoryCounterparty =
                    (source as? TransferCounterparty.Category)
                        ?: (destination as? TransferCounterparty.Category)
                        ?: return@flatMapLatest flowOf(emptyList())

                categoriesRepository
                    .getSubcategoriesFlow(categoryCounterparty.category.id)
                    .map { subcategories ->
                        subcategories
                            .sortedBy(Subcategory::title)
                            .map { subcategory ->
                                ViewSelectableSubcategoryListItem(
                                    subcategory = subcategory,
                                    isSelected = subcategory == categoryCounterparty.subcategory,
                                )
                            }
                    }

            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ) {
        log.debug {
            "setSourceAndDestination(): setting: " +
                    "\nsourceId=$sourceId," +
                    "\ndestinationId=$destinationId"
        }

        sourceId?.let(requestedSourceCounterpartyId::tryEmit)
        destinationId?.let(requestedDestinationCounterpartyId::tryEmit)
        _sourceAmountValue.tryEmit(BigInteger.ZERO)
        _destinationAmountValue.tryEmit(BigInteger.ZERO)
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

        val currentSelectedSubcategory =
            ((sourceCounterparty.value as? TransferCounterparty.Category)?.subcategory)
                ?: ((destinationCounterparty.value as? TransferCounterparty.Category)?.subcategory)

        if (currentSelectedSubcategory != clickedSubcategory) {
            log.debug {
                "onSubcategoryItemClicked(): selecting subcategory:" +
                        "\nselected=$clickedSubcategory"
            }

            subcategoryToSelect.tryEmit(clickedSubcategory)
        } else {
            log.debug {
                "onSubcategoryItemClicked(): unselecting subcategory"
            }

            subcategoryToSelect.tryEmit(null)
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
                        "transferFunds(): funds transferred"
                    }

                    _events.emit(Event.TransferDone)
                }
        }
    }

    private fun Flow<TransferCounterpartyId>.flatMapLatestToCounterparty() =
        flatMapLatest { id ->
            when (id) {
                is TransferCounterpartyId.Account ->
                    accountRepository
                        .getAccountFlow(id.accountId)
                        .map(TransferCounterparty::Account)

                is TransferCounterpartyId.Category ->
                    categoriesRepository
                        .getCategoryFlow(id.categoryId)
                        // Reset subcategory to select when the counterparty changes.
                        .onStart { subcategoryToSelect.emit(null) }
                        .combine(subcategoryToSelect, ::Pair)
                        .map { (category, subcategoryToSelect) ->
                            TransferCounterparty.Category(
                                category = category,
                                subcategory = subcategoryToSelect,
                            )
                        }
            }
        }

    sealed interface Event {
        object TransferDone : Event
    }
}
