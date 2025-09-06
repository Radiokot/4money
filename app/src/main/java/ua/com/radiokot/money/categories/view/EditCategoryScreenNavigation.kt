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
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.SubcategoryToUpdate
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.currency.data.Currency


private const val SAVED_STATE_KEY_SELECTED_COLOR_SCHEME = "selected-color-scheme"
private const val SAVED_STATE_KEY_SELECTED_ICON = "selected-icon"
private const val SAVED_STATE_KEY_SELECTED_CURRENCY = "selected-currency"
private const val SAVED_STATE_KEY_SUBCATEGORY_TO_UPDATE = "subcategory-to-update"

@Serializable
data class EditCategoryScreenRoute(
    val categoryToEditId: String?,
    val isIncome: Boolean,
) {

    constructor(categoryToEdit: Category) : this(
        categoryToEditId = categoryToEdit.id,
        isIncome = categoryToEdit.isIncome,
    )

    companion object {

        fun setSelectedColorScheme(
            selectedColorScheme: ItemColorScheme,
            navController: NavController,
        ) = navController
            .currentBackStackEntry
            ?.savedStateHandle
            ?.set(SAVED_STATE_KEY_SELECTED_COLOR_SCHEME, selectedColorScheme)

        fun setSelectedIcon(
            selectedIcon: ItemIcon?,
            navController: NavController,
        ) = navController
            .currentBackStackEntry
            ?.savedStateHandle
            ?.set(SAVED_STATE_KEY_SELECTED_ICON, selectedIcon to Unit)

        fun setSelectedCurrency(
            selectedCurrency: Currency,
            navController: NavController,
        ) = navController
            .currentBackStackEntry
            ?.savedStateHandle
            ?.set(SAVED_STATE_KEY_SELECTED_CURRENCY, selectedCurrency)

        fun setSubcategoryToUpdate(
            subcategoryToUpdate: SubcategoryToUpdate,
            navController: NavController,
        ) = navController
            .currentBackStackEntry
            ?.savedStateHandle
            ?.set(SAVED_STATE_KEY_SUBCATEGORY_TO_UPDATE, subcategoryToUpdate)
    }
}

fun NavGraphBuilder.editCategoryScreen(
    onProceedToLogoCustomization: (
        currentTitle: String,
        currentColorScheme: ItemColorScheme,
        icon: ItemIcon?,
    ) -> Unit,
    onProceedToCurrencySelection: (currentCurrency: Currency) -> Unit,
    onProceedToSubcategoryEdit: (
        subcategoryToUpdate: SubcategoryToUpdate,
        colorScheme: ItemColorScheme,
    ) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) = composable<EditCategoryScreenRoute> { entry ->

    val route: EditCategoryScreenRoute = entry.toRoute()
    val viewModel: EditCategoryScreenViewModel = koinViewModel {
        parametersOf(
            EditCategoryScreenViewModel.Parameters(
                categoryToEditId = route.categoryToEditId,
                isIncome = route.isIncome,
            )
        )
    }

    LaunchedEffect(route) {
        viewModel.events.collect { event ->
            when (event) {

                is EditCategoryScreenViewModel.Event.ProceedToLogoCustomization ->
                    onProceedToLogoCustomization(
                        event.currentTitle,
                        event.currentColorScheme,
                        event.currentIcon,
                    )

                is EditCategoryScreenViewModel.Event.ProceedToCurrencySelection ->
                    onProceedToCurrencySelection(event.currentCurrency)

                is EditCategoryScreenViewModel.Event.ProceedToSubcategoryEdit ->
                    onProceedToSubcategoryEdit(
                        event.subcategoryToUpdate,
                        event.colorScheme,
                    )

                EditCategoryScreenViewModel.Event.Close ->
                    onClose()

                EditCategoryScreenViewModel.Event.Done ->
                    onDone()
            }
        }
    }

    LaunchedEffect(route) {
        entry.savedStateHandle
            .getStateFlow<ItemColorScheme?>(
                key = SAVED_STATE_KEY_SELECTED_COLOR_SCHEME,
                initialValue = null,
            )
            .filterNotNull()
            .collect(viewModel::onColorSchemeSelected)
    }

    LaunchedEffect(route) {
        entry.savedStateHandle
            .getStateFlow<Pair<ItemIcon?, Unit>?>(
                key = SAVED_STATE_KEY_SELECTED_ICON,
                initialValue = null,
            )
            .filterNotNull()
            .collect { (icon) ->
                viewModel.onIconSelected(icon)
            }
    }

    LaunchedEffect(route) {
        entry.savedStateHandle
            .getStateFlow<Currency?>(
                key = SAVED_STATE_KEY_SELECTED_CURRENCY,
                initialValue = null,
            )
            .filterNotNull()
            .collect(viewModel::onCurrencySelected)
    }

    LaunchedEffect(route) {
        entry.savedStateHandle
            .getStateFlow<SubcategoryToUpdate?>(
                key = SAVED_STATE_KEY_SUBCATEGORY_TO_UPDATE,
                initialValue = null,
            )
            .filterNotNull()
            .collect(viewModel::onSubcategoryEdited)
    }

    EditCategoryScreenRoot(
        viewModel = viewModel,
    )
}
