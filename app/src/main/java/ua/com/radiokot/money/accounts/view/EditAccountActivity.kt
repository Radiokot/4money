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

package ua.com.radiokot.money.accounts.view

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
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.currency.view.CurrencySelectionScreenRoute
import ua.com.radiokot.money.currency.view.currencySelectionScreen
import ua.com.radiokot.money.rememberMoneyAppNavController

class EditAccountActivity : UserSessionScopeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            UserSessionScope {
                Content()
            }
        }
    }
}

@Composable
private fun Content(

) {
    val navController = rememberMoneyAppNavController()
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    NavHost(
        navController = navController,
        startDestination = EditAccountScreenRoute(
            accountToEditId = null,
        ),
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
        modifier = Modifier
            .fillMaxSize(),
    ) {

        editAccountScreen(
            onProceedToAccountTypeSelection = { currentType ->
                // TODO
            },
            onProceedToLogoCustomization = { currentTitle, currentColorScheme ->
                softwareKeyboardController?.hide()
                navController.navigate(
                    AccountLogoScreenRoute(
                        accountTitle = currentTitle,
                        initialColorSchemeName = currentColorScheme.name,
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
        )

        accountLogoScreen(
            onClose = {
                navController.popBackStack()
            },
            onDone = { colorScheme ->
                navController.popBackStack()
                EditAccountScreenRoute.setSelectedColorScheme(
                    selectedColorScheme = colorScheme,
                    navController = navController,
                )
            },
        )

        currencySelectionScreen(
            onClose = {
                navController.popBackStack()
            },
            onDone = { currency ->
                navController.popBackStack()
                EditAccountScreenRoute.setSelectedCurrency(
                    selectedCurrency = currency,
                    navController = navController,
                )
            }
        )
    }
}
