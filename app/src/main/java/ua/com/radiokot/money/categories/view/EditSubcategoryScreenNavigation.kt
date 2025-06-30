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

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.categories.data.SubcategoryToUpdate

@Serializable
data class EditSubcategoryScreenRoute(
    private val subcategoryToUpdateJson: String,
    val colorSchemeName: String,
) {

    val subcategoryToUpdate: SubcategoryToUpdate
        get() = Json.decodeFromString(subcategoryToUpdateJson)

    constructor(
        subcategoryToUpdate: SubcategoryToUpdate,
        colorSchemeName: String,
    ) : this(
        subcategoryToUpdateJson = Json.encodeToString(subcategoryToUpdate),
        colorSchemeName = colorSchemeName,
    )
}

fun NavGraphBuilder.editSubcategoryScreen(
    onClose: () -> Unit,
    onDone: (subcategoryToUpdate: SubcategoryToUpdate) -> Unit,
) = composable<EditSubcategoryScreenRoute> { entry ->

    val route: EditSubcategoryScreenRoute = entry.toRoute()
    val viewModel: EditSubcategoryScreenViewModel = koinViewModel {
        parametersOf(
            EditSubcategoryScreenViewModel.Parameters(
                subcategoryToUpdate = route.subcategoryToUpdate,
                colorSchemeName = route.colorSchemeName,
            )
        )
    }

    LaunchedEffect(route) {
        viewModel.events.collect { event ->
            when (event) {

                EditSubcategoryScreenViewModel.Event.Close ->
                    onClose()

                is EditSubcategoryScreenViewModel.Event.Done ->
                    onDone(event.subcategoryToUpdate)
            }
        }
    }

    EditSubcategoryScreenRoot(
        viewModel = viewModel,
    )
}
