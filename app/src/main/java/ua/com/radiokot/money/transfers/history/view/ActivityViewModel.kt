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
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.isSameDayAs
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import ua.com.radiokot.money.transfers.view.ViewTransferListItem

class ActivityViewModel(
    private val transferHistoryRepository: TransferHistoryRepository,
) : ViewModel() {
    private val localTimeZone = TimeZone.currentSystemDefault()

    private val transferHistoryPager: Pager<Instant, Transfer> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            transferHistoryRepository.getTransferHistoryPagingSource(
                source = null,
                destination = null,
                period = HistoryPeriod.Month(),
            )
        },
    )

    val transferItemPagingFlow = transferHistoryPager.flow.map { page ->
        val today = Clock.System.now().toLocalDateTime(localTimeZone).date
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        page
            .map<Transfer, Pair<ViewTransferListItem, LocalDate>> { transfer ->
                val transferLocalDate = transfer.getLocalDateAt(localTimeZone)
                val transferListItem = ViewTransferListItem.Transfer.fromTransfer(transfer)
                transferListItem to transferLocalDate
            }
            .insertSeparators { previousItemDatePair, nextItemDatePair ->
                val previousLocalDate = previousItemDatePair?.second
                val nextLocalDate = nextItemDatePair?.second

                if (nextLocalDate != null &&
                    (previousLocalDate == null || !nextLocalDate.isSameDayAs(previousLocalDate))
                ) {
                    val header = ViewTransferListItem.Header(
                        localDate = nextLocalDate,
                        dayType = when {
                            nextLocalDate.isSameDayAs(today) ->
                                ViewTransferListItem.Header.DayType.Today

                            nextLocalDate.isSameDayAs(yesterday) ->
                                ViewTransferListItem.Header.DayType.Yesterday

                            else ->
                                ViewTransferListItem.Header.DayType.DayOfWeek
                        }
                    )

                    header to nextLocalDate
                } else {
                    null
                }
            }
            .map { it.first }
    }
        .flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)
}
