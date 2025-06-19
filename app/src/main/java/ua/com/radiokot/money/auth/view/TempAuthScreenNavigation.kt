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

package ua.com.radiokot.money.auth.view

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object TempAuthScreenRoute

fun NavGraphBuilder.tempAuth(
    onProceedToPhraseAuth: () -> Unit,
    onDone: () -> Unit,
) = composable<TempAuthScreenRoute> { entry ->

    val route: TempAuthScreenRoute = entry.toRoute()
    val viewModel: TempAuthScreenViewModel = koinViewModel()

    LaunchedEffect(route) {
        viewModel.events.collect { event ->
            when (event) {
                TempAuthScreenViewModel.Event.Done ->
                    onDone()

                TempAuthScreenViewModel.Event.ProceedToPhraseAuth ->
                    onProceedToPhraseAuth()

                is TempAuthScreenViewModel.Event.ShowAuthError -> {
                }
            }
        }
    }

    TempAuthScreenRoot(
        viewModel = viewModel,
    )
}
