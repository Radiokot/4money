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

package ua.com.radiokot.money.home.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import ua.com.radiokot.money.MoneyAppModalBottomSheetLayout
import ua.com.radiokot.money.accounts.view.AccountActionSheetRoute
import ua.com.radiokot.money.accounts.view.AccountsScreenRoute
import ua.com.radiokot.money.accounts.view.accountActionSheet
import ua.com.radiokot.money.accounts.view.accountsScreen
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.categories.view.CategoriesScreenRoute
import ua.com.radiokot.money.categories.view.categoriesScreen
import ua.com.radiokot.money.rememberMoneyAppNavController
import ua.com.radiokot.money.transfers.history.view.ActivityScreenRoute
import ua.com.radiokot.money.transfers.history.view.activityScreen
import ua.com.radiokot.money.transfers.view.TransferSheetRoute
import ua.com.radiokot.money.transfers.view.transferSheet
import ua.com.radiokot.money.uikit.TextButton

class HomeActivity : UserSessionScopeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            UserSessionScope {
                HomeScreen()
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun HomeScreen(

) {
    val navController = rememberMoneyAppNavController()

    Column {
        NavHost(
            navController = navController,
            startDestination = AccountsScreenRoute(
                isIncognito = false,
            ),
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
            modifier = Modifier
                .weight(1f)
                .safeDrawingPadding(),
        ) {

            accountsScreen(
                onAccountClicked = { account ->
                    navController.navigate(
                        route = AccountActionSheetRoute(
                            accountId = account.id,
                        )
                    )
                }
            )

            categoriesScreen(
                onCategoryClicked = {},
            )

            activityScreen()

            accountActionSheet(
                onBalanceUpdated = navController::navigateUp,
                onTransferCounterpartiesSelected = { source, destination ->
                    navController.navigate(
                        route = TransferSheetRoute(
                            sourceId = source.id,
                            destinationId = destination.id,
                        ),
                        navOptions = navOptions {
                            popUpTo<AccountActionSheetRoute> {
                                inclusive = true
                            }
                        },
                    )
                }
            )

            transferSheet(
                onTransferDone = navController::navigateUp,
            )
        }

        BottomNavigation(
            onAccountsClicked = {
                navController.popBackStack()
                navController.navigate(
                    route = AccountsScreenRoute(
                        isIncognito = false,
                    ),
                )
            },
            onCategoriesClicked = {
                navController.popBackStack()
                navController.navigate(
                    route = CategoriesScreenRoute(
                        isIncognito = false,
                    ),
                )
            },
            onActivityClicked = {
                navController.popBackStack()
                navController.navigate(
                    route = ActivityScreenRoute,
                )
            },
        )
    }

    MoneyAppModalBottomSheetLayout(
        moneyAppNavController = navController,
    )
}

@Composable
private fun BottomNavigation(
    onAccountsClicked: () -> Unit,
    onCategoriesClicked: () -> Unit,
    onActivityClicked: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(
        16.dp,
        Alignment.CenterHorizontally
    ),
    modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xfff0edf1))
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(
            vertical = 12.dp,
        )
) {
    val clickableAccountsModifier = remember {
        Modifier.clickable { onAccountsClicked() }
    }

    TextButton(
        text = "👛 Accounts",
        modifier = Modifier
            .then(clickableAccountsModifier),
    )

    val clickableCategoriesModifier = remember {
        Modifier.clickable { onCategoriesClicked() }
    }

    TextButton(
        text = "📊 Categories",
        modifier = Modifier
            .then(clickableCategoriesModifier)
    )

    val clickableActivityModifier = remember {
        Modifier.clickable { onActivityClicked() }
    }

    TextButton(
        text = "📃 Activity",
        modifier = Modifier
            .then(clickableActivityModifier)
    )
}

