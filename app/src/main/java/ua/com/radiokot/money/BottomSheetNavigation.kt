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

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.navigation.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Navigator.Name("bottom-sheet")
class BottomSheetNavigator: Navigator<BottomSheetNavigator.Destination>() {

    val backStack: StateFlow<List<NavBackStackEntry>>
        get() =
            if (isAttached)
                state.backStack
            else
                MutableStateFlow(emptyList())

    fun dismiss() {
        backStack.value.forEach {
            popBackStack(it, false)
        }
    }

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.forEach { entry -> state.push(entry) }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
        // When popping, the incoming dialog is marked transitioning to hold it in
        // STARTED. With pop complete, we can remove it from transition so it can move to RESUMED.
        val popIndex = state.transitionsInProgress.value.indexOf(popUpTo)
        // do not mark complete for entries up to and including popUpTo
        state.transitionsInProgress.value.forEachIndexed { index, entry ->
            if (index > popIndex) onTransitionComplete(entry)
        }
    }

    fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }

    override fun createDestination() = Destination(
        navigator = this,
        content = {},
    )

    @NavDestination.ClassType(Composable::class)
    class Destination(
        navigator: Navigator<out NavDestination>,
        internal val content: @Composable (NavBackStackEntry) -> Unit,
    ) : NavDestination(navigator), FloatingWindow

    class DestinationBuilder(
        navigator: Navigator<out Destination>,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        private val content: @Composable (NavBackStackEntry) -> Unit,
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
    noinline content: @Composable (NavBackStackEntry) -> Unit,
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
