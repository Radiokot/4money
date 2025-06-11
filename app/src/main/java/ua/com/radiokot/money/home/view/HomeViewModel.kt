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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.view.ActivityFilterViewModelDelegate
import ua.com.radiokot.money.transfers.history.view.HistoryStatsPeriodViewModel
import ua.com.radiokot.money.transfers.view.ViewTransferCounterparty
import ua.com.radiokot.money.map

class HomeViewModel(

) : ViewModel(),
    HistoryStatsPeriodViewModel,
    ActivityFilterViewModelDelegate {

    private val log by lazyLogger("HomeVM")
    private val _historyStatsPeriod: MutableStateFlow<HistoryPeriod> =
        MutableStateFlow(HistoryPeriod.Month())
    override val historyStatsPeriod = _historyStatsPeriod.asStateFlow()
    private val _activityFilterTransferCounterparties: MutableStateFlow<Set<TransferCounterparty>?> =
        MutableStateFlow(null)
    override val activityFilterTransferCounterparties: StateFlow<Set<TransferCounterparty>?> =
        _activityFilterTransferCounterparties.asStateFlow()
    override val activityFilterCounterparties: StateFlow<List<ViewTransferCounterparty>> =
        activityFilterTransferCounterparties
            .map(viewModelScope) { counterparties ->
                counterparties
                    ?.mapTo(mutableListOf(), ViewTransferCounterparty::fromCounterparty)
                    ?: emptyList()
            }

    override fun onNextHistoryStatsPeriodClicked() {
        log.debug {
            "onNextHistoryStatsPeriodClicked(): switching to next period"
        }

        _historyStatsPeriod.update { period ->
            checkNotNull(period.getNext()) {
                "Next period must be obtainable"
            }
        }
    }

    override fun onPreviousHistoryStatsPeriodClicked() {
        log.debug {
            "onPreviousHistoryStatsPeriodClicked(): switching to previous period"
        }

        _historyStatsPeriod.update { period ->
            checkNotNull(period.getPrevious()) {
                "Previous period must be obtainable"
            }
        }
    }

    override fun filterActivityByCounterparty(
        counterparty: TransferCounterparty,
    ) {
        val counterparties = setOf(counterparty)

        log.debug {
            "filterActivityByCounterparty(): setting counterparties: " +
                    "\nupdatedCounterparties=$counterparties"
        }

        _activityFilterTransferCounterparties.value = counterparties
    }

    override fun removeCounterpartyFromActivityFilter(
        counterparty: TransferCounterparty,
    ) {
        _activityFilterTransferCounterparties.update { counterparties ->
            counterparties
                ?.minus(counterparties)
                ?.takeIf(Set<*>::isNotEmpty)
                .also {
                    log.debug {
                        "removeCounterpartyFromActivityFilter(): removing counterparty: " +
                                "\ncounterparty=$counterparty" +
                                "\nupdatedCounterparties=$it"
                    }
                }
        }
    }
}
