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

package ua.com.radiokot.money.accounts

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.data.PowerSyncAccountRepository
import ua.com.radiokot.money.accounts.logic.AddAccountUseCase
import ua.com.radiokot.money.accounts.logic.EditAccountUseCase
import ua.com.radiokot.money.accounts.logic.GetVisibleAccountsUseCase
import ua.com.radiokot.money.accounts.logic.MoveAccountUseCase
import ua.com.radiokot.money.accounts.logic.PowerSyncAddAccountUseCase
import ua.com.radiokot.money.accounts.logic.PowerSyncEditAccountUseCase
import ua.com.radiokot.money.accounts.logic.PowerSyncMoveAccountUseCase
import ua.com.radiokot.money.accounts.logic.UpdateAccountBalanceUseCase
import ua.com.radiokot.money.accounts.view.AccountActionSheetViewModel
import ua.com.radiokot.money.accounts.view.AccountsViewModel
import ua.com.radiokot.money.accounts.view.EditAccountScreenViewModel
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.categories.categoriesModule
import ua.com.radiokot.money.currency.currencyModule
import ua.com.radiokot.money.powersync.powerSyncModule
import ua.com.radiokot.money.transfers.history.transfersHistoryModule

val accountsModule = module {
    includes(
        powerSyncModule,
        currencyModule,
        categoriesModule,
        transfersHistoryModule,
    )

    sessionScope {
        scoped {
            PowerSyncAccountRepository(
                colorSchemeRepository = get(),
                database = get(),
            )
        } bind AccountRepository::class

        factory {
            UpdateAccountBalanceUseCase(
                accountRepository = get(),
            )
        } bind UpdateAccountBalanceUseCase::class

        factory {
            PowerSyncMoveAccountUseCase(
                database = get(),
                accountRepository = get(),
                getVisibleAccountsUseCase = get(),
            )
        } bind MoveAccountUseCase::class

        factory {
            PowerSyncEditAccountUseCase(
                database = get(),
                accountRepository = get(),
                getVisibleAccountsUseCase = get(),
            )
        } bind EditAccountUseCase::class

        factory {
            PowerSyncAddAccountUseCase(
                database = get(),
                accountRepository = get(),
                getVisibleAccountsUseCase = get(),
            )
        } bind AddAccountUseCase::class

        scoped {
            GetVisibleAccountsUseCase(
                accountRepository = get(),
            )
        } bind GetVisibleAccountsUseCase::class

        viewModel {
            AccountsViewModel(
                currencyRepository = get(),
                currencyPreferences = get(),
                moveAccountUseCase = get(),
                getVisibleAccountsUseCase = get(),
            )
        } bind AccountsViewModel::class

        viewModel {
            AccountActionSheetViewModel(
                accountRepository = get(),
                updateAccountBalanceUseCase = get(),
            )
        } bind AccountActionSheetViewModel::class

        viewModel {
            EditAccountScreenViewModel(
                parameters = requireNotNull(getOrNull()) {
                    "EditAccountScreenViewModel.Parameters are required"
                },
                accountRepository = get(),
                currencyRepository = get(),
                currencyPreferences = get(),
                itemColorSchemeRepository = get(),
                editAccountUseCase = get(),
                addAccountUseCase = get(),
            )
        } bind EditAccountScreenViewModel::class
    }
}
