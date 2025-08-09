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

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.get
import com.composables.core.BottomSheetScope
import com.composables.core.ModalBottomSheet
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.composeunstyled.LocalModalWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Navigator.Name("bottom-sheet")
class BottomSheetNavigator : Navigator<BottomSheetNavigator.Destination>() {

    val backStack: StateFlow<List<NavBackStackEntry>>
        get() =
            if (isAttached)
                state.backStack
            else
                MutableStateFlow(emptyList())

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.forEach(state::push)
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        // The top item will undergo the transition,
        // while others can be popped immediately.
        state.prepareForTransition(backStack.value.last())

        state.pop(popUpTo, savedState)
    }

    override fun createDestination(): Destination {
        throw NotImplementedError("Use .bottomSheet<>{ } destination builder instead")
    }

    fun onDismiss() {
        backStack
            .value
            .firstOrNull()
            ?.also { popBackStack(it, false) }
    }

    fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }

    @NavDestination.ClassType(Composable::class)
    class Destination(
        navigator: Navigator<out NavDestination>,
        internal val content: @Composable BottomSheetScope.(NavBackStackEntry) -> Unit,
    ) : NavDestination(navigator), FloatingWindow

    class DestinationBuilder(
        navigator: Navigator<out Destination>,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        private val content: @Composable BottomSheetScope.(NavBackStackEntry) -> Unit,
    ) : NavDestinationBuilder<Destination>(navigator, route, typeMap) {

        override fun instantiateDestination() = Destination(
            navigator = navigator,
            content = content,
        )
    }
}

inline fun <reified T : Any> NavGraphBuilder.bottomSheet(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable BottomSheetScope.(NavBackStackEntry) -> Unit,
) {
    destination(
        BottomSheetNavigator.DestinationBuilder(
            provider[BottomSheetNavigator::class],
            T::class,
            typeMap,
            content
        )
            .apply { deepLinks.forEach { deepLink -> deepLink(deepLink) } }
    )
}


@Composable
fun MoneyAppModalBottomSheetHost(
    moneyAppNavController: NavController,
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val bottomSheetNavigator = moneyAppNavController
        .navigatorProvider
        .getNavigator(BottomSheetNavigator::class.java)

    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.Hidden,
        animationSpec = tween(150),
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
        onDismiss = bottomSheetNavigator::onDismiss,
    ) {
        val modalWindow: Window = LocalModalWindow.current

        LaunchedEffect(modalWindow) {
            WindowInsetsControllerCompat(modalWindow, modalWindow.decorView)
                .isAppearanceLightNavigationBars = true
            // This removes the default navigation bar scrim.
            modalWindow.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        val scrimFadeAnimationSpec: SpringSpec<Float> = remember {
            spring(
                stiffness = Spring.StiffnessMedium,
            )
        }

        Scrim(
            enter = fadeIn(scrimFadeAnimationSpec),
            exit = fadeOut(scrimFadeAnimationSpec),
        )

        Sheet(
            imeAware = true,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                    )
                )
        ) SheetContent@{

            val backStack by bottomSheetNavigator.backStack.collectAsState()
            val topBackStackEntry by remember {
                derivedStateOf { backStack.lastOrNull() }
            }

            AnimatedContent(
                targetState = topBackStackEntry
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
            ) { shownBackStackEntry ->

                shownBackStackEntry.LocalOwnersProvider(saveableStateHolder) {
                    (shownBackStackEntry.destination as BottomSheetNavigator.Destination)
                        .content(this@SheetContent, shownBackStackEntry)
                }

                val isBackHandlerEnabled by remember {
                    derivedStateOf { backStack.size > 1 }
                }

                BackHandler(
                    enabled = isBackHandlerEnabled,
                ) {
                    bottomSheetNavigator.popBackStack(shownBackStackEntry, false)
                }

                DisposableEffect(shownBackStackEntry) {
                    onDispose {
                        bottomSheetNavigator.onTransitionComplete(shownBackStackEntry)
                    }
                }
            }
        }
    }
}
