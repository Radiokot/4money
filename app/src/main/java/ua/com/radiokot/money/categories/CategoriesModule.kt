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

package ua.com.radiokot.money.categories

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.PowerSyncCategoryRepository
import ua.com.radiokot.money.categories.logic.AddCategoryUseCase
import ua.com.radiokot.money.categories.logic.EditCategoryUseCase
import ua.com.radiokot.money.categories.logic.GetCategoryStatsUseCase
import ua.com.radiokot.money.categories.logic.PowerSyncAddCategoryUseCase
import ua.com.radiokot.money.categories.logic.PowerSyncEditCategoryUseCase
import ua.com.radiokot.money.categories.view.CategoriesScreenViewModel
import ua.com.radiokot.money.categories.view.EditCategoryScreenViewModel
import ua.com.radiokot.money.categories.view.EditSubcategoryScreenViewModel
import ua.com.radiokot.money.colors.colorsModule
import ua.com.radiokot.money.currency.currencyModule
import ua.com.radiokot.money.transfers.history.transfersHistoryModule

val categoriesModule = module {
    includes(
        transfersHistoryModule,
        currencyModule,
        colorsModule,
    )

    sessionScope {
        scoped {
            PowerSyncCategoryRepository(
                colorSchemeRepository = get(),
                database = get(),
            )
        } bind CategoryRepository::class

        factory {
            GetCategoryStatsUseCase(
                categoryRepository = get(),
                historyStatsRepository = get(),
            )
        } bind GetCategoryStatsUseCase::class

        viewModel { parameters ->
            CategoriesScreenViewModel(
                historyStatsPeriodViewModel = checkNotNull(parameters.getOrNull()) {
                    "HistoryStatsPeriodViewModel must be provided through the parameters " +
                            "to share the same instance"
                },
                getCategoryStatsUseCase = get(),
                currencyRepository = get(),
                currencyPreferences = get(),
            )
        } bind CategoriesScreenViewModel::class

        factory {
            PowerSyncEditCategoryUseCase(
                categoryRepository = get(),
                database = get(),
            )
        } bind EditCategoryUseCase::class

        factory {
            PowerSyncAddCategoryUseCase(
                database = get(),
                categoryRepository = get(),
            )
        } bind AddCategoryUseCase::class

        viewModel {
            EditCategoryScreenViewModel(
                parameters = checkNotNull(getOrNull()) {
                    "EditCategoryScreenViewModel.Parameters are required"
                },
                categoryRepository = get(),
                currencyPreferences = get(),
                currencyRepository = get(),
                itemColorSchemeRepository = get(),
                editCategoryUseCase = get(),
                addCategoryUseCase = get(),
            )
        } bind EditCategoryScreenViewModel::class

        viewModel {
            EditSubcategoryScreenViewModel(
                parameters = checkNotNull(getOrNull()) {
                    "EditSubcategoryScreenViewModel.Parameters are required"
                },
                itemColorSchemeRepository = get(),
            )
        } bind EditSubcategoryScreenViewModel::class
    }
}
