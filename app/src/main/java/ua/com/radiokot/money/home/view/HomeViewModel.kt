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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

class HomeViewModel: ViewModel() {

    private val log by lazyLogger("HomeVM")
    private val _period: MutableStateFlow<HistoryPeriod> = MutableStateFlow(HistoryPeriod.Month())
    val period = _period.asStateFlow()

    fun onNextPeriodClicked() {
        log.debug {
            "onNextPeriodClicked(): switching to next period"
        }

        _period.update { period ->
            checkNotNull(period.getNext()) {
                "Next period must be obtainable"
            }
        }
    }

    fun onPreviousPeriodClicked() {
        log.debug {
            "onPreviousPeriodClicked(): switching to previous period"
        }

        _period.update { period ->
            checkNotNull(period.getPrevious()) {
                "Previous period must be obtainable"
            }
        }
    }
}
