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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.Subcategory
import ua.com.radiokot.money.categories.view.ViewSelectableSubcategoryListItem
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.logic.EditTransferUseCase
import ua.com.radiokot.money.transfers.logic.TransferFundsUseCase
import ua.com.radiokot.money.map
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class TransferSheetViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transferFundsUseCase: TransferFundsUseCase,
    private val editTransferUseCase: EditTransferUseCase,
) : ViewModel() {

    private val log by lazyLogger("TransferSheetVM")
    private val _sourceAmountValue: MutableStateFlow<BigInteger> = MutableStateFlow(BigInteger.ZERO)
    val sourceAmountValue = _sourceAmountValue.asStateFlow()
    private val _destinationAmountValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(BigInteger.ZERO)
    val destinationAmountValue = _destinationAmountValue.asStateFlow()
    private val _memo: MutableStateFlow<String> = MutableStateFlow("")
    val memo = _memo.asStateFlow()
    private val dateTime: MutableStateFlow<LocalDateTime> = MutableStateFlow(
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
    )
    private val subcategoryToSelect: MutableStateFlow<Subcategory?> = MutableStateFlow(null)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()
    private val requestedSourceCounterpartyId: MutableStateFlow<TransferCounterpartyId?> =
        MutableStateFlow(null)
    private val requestedDestinationCounterpartyId: MutableStateFlow<TransferCounterpartyId?> =
        MutableStateFlow(null)
    private var transferToEditId: String? = null

    private var sourceCounterparty: TransferCounterparty? = null
    private val sourceCounterpartySharedFlow: SharedFlow<TransferCounterparty> =
        requestedSourceCounterpartyId
            .filterNotNull()
            .flatMapLatestToCounterparty()
            .onEach(::sourceCounterparty::set)
            .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    private var destinationCounterparty: TransferCounterparty? = null
    private val destinationCounterpartySharedFlow: SharedFlow<TransferCounterparty> =
        requestedDestinationCounterpartyId
            .filterNotNull()
            .flatMapLatestToCounterparty()
            .onEach(::destinationCounterparty::set)
            .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    val source: StateFlow<ViewTransferCounterparty?> =
        sourceCounterpartySharedFlow
            .filterNotNull()
            .map(ViewTransferCounterparty::fromCounterparty)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val destination: StateFlow<ViewTransferCounterparty?> =
        destinationCounterpartySharedFlow
            .filterNotNull()
            .map(ViewTransferCounterparty::fromCounterparty)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val subcategoryItemList: StateFlow<List<ViewSelectableSubcategoryListItem>> =
        combine(
            sourceCounterpartySharedFlow,
            destinationCounterpartySharedFlow,
            transform = ::Pair
        )
            .flatMapLatest { (source, destination) ->
                val categoryCounterparty =
                    (source as? TransferCounterparty.Category)
                        ?: (destination as? TransferCounterparty.Category)
                        ?: return@flatMapLatest flowOf(emptyList())

                categoryRepository
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

    val subcategoriesColorScheme: StateFlow<ItemColorScheme?> =
        combine(
            sourceCounterpartySharedFlow,
            destinationCounterpartySharedFlow,
            transform = ::Pair
        )
            .map { (source, destination) ->
                val categoryCounterparty =
                    (source as? TransferCounterparty.Category)
                        ?: (destination as? TransferCounterparty.Category)
                        ?: return@map null
                categoryCounterparty.category.colorScheme
            }
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

    val date: StateFlow<ViewDate> =
        dateTime
            .map(viewModelScope, ::ViewDate)

    // Reset amounts if counterparty currency changes.
    init {
        viewModelScope.launch {
            var currentSourceCurrency: ViewCurrency? = null
            source.collect { updatedSource ->
                if (currentSourceCurrency != null
                    && updatedSource?.currency != currentSourceCurrency
                ) {
                    _sourceAmountValue.emit(BigInteger.ZERO)
                }
                currentSourceCurrency = updatedSource?.currency
            }
        }

        viewModelScope.launch {
            var currentDestinationCurrency: ViewCurrency? = null
            destination.collect { updatedDestination ->
                if (currentDestinationCurrency != null
                    && updatedDestination?.currency != currentDestinationCurrency
                ) {
                    _destinationAmountValue.emit(BigInteger.ZERO)
                }
                currentDestinationCurrency = updatedDestination?.currency
            }
        }
    }

    fun setParameters(
        sourceId: TransferCounterpartyId,
        destinationId: TransferCounterpartyId,
        transferToEditId: String?,
        sourceAmount: BigInteger?,
        destinationAmount: BigInteger?,
        memo: String?,
        dateTime: LocalDateTime?,
    ) {
        log.debug {
            "setParameters(): setting:" +
                    "\nsourceId=$sourceId," +
                    "\ndestinationId=$destinationId," +
                    "\ntransferToEditId=$transferToEditId," +
                    "\nsourceAmount=$sourceAmount," +
                    "\ndestinationAmount=$destinationAmount," +
                    "\nmemo=$memo," +
                    "\ntime=$dateTime"
        }

        requestedSourceCounterpartyId.tryEmit(sourceId)
        requestedDestinationCounterpartyId.tryEmit(destinationId)

        this.transferToEditId = transferToEditId
        sourceAmount?.also(_sourceAmountValue::tryEmit)
        destinationAmount?.also(_destinationAmountValue::tryEmit)
        memo?.also(_memo::tryEmit)
        dateTime?.also(this.dateTime::tryEmit)
    }

    fun onCounterpartiesSelected(
        newSourceId: TransferCounterpartyId?,
        newDestinationId: TransferCounterpartyId?,
    ) {
        log.debug {
            "onCounterpartiesSelected(): updating:" +
                    "\nnewSourceId=$newSourceId," +
                    "\nnewDestinationId=$newDestinationId"
        }

        newSourceId?.also(requestedSourceCounterpartyId::tryEmit)
        newDestinationId?.also(requestedDestinationCounterpartyId::tryEmit)
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

    fun onMemoUpdated(memo: String) {
        _memo.tryEmit(memo)
    }

    fun onSourceClicked() {
        val source = sourceCounterparty
            ?: error("Source counterparty must be set at this point")
        val destination = destinationCounterparty
            ?: error("Destination counterparty must be set at this point")

        log.debug {
            "onSourceClicked(): requesting to select source"
        }

        _events.tryEmit(
            Event.ProceedToCounterpartySelection(
                alreadySelectedCounterpartyId = destination.id,
                selectSource = true,
                showCategories = source is TransferCounterparty.Category,
                showAccounts = source is TransferCounterparty.Account,
            )
        )
    }

    fun onDestinationClicked() {
        val source = sourceCounterparty
            ?: error("Source counterparty must be set at this point")
        val destination = destinationCounterparty
            ?: error("Destination counterparty must be set at this point")

        log.debug {
            "onDestinationClicked(): requesting to select destination"
        }

        _events.tryEmit(
            Event.ProceedToCounterpartySelection(
                alreadySelectedCounterpartyId = source.id,
                selectSource = false,
                showCategories = destination is TransferCounterparty.Category,
                showAccounts = destination is TransferCounterparty.Account,
            )
        )
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
            ((sourceCounterparty as? TransferCounterparty.Category)?.subcategory)
                ?: ((destinationCounterparty as? TransferCounterparty.Category)?.subcategory)

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

    fun onDateClicked() {
        val currentDate = date.value.localDate

        log.debug {
            "onDateClicked(): requesting picking a date:" +
                    "\ncurrent=$currentDate"
        }

        _events.tryEmit(Event.ProceedToDatePicker(currentDate))
    }

    fun onDatePicked(
        newDate: LocalDate,
    ) {
        dateTime.update { currentDateTime ->
            LocalDateTime(
                date = newDate,
                time = currentDateTime.time,
            ).also {
                log.debug {
                    "onDatePicked(): updating date:" +
                            "\nnewDateTime=$it"
                }
            }
        }
    }

    fun onSaveClicked() {
        if (!isSaveEnabled.value) {
            log.warn {
                "onSaveClicked(): ignoring as save is not enabled"
            }
            return
        }

        if (transferToEditId != null) {
            editTransfer()
        } else {
            transferFunds()
        }
    }

    private var editTransferJob: Job? = null
    private fun editTransfer() {
        val transferId = transferToEditId
            ?: error("Transfer to edit ID must be set for this to be called")
        val source = sourceCounterparty
            ?: error("Source counterparty must be set at this point")
        val destination = destinationCounterparty
            ?: error("Destination counterparty must be set at this point")
        val destinationAmount = destinationAmountValue.value
        val sourceAmount =
            if (isSourceInputShown.value)
                sourceAmountValue.value
            else
                destinationAmount
        val memo = memo.value
            .trim()
            .takeIf(String::isNotEmpty)
        val dateTime = dateTime.value

        editTransferJob?.cancel()
        editTransferJob = viewModelScope.launch {
            log.debug {
                "editTransfer(): editing:" +
                        "\ntransferId=$transferId," +
                        "\nsource=$source," +
                        "\nsourceAmount=$sourceAmount," +
                        "\ndestination=$destination," +
                        "\ndestinationAmount=$destinationAmount," +
                        "\nmemo=$memo," +
                        "\ndateTime=$dateTime"
            }

            editTransferUseCase(
                transferId = transferId,
                sourceId = source.id,
                sourceAmount = sourceAmount,
                destinationId = destination.id,
                destinationAmount = destinationAmount,
                dateTime = dateTime,
                memo = memo,
            )
                .onFailure { error ->
                    log.error(error) {
                        "editTransfer(): failed to edit the transfer"
                    }
                }
                .onSuccess {
                    log.debug {
                        "editTransfer(): transfer edited"
                    }

                    _events.emit(Event.TransferDone)
                }
        }
    }

    private var transferJob: Job? = null
    private fun transferFunds() {
        val source = sourceCounterparty
            ?: error("Source counterparty must be set at this point")
        val destination = destinationCounterparty
            ?: error("Destination counterparty must be set at this point")
        val destinationAmount = destinationAmountValue.value
        val sourceAmount =
            if (isSourceInputShown.value)
                sourceAmountValue.value
            else
                destinationAmount
        val memo = memo.value
            .trim()
            .takeIf(String::isNotEmpty)
        val dateTime = dateTime.value

        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            log.debug {
                "transferFunds(): transferring:" +
                        "\nsource=$source," +
                        "\nsourceAmount=$sourceAmount," +
                        "\ndestination=$destination," +
                        "\ndestinationAmount=$destinationAmount," +
                        "\nmemo=$memo," +
                        "\ndateTime=$dateTime"
            }

            transferFundsUseCase(
                sourceId = source.id,
                sourceAmount = sourceAmount,
                destinationId = destination.id,
                destinationAmount = destinationAmount,
                dateTime = dateTime,
                memo = memo,
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
                    categoryRepository
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

        /**
         * Pass the picked date to [onDatePicked].
         */
        class ProceedToDatePicker(
            val currentDate: LocalDate,
        ) : Event

        object TransferDone : Event

        /**
         * Pass the selected IDs to [onCounterpartiesSelected].
         */
        class ProceedToCounterpartySelection(
            val alreadySelectedCounterpartyId: TransferCounterpartyId,
            val selectSource: Boolean,
            val showCategories: Boolean,
            val showAccounts: Boolean,
        ) : Event
    }
}
