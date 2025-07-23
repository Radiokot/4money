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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.view.ViewSelectableSubcategoryListItem
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.logic.EditTransferUseCase
import ua.com.radiokot.money.transfers.logic.TransferFundsUseCase
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class TransferSheetViewModel(
    private val parameters: Parameters,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transferFundsUseCase: TransferFundsUseCase,
    private val editTransferUseCase: EditTransferUseCase,
) : ViewModel() {

    private val log by lazyLogger("TransferSheetVM")
    private val _sourceCounterparty: MutableStateFlow<TransferCounterparty> =
        MutableStateFlow(runBlocking {
            parameters.sourceId.toCounterparty()
        })
    private val _destinationCounterparty: MutableStateFlow<TransferCounterparty> =
        MutableStateFlow(runBlocking {
            parameters.destinationId.toCounterparty()
        })
    private val _sourceAmountValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(parameters.sourceAmount ?: BigInteger.ZERO)
    val sourceAmountValue = _sourceAmountValue.asStateFlow()
    private val _destinationAmountValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(parameters.destinationAmount ?: BigInteger.ZERO)
    val destinationAmountValue = _destinationAmountValue.asStateFlow()
    private val _memo: MutableStateFlow<String> =
        MutableStateFlow(parameters.memo ?: "")
    val memo = _memo.asStateFlow()
    private val dateTime: MutableStateFlow<LocalDateTime> =
        MutableStateFlow(
            parameters.dateTime
                ?: Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
        )
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val source: StateFlow<ViewTransferCounterparty> =
        _sourceCounterparty
            .map(viewModelScope, ViewTransferCounterparty::fromCounterparty)

    val destination: StateFlow<ViewTransferCounterparty> =
        _destinationCounterparty
            .map(viewModelScope, ViewTransferCounterparty::fromCounterparty)

    val subcategoryItemList: StateFlow<List<ViewSelectableSubcategoryListItem>> =
        combine(
            _sourceCounterparty,
            _destinationCounterparty,
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
                            .sorted()
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
            _sourceCounterparty,
            _destinationCounterparty,
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
        combine(
            source,
            destination,
            transform = ::Pair
        )
            .map { (source, destination) ->
                source.currency != destination.currency
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
                    && updatedSource.currency != currentSourceCurrency
                ) {
                    _sourceAmountValue.emit(BigInteger.ZERO)
                }
                currentSourceCurrency = updatedSource.currency
            }
        }

        viewModelScope.launch {
            var currentDestinationCurrency: ViewCurrency? = null
            destination.collect { updatedDestination ->
                if (currentDestinationCurrency != null
                    && updatedDestination.currency != currentDestinationCurrency
                ) {
                    _destinationAmountValue.emit(BigInteger.ZERO)
                }
                currentDestinationCurrency = updatedDestination.currency
            }
        }
    }

    fun onCounterpartiesSelected(
        newSourceId: TransferCounterpartyId?,
        newDestinationId: TransferCounterpartyId?,
    ) = viewModelScope.launch {

        if (newSourceId != null && newSourceId != _sourceCounterparty.value.id) {
            _sourceCounterparty.value = newSourceId.toCounterparty().also {
                log.debug {
                    "onCounterpartiesSelected(): updating source:" +
                            "\nnewSource=$it"
                }
            }
        }

        if (newDestinationId != null && newDestinationId != _destinationCounterparty.value.id) {
            _destinationCounterparty.value = newDestinationId.toCounterparty().also {
                log.debug {
                    "onCounterpartiesSelected(): updating destination:" +
                            "\nnewDestination=$it"
                }
            }
        }
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
        val sourceCounterparty = _sourceCounterparty.value
        val destinationCounterparty = _destinationCounterparty.value

        log.debug {
            "onSourceClicked(): requesting to select source"
        }

        _events.tryEmit(
            Event.ProceedToCounterpartySelection(
                alreadySelectedCounterpartyId = destinationCounterparty.id,
                selectSource = true,
                showCategories = sourceCounterparty is TransferCounterparty.Category,
                showAccounts = sourceCounterparty is TransferCounterparty.Account,
            )
        )
    }

    fun onDestinationClicked() {
        val sourceCounterparty = _sourceCounterparty.value
        val destinationCounterparty = _destinationCounterparty.value

        log.debug {
            "onDestinationClicked(): requesting to select destination"
        }

        _events.tryEmit(
            Event.ProceedToCounterpartySelection(
                alreadySelectedCounterpartyId = sourceCounterparty.id,
                selectSource = false,
                showCategories = destinationCounterparty is TransferCounterparty.Category,
                showAccounts = destinationCounterparty is TransferCounterparty.Account,
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

        val sourceCategoryCounterparty: TransferCounterparty.Category? =
            _sourceCounterparty.value as? TransferCounterparty.Category
        val destinationCategoryCounterparty: TransferCounterparty.Category? =
            _destinationCounterparty.value as? TransferCounterparty.Category
        val currentSelectedSubcategory =
            (sourceCategoryCounterparty?.subcategory)
                ?: (destinationCategoryCounterparty?.subcategory)

        if (currentSelectedSubcategory != clickedSubcategory) {
            log.debug {
                "onSubcategoryItemClicked(): selecting subcategory:" +
                        "\nselected=$clickedSubcategory"
            }

            if (sourceCategoryCounterparty != null) {
                _sourceCounterparty.value =
                    TransferCounterparty.Category(
                        category = sourceCategoryCounterparty.category,
                        subcategory = clickedSubcategory,
                    )
            } else if (destinationCategoryCounterparty != null) {
                _destinationCounterparty.value =
                    TransferCounterparty.Category(
                        category = destinationCategoryCounterparty.category,
                        subcategory = clickedSubcategory,
                    )
            }
        } else {
            log.debug {
                "onSubcategoryItemClicked(): unselecting subcategory"
            }

            if (sourceCategoryCounterparty != null) {
                _sourceCounterparty.value =
                    TransferCounterparty.Category(
                        category = sourceCategoryCounterparty.category,
                        subcategory = null,
                    )
            } else if (destinationCategoryCounterparty != null) {
                _destinationCounterparty.value =
                    TransferCounterparty.Category(
                        category = destinationCategoryCounterparty.category,
                        subcategory = null,
                    )
            }
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

        if (parameters.transferToEditId != null) {
            editTransfer()
        } else {
            transferFunds()
        }
    }

    private var editTransferJob: Job? = null
    private fun editTransfer() {
        val transferId = parameters.transferToEditId
            ?: error("Transfer to edit ID must be set for this to be called")
        val sourceCounterparty = _sourceCounterparty.value
        val destinationCounterparty = _destinationCounterparty.value
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
                        "\nsource=$sourceCounterparty," +
                        "\nsourceAmount=$sourceAmount," +
                        "\ndestination=$destinationCounterparty," +
                        "\ndestinationAmount=$destinationAmount," +
                        "\nmemo=$memo," +
                        "\ndateTime=$dateTime"
            }

            editTransferUseCase(
                transferId = transferId,
                sourceId = sourceCounterparty.id,
                sourceAmount = sourceAmount,
                destinationId = destinationCounterparty.id,
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
                    log.info {
                        "Edited transfer $transferId " +
                                "to transfer $sourceAmount from $sourceCounterparty " +
                                "as $destinationAmount to $destinationCounterparty at $dateTime"
                    }

                    log.debug {
                        "editTransfer(): transfer edited"
                    }

                    _events.emit(Event.TransferDone)
                }
        }
    }

    private var transferJob: Job? = null
    private fun transferFunds() {
        val sourceCounterparty = _sourceCounterparty.value
        val destinationCounterparty = _destinationCounterparty.value
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
                        "\nsource=$sourceCounterparty," +
                        "\nsourceAmount=$sourceAmount," +
                        "\ndestination=$destinationCounterparty," +
                        "\ndestinationAmount=$destinationAmount," +
                        "\nmemo=$memo," +
                        "\ndateTime=$dateTime"
            }

            transferFundsUseCase(
                sourceId = sourceCounterparty.id,
                sourceAmount = sourceAmount,
                destinationId = destinationCounterparty.id,
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
                    log.info {
                        "Transferred $sourceAmount from $sourceCounterparty " +
                                "as $destinationAmount to $destinationCounterparty"
                    }

                    log.debug {
                        "transferFunds(): funds transferred"
                    }

                    _events.emit(Event.TransferDone)
                }
        }
    }

    private suspend fun TransferCounterpartyId.toCounterparty(

    ): TransferCounterparty = when (this) {

        is TransferCounterpartyId.Account ->
            TransferCounterparty.Account(
                account = accountRepository
                    .getAccount(accountId)!!,
            )

        is TransferCounterpartyId.Category ->
            TransferCounterparty.Category(
                category = categoryRepository
                    .getCategory(categoryId)!!,
                subcategory =
                if (subcategoryId != null)
                    categoryRepository
                        .getSubcategory(subcategoryId)!!
                else
                    null
            )
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

    class Parameters(
        val sourceId: TransferCounterpartyId,
        val destinationId: TransferCounterpartyId,
        val transferToEditId: String?,
        val sourceAmount: BigInteger?,
        val destinationAmount: BigInteger?,
        val memo: String?,
        val dateTime: LocalDateTime?,
    )
}
