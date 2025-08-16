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

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.accounts.accountsModule
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.categories.categoriesModule
import ua.com.radiokot.money.powersync.powerSyncModule
import ua.com.radiokot.money.transfers.data.TransferPreferencesOnPrefs
import ua.com.radiokot.money.transfers.data.TransfersPreferences
import ua.com.radiokot.money.transfers.history.transfersHistoryModule
import ua.com.radiokot.money.transfers.logic.EditTransferUseCase
import ua.com.radiokot.money.transfers.logic.GetLastUsedAccountsByCategoryUseCase
import ua.com.radiokot.money.transfers.logic.PowerSyncEditTransferUseCase
import ua.com.radiokot.money.transfers.logic.PowerSyncRevertTransferUseCase
import ua.com.radiokot.money.transfers.logic.PowerSyncTransferFundsUseCase
import ua.com.radiokot.money.transfers.logic.RevertTransferUseCase
import ua.com.radiokot.money.transfers.logic.TransferFundsUseCase
import ua.com.radiokot.money.transfers.view.TransferCounterpartySelectionSheetViewModel
import ua.com.radiokot.money.transfers.view.TransferSheetViewModel
import ua.com.radiokot.money.transfers.view.TransfersNavigator

val transfersModule = module {
    includes(
        powerSyncModule,
        accountsModule,
        categoriesModule,
        transfersHistoryModule,
    )

    single {
        TransferPreferencesOnPrefs(
            preferences = androidContext().getSharedPreferences(
                "transfers",
                Context.MODE_PRIVATE,
            )
        )
    } bind TransfersPreferences::class

    sessionScope {
        factory {
            PowerSyncTransferFundsUseCase(
                accountRepository = get(),
                transferHistoryRepository = get(),
                database = get(),
                transfersPreferences = get(),
            )
        } bind TransferFundsUseCase::class

        factory {
            PowerSyncRevertTransferUseCase(
                accountRepository = get(),
                transferHistoryRepository = get(),
                database = get(),
            )
        } bind RevertTransferUseCase::class

        factory {
            PowerSyncEditTransferUseCase(
                accountRepository = get(),
                transferHistoryRepository = get(),
                database = get(),
            )
        } bind EditTransferUseCase::class

        factory {
            GetLastUsedAccountsByCategoryUseCase(
                accountRepository = get(),
                transfersPreferences = get(),
            )
        } bind GetLastUsedAccountsByCategoryUseCase::class

        scoped {
            TransfersNavigator.Factory { isIncognito, navController ->
                TransfersNavigator(
                    getLastUsedAccountsByCategoryUseCase = get(),
                    isIncognito = isIncognito,
                    navController = navController,
                )
            }
        } bind TransfersNavigator.Factory::class

        viewModel {
            TransferSheetViewModel(
                parameters = requireNotNull(getOrNull()) {
                    "TransferSheetViewModel.Parameters are required"
                },
                accountRepository = get(),
                categoryRepository = get(),
                transferFundsUseCase = get(),
                editTransferUseCase = get(),
            )
        } bind TransferSheetViewModel::class

        viewModel {
            TransferCounterpartySelectionSheetViewModel(
                categoryRepository = get(),
                getCategoriesWithAmountUseCase = get(),
                getVisibleAccountsUseCase = get(),
            )
        } bind TransferCounterpartySelectionSheetViewModel::class
    }
}
