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

package ua.com.radiokot.money.transfers.history

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.powersync.powerSyncModule
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository
import ua.com.radiokot.money.transfers.history.data.PowerSyncHistoryStatsRepository
import ua.com.radiokot.money.transfers.history.data.PowerSyncTransferHistoryRepository
import ua.com.radiokot.money.transfers.history.data.TransferHistoryRepository
import ua.com.radiokot.money.transfers.history.view.ActivityViewModel

val transfersHistoryModule = module {
    includes(
        powerSyncModule,
    )

    sessionScope {
        scoped {
            PowerSyncHistoryStatsRepository(
                database = get(),
            )
        } bind HistoryStatsRepository::class

        scoped {
            PowerSyncTransferHistoryRepository(
                database = get(),
                accountRepository = get(),
                categoryRepository = get(),
            )
        } bind TransferHistoryRepository::class

        viewModel { parameters ->
            ActivityViewModel(
                historyStatsPeriodViewModel = checkNotNull(parameters.getOrNull()) {
                    "HistoryStatsPeriodViewModel must be provided through the parameters " +
                            "to share the same instance"
                },
                activityFilterViewModelDelegate = checkNotNull(parameters.getOrNull()){
                    "ActivityFilterViewModelDelegate must be provided through the parameters " +
                            "to share the same instance"
                },
                transferHistoryRepository = get(),
                revertTransferUseCase = get(),
            )
        } bind ActivityViewModel::class
    }
}
