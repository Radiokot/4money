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

package ua.com.radiokot.money.currency.view

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.currency.data.Currency

@Serializable
data class CurrencySelectionScreenRoute(
    val selectedCurrencyCode: String,
)

fun NavGraphBuilder.currencySelectionScreen(
    onClose: () -> Unit,
    onDone: (currency: Currency) -> Unit,
) = composable<CurrencySelectionScreenRoute>(
    enterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Companion.Start,
        )
    },
    exitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Companion.End,
        )
    },
) { entry ->

    val route: CurrencySelectionScreenRoute = entry.toRoute()
    val viewModel: CurrencySelectionScreenViewModel = koinViewModel {
        parametersOf(
            CurrencySelectionScreenViewModel.Parameters(
                selectedCurrencyCode = route.selectedCurrencyCode,
            )
        )
    }

    LaunchedEffect(route) {
        viewModel.events.collect { event ->
            when (event) {
                CurrencySelectionScreenViewModel.Event.Close ->
                    onClose()

                is CurrencySelectionScreenViewModel.Event.Done ->
                    onDone(event.selectedCurrency)
            }
        }
    }

    CurrencySelectionScreenRoot(
        viewModel = viewModel,
    )
}
