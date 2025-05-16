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

package ua.com.radiokot.money

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.compose.rememberNavController
import com.composables.core.ModalBottomSheet
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.serializer

@Composable
fun rememberMoneyAppNavController(): NavHostController {
    val bottomSheetNavigator = remember {
        BottomSheetNavigator()
    }
    return rememberNavController(
        bottomSheetNavigator,
    )
}

@Composable
fun MoneyAppModalBottomSheetHost(
    moneyAppNavController: NavController,
) {
    val bottomSheetNavigator = moneyAppNavController
        .navigatorProvider
        .getNavigator(BottomSheetNavigator::class.java)

    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.Hidden,
        // Use normal speed once the IME padding issue is resolved
        // https://github.com/composablehorizons/compose-unstyled/issues/74
        animationSpec = tween(50),
    )

    LaunchedEffect(sheetState) {
        bottomSheetNavigator
            .backStack
            .map(List<*>::isNotEmpty)
            .distinctUntilChanged()
            .collectLatest { isBackStackNotEmpty ->
                if (isBackStackNotEmpty) {
                    sheetState.targetDetent = SheetDetent.FullyExpanded
                } else {
                    sheetState.targetDetent = SheetDetent.Hidden
                }
            }
    }

    ModalBottomSheet(
        state = sheetState,
        onDismiss = bottomSheetNavigator::dismiss,
    ) {
        val scrimFadeAnimationSpec: SpringSpec<Float> = remember {
            spring(
                stiffness = Spring.StiffnessMedium,
            )
        }

        Scrim(
            enter = fadeIn(scrimFadeAnimationSpec),
            exit = fadeOut(scrimFadeAnimationSpec),
        )

        val backStack by bottomSheetNavigator.backStack.collectAsState()

        BackHandler(enabled = backStack.size > 1) {
            backStack.last().also {
                bottomSheetNavigator.popBackStack(it, false)
                bottomSheetNavigator.onTransitionComplete(it)
            }
        }

        Sheet(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                    )
                )
                .imePadding()
        ) SheetContent@{
            val saveableStateHolder = rememberSaveableStateHolder()

            AnimatedContent(
                targetState = backStack.lastOrNull()
                    ?: return@SheetContent,
                contentAlignment = Alignment.BottomCenter,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(),
                        initialContentExit = ExitTransition.None,
                        sizeTransform = null,
                    )
                },
                label = "sheet-content-transition",
            ) { topBackStackEntry ->

                topBackStackEntry.LocalOwnersProvider(saveableStateHolder) {
                    (topBackStackEntry.destination as BottomSheetNavigator.Destination)
                        .content(topBackStackEntry)
                }

                DisposableEffect(topBackStackEntry) {
                    onDispose {
                        bottomSheetNavigator.onTransitionComplete(topBackStackEntry)
                    }
                }
            }
        }
    }
}

inline fun <reified RouteType> NavDestination.routeIs(): Boolean =
    route?.startsWith(serializer<RouteType>().descriptor.serialName) == true
