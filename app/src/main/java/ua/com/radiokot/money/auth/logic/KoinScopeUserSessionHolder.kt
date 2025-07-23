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

package ua.com.radiokot.money.auth.logic

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.getKoin
import org.koin.androidx.scope.createActivityScope
import org.koin.androidx.scope.createFragmentScope
import org.koin.compose.LocalKoinApplication
import org.koin.compose.LocalKoinScope
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.scope.Scope
import org.koin.dsl.ScopeDSL
import org.koin.mp.KoinPlatform
import ua.com.radiokot.money.auth.data.UserSession

const val DI_SCOPE_SESSION = "user-session"

class KoinScopeUserSessionHolder(
    private val koin: Koin,
) : UserSessionHolder {

    override fun set(session: UserSession): Unit = with(koin) {
        closeExistingScope()

        createScope(
            scopeId = DI_SCOPE_SESSION,
            qualifier = _q<UserSession>(),
            source = session
        )
    }

    override fun clear() {
        closeExistingScope()
    }

    private fun closeExistingScope() = with(koin) {
        getScopeOrNull(DI_SCOPE_SESSION)?.close()
    }

    override val isSet: Boolean
        get() = with(koin) {
            return getScopeOrNull(DI_SCOPE_SESSION)?.isNotClosed() == true
        }
}

/**
 * @return Activity [Scope] linked to the [UserSession] scope, if it exists.
 */
fun ComponentActivity.createActivityScopeWithSession(): Scope =
    getKoin().getScopeOrNull(DI_SCOPE_SESSION)
        ?.apply { linkTo(createActivityScope()) }
        ?: createActivityScope()

/**
 * @return Fragment [Scope] linked to the [UserSession] scope, if it exists.
 */
fun Fragment.createFragmentScopeWithSession(): Scope =
    getKoin().getScopeOrNull(DI_SCOPE_SESSION)
        ?.apply { linkTo(createFragmentScope()) }
        ?: createFragmentScope()

fun Module.sessionScope(scopeSet: ScopeDSL.() -> Unit): Unit =
    scope<UserSession>(scopeSet)

/**
 * Runs the [content] in the [UserSession] scope, if it exists.
 */
@OptIn(KoinInternalApi::class)
@Composable
fun UserSessionScope(
    content: @Composable () -> Unit,
) {
    val koin = KoinPlatform.getKoin()
    CompositionLocalProvider(
        LocalKoinApplication.provides(koin),
        LocalKoinScope.provides(
            koin.getScopeOrNull(DI_SCOPE_SESSION)
                ?: koin.scopeRegistry.rootScope
        ),
        content = content,
    )
}
