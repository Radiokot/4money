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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import ua.com.radiokot.money.MoneyAppModalBottomSheetHost
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
                    action = when (intent.action) {
                        "ua.com.radiokot.money.actions.ADD_INCOME" ->
                            Action.AddIncome

                        "ua.com.radiokot.money.actions.ADD_EXPENSE" ->
                            Action.AddExpense

                        else ->
                            Action.AddOperation
                    },
                    finishActivity = ::finish,
                )
            }
        }
    }
}

private enum class Action {
    AddOperation,
    AddIncome,
    AddExpense,
}

@SuppressLint("RestrictedApi")
@Composable
private fun TransferShortcutScreen(
    action: Action,
    finishActivity: () -> Unit,
) {
    val navController = rememberMoneyAppNavController()
    val transfersNavigatorFactory = koinInject<TransfersNavigator.Factory>()
    val transfersNavigator = remember(transfersNavigatorFactory, navController) {
        transfersNavigatorFactory.create(
            isIncognito = true,
            navController = navController,
        )
    }

    LaunchedEffect(navController) {
        navController.currentBackStack.collectLatest { backStack ->
            if (backStack.isEmpty()) {
                finishActivity()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = TransferCounterpartySelectionSheetRoute(
            isIncognito = true,
            isForSource = when (action) {
                Action.AddOperation -> null
                Action.AddIncome -> true
                Action.AddExpense -> false
            },
            showAccounts = action == Action.AddOperation,
            alreadySelectedCounterpartyId = null,
        ),
        modifier = Modifier
            .fillMaxSize()
    ) {
        transferCounterpartySelectionSheet(
            onSelected = transfersNavigator::proceedToTransfer,
        )

        transferSheet(
            onProceedToTransferCounterpartySelection = {
                    alreadySelectedCounterpartyId: TransferCounterpartyId,
                    selectSource: Boolean,
                    showCategories: Boolean,
                    showAccounts: Boolean,
                ->
                navController.navigate(
                    route = TransferCounterpartySelectionSheetRoute(
                        isForSource = selectSource,
                        alreadySelectedCounterpartyId = alreadySelectedCounterpartyId,
                        showCategories = showCategories,
                        showAccounts = showAccounts,
                        isIncognito = true,
                    )
                )
            },
            onTransferDone = finishActivity,
        )
    }

    MoneyAppModalBottomSheetHost(
        moneyAppNavController = navController,
    )
}
