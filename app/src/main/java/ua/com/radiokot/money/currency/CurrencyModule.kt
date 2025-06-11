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

package ua.com.radiokot.money.currency

import android.content.Context
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyPreferencesOnPrefs
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.currency.data.PowerSyncCurrencyRepository
import ua.com.radiokot.money.currency.view.CurrencySelectionScreenViewModel
import ua.com.radiokot.money.powersync.powerSyncModule

val currencyModule = module {
    includes(
        powerSyncModule,
    )

    single {
        CurrencyPreferencesOnPrefs(
            sharedPreferences = androidApplication().getSharedPreferences(
                "currency",
                Context.MODE_PRIVATE,
            )
        )
    } bind CurrencyPreferences::class

    sessionScope {
        scoped {
            PowerSyncCurrencyRepository(
                database = get(),
            )
        } bind CurrencyRepository::class

        viewModel {
            CurrencySelectionScreenViewModel(
                parameters = requireNotNull(getOrNull()) {
                    "CurrencySelectionScreenViewModel.Parameters are required"
                },
                currencyRepository = get(),
            )
        } bind CurrencySelectionScreenViewModel::class
    }
}
