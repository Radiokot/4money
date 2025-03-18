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

package ua.com.radiokot.money.transfers

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.accounts.accountsModule
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.categories.categoriesModule
import ua.com.radiokot.money.powersync.powerSyncModule
import ua.com.radiokot.money.transfers.history.transfersHistoryModule
import ua.com.radiokot.money.transfers.logic.PowerSyncRevertTransferUseCase
import ua.com.radiokot.money.transfers.logic.PowerSyncTransferFundsUseCase
import ua.com.radiokot.money.transfers.logic.RevertTransferUseCase
import ua.com.radiokot.money.transfers.logic.TransferFundsUseCase
import ua.com.radiokot.money.transfers.view.TransferCounterpartySelectionSheetViewModel
import ua.com.radiokot.money.transfers.view.TransferSheetViewModel

val transfersModule = module {
    includes(
        powerSyncModule,
        accountsModule,
        categoriesModule,
        transfersHistoryModule,
    )

    sessionScope {
        factory {
            PowerSyncTransferFundsUseCase(
                database = get(),
            )
        } bind TransferFundsUseCase::class

        factory {
            PowerSyncRevertTransferUseCase(
                transferHistoryRepository = get(),
                database = get(),
            )
        } bind RevertTransferUseCase::class

        viewModel {
            TransferSheetViewModel(
                accountRepository = get(),
                categoryRepository = get(),
                transferFundsUseCase = get(),
            )
        } bind TransferSheetViewModel::class

        viewModel {
            TransferCounterpartySelectionSheetViewModel(
                accountRepository = get(),
                categoryRepository = get(),
                historyStatsRepository = get(),
            )
        } bind TransferCounterpartySelectionSheetViewModel::class
    }
}
