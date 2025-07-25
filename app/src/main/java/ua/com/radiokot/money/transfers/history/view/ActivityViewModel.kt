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

package ua.com.radiokot.money.transfers.history.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.isSameDayAs
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import ua.com.radiokot.money.transfers.logic.RevertTransferUseCase
import ua.com.radiokot.money.transfers.view.ViewDate
import ua.com.radiokot.money.transfers.view.ViewTransferListItem

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    historyStatsPeriodViewModel: HistoryStatsPeriodViewModel,
    private val activityFilterViewModelDelegate: ActivityFilterViewModelDelegate,
    private val transferHistoryRepository: TransferHistoryRepository,
    private val revertTransferUseCase: RevertTransferUseCase,
) : ViewModel(),
    HistoryStatsPeriodViewModel by historyStatsPeriodViewModel,
    ActivityFilterViewModel by activityFilterViewModelDelegate {

    private val log by lazyLogger("ActivityVM")
    private val localTimeZone = TimeZone.currentSystemDefault()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()
    val isBackHandlerEnabled: StateFlow<Boolean> =
        activityFilterViewModelDelegate.activityFilterTransferCounterparties
            .map(viewModelScope) { counterparties ->
                !counterparties.isNullOrEmpty()
            }

    private val transferHistoryPagerFlow: Flow<Pager<*, Transfer>> =
        combine(
            historyStatsPeriod,
            activityFilterViewModelDelegate.activityFilterTransferCounterparties,
            transform = ::Pair
        )
            .mapLatest { (period, counterparties) ->
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory =
                    {
                        transferHistoryRepository.getTransferHistoryPagingSource(
                            withinPeriod = period,
                            counterpartyIds = counterparties
                                ?.mapTo(mutableSetOf()) { counterparty ->
                                    counterparty.id.toString()
                                },
                        )
                    },
                )
            }

    val transferItemPagingFlow: Flow<PagingData<ViewTransferListItem>> =
        transferHistoryPagerFlow
            .flatMapLatest { it.flow }
            .cachedIn(viewModelScope)
            .map { pagingData ->
                val today = Clock.System.now().toLocalDateTime(localTimeZone).date
                val yesterday = today.minus(1, DateTimeUnit.DAY)

                pagingData
                    .map<Transfer, Pair<ViewTransferListItem, LocalDate>> { transfer ->
                        val transferListItem =
                            ViewTransferListItem.Transfer.fromTransfer(transfer)
                        transferListItem to transfer.dateTime.date
                    }
                    .insertSeparators { previousItemDatePair, nextItemDatePair ->
                        val previousLocalDate = previousItemDatePair?.second
                        val nextLocalDate = nextItemDatePair?.second

                        if (nextLocalDate != null &&
                            (previousLocalDate == null || !nextLocalDate.isSameDayAs(
                                previousLocalDate
                            ))
                        ) {
                            val header = ViewTransferListItem.Header(
                                date = ViewDate(
                                    localDate = nextLocalDate,
                                    today = today,
                                    yesterday = yesterday,
                                ),
                            )

                            header to nextLocalDate
                        } else {
                            null
                        }
                    }
                    .map { it.first }
            }
            .flowOn(Dispatchers.Default)

    fun onTransferItemClicked(item: ViewTransferListItem.Transfer) {
        val transfer = item.source
        if (transfer == null) {
            log.debug {
                "onTransferItemClicked(): missing transfer source"
            }
            return
        }

        _events.tryEmit(
            Event.ProceedToEditingTransfer(
                transferToEdit = transfer,
            )
        )
    }

    fun onTransferItemLongClicked(item: ViewTransferListItem.Transfer) {
        val transfer = item.source
        if (transfer == null) {
            log.debug {
                "onTransferItemLongClicked(): missing transfer source"
            }
            return
        }

        _events.tryEmit(
            Event.ProceedToRevertingTransferConfirmation(
                transferToRevertId = transfer.id,
            )
        )
    }

    fun onTransferRevertConfirmed(
        transferToRevertId: String,
    ) {
        revertTransfer(
            id = transferToRevertId,
        )
    }

    private var revertTransferJob: Job? = null
    private fun revertTransfer(id: String) {
        revertTransferJob?.cancel()
        revertTransferJob = viewModelScope.launch {
            log.debug {
                "revertTransfer(): reverting:" +
                        "\nid=$id"
            }

            revertTransferUseCase(
                transferId = id,
            )
                .onFailure { error ->
                    log.error(error) {
                        "revertTransfer(): failed to revert transfer"
                    }
                }
                .onSuccess {
                    log.info {
                        "Reverted transfer $id"
                    }

                    log.debug {
                        "revertTransfer(): transfer reverted"
                    }
                }
        }
    }

    fun onBack() {

        if (!isBackHandlerEnabled.value) {
            log.warn {
                "onBack(): ignoring as handler is disabled"
            }
            return
        }

        activityFilterViewModelDelegate
            .activityFilterTransferCounterparties
            .value
            ?.forEach(activityFilterViewModelDelegate::removeCounterpartyFromActivityFilter)
    }

    sealed interface Event {

        class ProceedToEditingTransfer(
            val transferToEdit: Transfer,
        ) : Event

        /**
         * Pass the confirmation to [onTransferRevertConfirmed].
         */
        class ProceedToRevertingTransferConfirmation(
            val transferToRevertId: String,
        ) : Event
    }
}
