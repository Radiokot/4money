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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.compose.NavHost
import kotlinx.serialization.json.Json
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.colors.data.ItemLogoType
import ua.com.radiokot.money.colors.view.ItemLogoScreenRoute
import ua.com.radiokot.money.colors.view.itemLogoScreen
import ua.com.radiokot.money.currency.view.CurrencySelectionScreenRoute
import ua.com.radiokot.money.currency.view.currencySelectionScreen
import ua.com.radiokot.money.rememberMoneyAppNavController

class EditCategoryActivity : UserSessionScopeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            UserSessionScope {
                Content(
                    route = intent
                        .getStringExtra(EXTRA_ROUTE_JSON)!!
                        .let(Json::decodeFromString),
                    finishActivity = ::finish,
                )
            }
        }
    }


    companion object {
        private const val EXTRA_ROUTE_JSON = "route-json"

        fun getBundle(
            route: EditCategoryScreenRoute,
        ) = Bundle().apply {
            putString(
                EXTRA_ROUTE_JSON,
                Json.encodeToString(route),
            )
        }
    }
}

@Composable
private fun Content(
    route: EditCategoryScreenRoute,
    finishActivity: () -> Unit,
) {
    val navController = rememberMoneyAppNavController()
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    NavHost(
        navController = navController,
        startDestination = route,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
        modifier = Modifier
            .fillMaxSize(),
    ) {

        editCategoryScreen(
            onProceedToLogoCustomization = {
                    currentTitle,
                    currentColorScheme,
                    icon,
                ->
                softwareKeyboardController?.hide()
                navController.navigate(
                    ItemLogoScreenRoute(
                        logoType = ItemLogoType.Category,
                        itemTitle = currentTitle,
                        initialColorSchemeName = currentColorScheme.name,
                        initialIconName = icon?.name,
                    ),
                )
            },
            onProceedToCurrencySelection = { currentCurrency ->
                softwareKeyboardController?.hide()
                navController.navigate(
                    CurrencySelectionScreenRoute(
                        selectedCurrencyCode = currentCurrency.code,
                    ),
                )
            },
            onProceedToSubcategoryEdit = { subcategoryToUpdate, colorScheme ->
                softwareKeyboardController?.hide()
                navController.navigate(
                    EditSubcategoryScreenRoute(
                        subcategoryToUpdate = subcategoryToUpdate,
                        colorSchemeName = colorScheme.name,
                    )
                )
            },
            onClose = finishActivity,
            onDone = finishActivity,
        )

        itemLogoScreen(
            onClose = navController::navigateUp,
            onDone = { colorScheme, icon ->
                navController.navigateUp()
                EditCategoryScreenRoute.setSelectedColorScheme(
                    selectedColorScheme = colorScheme,
                    navController = navController,
                )
                EditCategoryScreenRoute.setSelectedIcon(
                    selectedIcon = icon,
                    navController = navController,
                )
            },
        )

        currencySelectionScreen(
            onClose = navController::navigateUp,
            onDone = { currency ->
                navController.navigateUp()
                EditCategoryScreenRoute.setSelectedCurrency(
                    selectedCurrency = currency,
                    navController = navController,
                )
            }
        )

        editSubcategoryScreen(
            onClose = navController::navigateUp,
            onDone = { subcategoryToUpdate ->
                navController.navigateUp()
                EditCategoryScreenRoute.setSubcategoryToUpdate(
                    subcategoryToUpdate = subcategoryToUpdate,
                    navController = navController,
                )
            }
        )
    }
}
