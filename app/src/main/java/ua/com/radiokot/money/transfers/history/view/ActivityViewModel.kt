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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.isSameDayAs
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import ua.com.radiokot.money.transfers.view.ViewTransferListItem
import java.math.BigInteger

class ActivityViewModel(
    private val transferHistoryRepository: TransferHistoryRepository,
) : ViewModel() {
    private val localTimeZone = TimeZone.currentSystemDefault()

    val itemList: StateFlow<List<ViewTransferListItem>> =
        transferHistoryRepository
            .getTransferHistoryPageFlow(
                offsetExclusive = null,
                limit = 50,
                period = HistoryPeriod.Month(),
                source = null,
                destination = null,
            )
            .map { transfers ->
                val today = Clock.System.now().toLocalDateTime(localTimeZone).date
                val yesterday = today.minus(1, DateTimeUnit.DAY)

                buildList {
                    transfers.forEachIndexed { i, transfer ->
                        val transferLocalDate = transfer.getLocalDateAt(localTimeZone)

                        if (i == 0 || !transferLocalDate.isSameDayAs(
                                transfers[i - 1].getLocalDateAt(
                                    localTimeZone
                                )
                            )
                        ) {
                            add(
                                ViewTransferListItem.Header(
                                    localDate = transferLocalDate,
                                    amount = ViewAmount(
                                        value = BigInteger.ZERO,
                                        currency = ViewCurrency(
                                            symbol = "$",
                                            precision = 2,
                                        )
                                    ),
                                    dayType = when {
                                        transferLocalDate.isSameDayAs(today) ->
                                            ViewTransferListItem.Header.DayType.Today

                                        transferLocalDate.isSameDayAs(yesterday) ->
                                            ViewTransferListItem.Header.DayType.Yesterday

                                        else ->
                                            ViewTransferListItem.Header.DayType.DayOfWeek
                                    }
                                )
                            )
                        }

                        add(ViewTransferListItem.Transfer.fromTransfer(transfer))
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = emptyList()
            )
}
