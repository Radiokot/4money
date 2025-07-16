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
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.bottomSheet
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod

@Serializable
data class CategoryActionSheetRoute(
    val categoryId: String,
    val isIncome: Boolean,
    private val statsPeriodJson: String,
) {
    val statsPeriod: HistoryPeriod
        get() = Json.decodeFromString(statsPeriodJson)

    constructor(
        category: Category,
        statsPeriod: HistoryPeriod,
    ) : this(
        categoryId = category.id,
        isIncome = category.isIncome,
        statsPeriodJson = Json.encodeToString(statsPeriod),
    )
}

fun NavGraphBuilder.categoryActionSheet(
    onProceedToEdit: (Category) -> Unit,
    onProceedToFilteredActivity: (TransferCounterparty.Category) -> Unit,
) = bottomSheet<CategoryActionSheetRoute> { entry ->

    val route: CategoryActionSheetRoute = entry.toRoute()
    val viewModel: CategoryActionSheetViewModel = koinViewModel {
        parametersOf(
            CategoryActionSheetViewModel.Parameters(
                categoryId = route.categoryId,
                isIncome = route.isIncome,
                statsPeriod = route.statsPeriod,
            )
        )
    }

    LaunchedEffect(route) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoryActionSheetViewModel.Event.ProceedToEdit ->
                    onProceedToEdit(event.category)

                is CategoryActionSheetViewModel.Event.ProceedToFilteredActivity ->
                    onProceedToFilteredActivity(event.categoryCounterparty)
            }
        }
    }

    CategoryActionSheetRoot(
        viewModel = viewModel,
    )
}
