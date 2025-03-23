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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.categories.data.Category

@Serializable
data class CategoriesScreenRoute(
    val isIncognito: Boolean,
)

fun NavGraphBuilder.categoriesScreen(
    onProceedToTransfer: (category: Category) -> Unit,
) = composable<CategoriesScreenRoute> { entry ->

    val isIncognito = entry.toRoute<CategoriesScreenRoute>()
        .isIncognito
    val viewModel = koinViewModel<CategoriesViewModel>()

    LaunchedEffect(isIncognito) {
        launch {
            viewModel.events.collect { event ->
                when (event) {
                    is CategoriesViewModel.Event.ProceedToTransfer -> {
                        onProceedToTransfer(event.category)
                    }
                }
            }
        }
    }

    CategoriesScreenRoot(
        viewModel = viewModel,
        modifier = Modifier
            .fillMaxSize()
    )
}
