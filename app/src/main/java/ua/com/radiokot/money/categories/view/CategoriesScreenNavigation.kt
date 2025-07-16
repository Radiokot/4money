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

package ua.com.radiokot.money.categories.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.home.view.HomeViewModel
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

@Serializable
data class CategoriesScreenRoute(
    val isIncognito: Boolean,
)

fun NavGraphBuilder.categoriesScreen(
    homeViewModel: HomeViewModel,
    onProceedToTransfer: (category: Category) -> Unit,
    onProceedToCategoryActions: (
        category: Category,
        statsPeriod: HistoryPeriod,
    ) -> Unit,
    onProceedToCategoryAdd: (isIncome: Boolean) -> Unit,
) = composable<CategoriesScreenRoute> { entry ->

    val isIncognito = entry.toRoute<CategoriesScreenRoute>()
        .isIncognito
    val viewModel = koinViewModel<CategoriesScreenViewModel> {
        parametersOf(
            homeViewModel,
        )
    }

    LaunchedEffect(isIncognito) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoriesScreenViewModel.Event.ProceedToTransfer ->
                    onProceedToTransfer(event.category)

                is CategoriesScreenViewModel.Event.ProceedToCategoryActions ->
                    onProceedToCategoryActions(
                        event.category,
                        event.statsPeriod,
                    )

                is CategoriesScreenViewModel.Event.ProceedToCategoryAdd ->
                    onProceedToCategoryAdd(event.isIncome)
            }
        }
    }

    CategoriesScreenRoot(
        viewModel = viewModel,
        modifier = Modifier
            .fillMaxSize()
    )
}
