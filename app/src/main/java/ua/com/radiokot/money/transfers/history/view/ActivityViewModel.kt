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
import androidx.paging.filter
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.home.view.HomeViewModel
import ua.com.radiokot.money.isSameDayAs
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import ua.com.radiokot.money.transfers.logic.RevertTransferUseCase
import ua.com.radiokot.money.transfers.view.ViewDate
import ua.com.radiokot.money.transfers.view.ViewTransferListItem

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    homeViewModel: HomeViewModel,
    private val transferHistoryRepository: TransferHistoryRepository,
    private val revertTransferUseCase: RevertTransferUseCase,
) : ViewModel() {

    private val log by lazyLogger("ActivityVM")
    private val localTimeZone = TimeZone.currentSystemDefault()
    private val revertedTransfers: MutableStateFlow<Set<Transfer>> = MutableStateFlow(emptySet())

    private val transferHistoryPagerFlow: Flow<Pager<Instant, Transfer>> =
        homeViewModel.period.mapLatest { period ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory =
                {
                    transferHistoryRepository.getTransferHistoryPagingSource(
                        sourceId = null,
                        destinationId = null,
                        period = period,
                    )
                },
            )
        }

    val transferItemPagingFlow: Flow<PagingData<ViewTransferListItem>> =
        combine(
            transferHistoryPagerFlow
                .flatMapLatest { it.flow }
                .cachedIn(viewModelScope),
            revertedTransfers,
            transform = ::Pair
        ).map { (pagingData, revertedTransfers) ->
            val today = Clock.System.now().toLocalDateTime(localTimeZone).date
            val yesterday = today.minus(1, DateTimeUnit.DAY)

            pagingData
                .filter { it !in revertedTransfers }
                .map<Transfer, Pair<ViewTransferListItem, LocalDate>> { transfer ->
                    val transferLocalDate = transfer.getLocalDateAt(localTimeZone)
                    val transferListItem = ViewTransferListItem.Transfer.fromTransfer(transfer)
                    transferListItem to transferLocalDate
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

        revertTransfer(transfer)
    }

    private var revertTransferJob: Job? = null
    private fun revertTransfer(transfer: Transfer) {
        revertTransferJob?.cancel()
        revertTransferJob = viewModelScope.launch {
            log.debug {
                "revertTransfer(): reverting:" +
                        "\ntransfer=$transfer"
            }

            revertTransferUseCase(
                transferId = transfer.id,
            )
                .onFailure { error ->
                    log.error(error) {
                        "revertTransfer(): failed to revert transfer"
                    }
                }
                .onSuccess {
                    log.debug {
                        "revertTransfer(): transfer reverted, filtering list items"
                    }

                    revertedTransfers.update { it + transfer }
                }
        }
    }
}
