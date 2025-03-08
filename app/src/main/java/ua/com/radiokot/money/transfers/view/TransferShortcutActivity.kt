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

package ua.com.radiokot.money.transfers.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.collectLatest
import ua.com.radiokot.money.MoneyAppModalBottomSheetLayout
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.rememberMoneyAppNavController
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId

class TransferShortcutActivity : UserSessionScopeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        setContent {
            UserSessionScope {
                TransferShortcutScreen(
                    finishActivity = ::finish,
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TransferShortcutScreen(
    finishActivity: () -> Unit,
) {
    val navController = rememberMoneyAppNavController()

    LaunchedEffect(navController) {
        navController.currentBackStack.collectLatest { backStack ->
            if (backStack.isEmpty()) {
                finishActivity()
            }
        }
    }

    var selectedSourceCounterpartyId: TransferCounterpartyId? by remember {
        mutableStateOf(null)
    }

    NavHost(
        navController = navController,
        startDestination = TransferCounterpartySelectionSheetRoute(
            isIncognito = true,
            isForSource = true,
            alreadySelectedCounterpartyId = null,
        ),
        modifier = Modifier
            .fillMaxSize()
    ) {

        transferCounterpartySelectionSheet(
            onSelected = { counterparty ->
                if (selectedSourceCounterpartyId == null) {
                    selectedSourceCounterpartyId = counterparty.id

                    navController.navigate(
                        route = TransferCounterpartySelectionSheetRoute(
                            isIncognito = true,
                            isForSource = false,
                            alreadySelectedCounterpartyId = selectedSourceCounterpartyId!!,
                        ),
                        navOptions = navOptions {
                            popUpTo<TransferCounterpartySelectionSheetRoute> {
                                inclusive = true
                            }
                        },
                    )
                } else {
                    navController.navigate(
                        route = TransferSheetRoute(
                            sourceId = selectedSourceCounterpartyId!!,
                            destinationId = counterparty.id,
                        ),
                        navOptions = navOptions {
                            popUpTo<TransferCounterpartySelectionSheetRoute> {
                                inclusive = true
                            }
                        },
                    )
                }
            }
        )

        transferSheet(
            onTransferDone = navController::popBackStack,
        )
    }

    MoneyAppModalBottomSheetLayout(
       moneyAppNavController = navController,
    )
}
