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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.accounts.view.AccountActionSheetRoot
import ua.com.radiokot.money.accounts.view.AccountActionSheetViewModel
import ua.com.radiokot.money.accounts.view.AccountsScreenRoot
import ua.com.radiokot.money.accounts.view.AccountsViewModel
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.categories.view.CategoriesScreenRoot
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.history.view.ActivityScreenRoot
import ua.com.radiokot.money.transfers.view.TransferSheetRoot
import ua.com.radiokot.money.transfers.view.TransferSheetViewModel
import ua.com.radiokot.money.uikit.TextButton

class HomeActivity : UserSessionScopeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        setContent {
            val navController = rememberNavController()

            UserSessionScope {
                Column {
                    NavHost(
                        navController = navController,
                        startDestination = AccountsScreenDestination,
                        enterTransition = { fadeIn(tween(150)) },
                        exitTransition = { fadeOut(tween(150)) },
                        modifier = Modifier
                            .weight(1f),
                    ) {
                        composable<AccountsScreenDestination> {
                            val viewModel: AccountsViewModel = koinViewModel()

                            LaunchedEffect(Unit) {
                                viewModel.events.collect { event ->
                                    when (event) {
                                        is AccountsViewModel.Event.OpenAccountActions -> {
                                            navController.navigate(
                                                route = AccountActionSheetDestination(
                                                    accountId = event.account.id,
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            AccountsScreenRoot(
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }

                        composable<CategoriesScreenDestination> {
                            CategoriesScreenRoot(
                                viewModel = koinViewModel(),
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }

                        composable<ActivityScreenDestination> {
                            ActivityScreenRoot(
                                viewModel = koinViewModel(),
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }

                        dialog<AccountActionSheetDestination> { entry ->
                            val accountId = entry.toRoute<AccountActionSheetDestination>()
                                .accountId
                            val viewModel: AccountActionSheetViewModel = koinViewModel()

                            LaunchedEffect(accountId) {
                                viewModel.setAccount(
                                    accountId = accountId,
                                )

                                launch {
                                    viewModel.events.collect { event ->
                                        when (event) {
                                            AccountActionSheetViewModel.Event.Close -> {
                                                navController.popBackStack<AccountActionSheetDestination>(
                                                    inclusive = true,
                                                )
                                            }

                                            is AccountActionSheetViewModel.Event.GoToTransfer -> {
                                                navController.popBackStack<AccountActionSheetDestination>(
                                                    inclusive = true,
                                                )
                                                navController.navigate(
                                                    route = TransferSheetDestination(
                                                        sourceId = event.source.id,
                                                        destinationId = event.destination.id,
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AccountActionSheetRoot(
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        dialog<TransferSheetDestination> { entry ->
                            val arguments = entry.toRoute<TransferSheetDestination>()
                            val viewModel = koinViewModel<TransferSheetViewModel>()

                            LaunchedEffect(arguments) {
                                viewModel.setSourceAndDestination(
                                    sourceId = arguments.sourceId,
                                    destinationId = arguments.destinationId,
                                )

                                launch {
                                    viewModel.events.collect { event ->
                                        when (event) {
                                            TransferSheetViewModel.Event.Close -> {
                                                navController.popBackStack<TransferSheetDestination>(
                                                    inclusive = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            TransferSheetRoot(
                                viewModel = viewModel,
                            )
                        }
                    }

                    BottomNavigation(
                        onAccountsClicked = {
                            navController.popBackStack()
                            navController.navigate(
                                route = AccountsScreenDestination,
                            )
                        },
                        onCategoriesClicked = {
                            navController.popBackStack()
                            navController.navigate(
                                route = CategoriesScreenDestination,
                            )
                        },
                        onActivityClicked = {
                            navController.popBackStack()
                            navController.navigate(
                                route = ActivityScreenDestination,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Serializable
private object AccountsScreenDestination

@Serializable
private object CategoriesScreenDestination

@Serializable
private object ActivityScreenDestination

@Serializable
private data class AccountActionSheetDestination(
    val accountId: String,
)

@Serializable
private data class TransferSheetDestination(
    val sourceAccountId: String? = null,
    val sourceCategoryId: String? = null,
    val sourceSubcategoryId: String? = null,
    val destinationAccountId: String? = null,
    val destinationCategoryId: String? = null,
    val destinationSubcategoryId: String? = null,
) {
    constructor(
        sourceId: TransferCounterpartyId,
        destinationId: TransferCounterpartyId,
    ) : this(
        sourceAccountId = (sourceId as? TransferCounterpartyId.Account)?.accountId,
        sourceCategoryId = (sourceId as? TransferCounterpartyId.Category)?.categoryId,
        sourceSubcategoryId = (sourceId as? TransferCounterpartyId.Category)?.subcategoryId,
        destinationAccountId = (destinationId as? TransferCounterpartyId.Account)?.accountId,
        destinationCategoryId = (destinationId as? TransferCounterpartyId.Category)?.categoryId,
        destinationSubcategoryId = (destinationId as? TransferCounterpartyId.Category)?.subcategoryId,
    )

    val sourceId: TransferCounterpartyId
        get() = when {
            sourceAccountId != null ->
                TransferCounterpartyId.Account(sourceAccountId)

            sourceCategoryId != null ->
                TransferCounterpartyId.Category(
                    categoryId = sourceCategoryId,
                    subcategoryId = sourceSubcategoryId,
                )

            else ->
                error("All the source IDs are missing")
        }

    val destinationId: TransferCounterpartyId
        get() = when {
            destinationAccountId != null ->
                TransferCounterpartyId.Account(destinationAccountId)

            destinationCategoryId != null ->
                TransferCounterpartyId.Category(
                    categoryId = destinationCategoryId,
                    subcategoryId = destinationSubcategoryId,
                )

            else ->
                error("All the destination IDs are missing")
        }
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

