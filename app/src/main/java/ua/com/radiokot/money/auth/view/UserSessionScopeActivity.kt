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

package ua.com.radiokot.money.auth.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.scope.AndroidScopeComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.money.auth.data.UserSession
import ua.com.radiokot.money.auth.logic.createActivityScopeWithSession
import ua.com.radiokot.money.lazyLogger

abstract class UserSessionScopeActivity :
    AppCompatActivity(),
    AndroidScopeComponent {

    override val scope: Scope = createActivityScopeWithSession()
    private val log by lazyLogger("UserSessionScopeActivity")

    protected open val hasSession: Boolean
        get() = scope.getOrNull<UserSession>() != null

    /**
     * @return true if there is no session and switching to the auth screen has been called.
     */
    open fun goToAuthIfNoSession(): Boolean =
        if (!hasSession) {
            log.debug {
                "goToAuthIfNoSession(): going"
            }
            goToAuth()
            true
        } else {
            false
        }

    /**
     * @return true if there is no session and finishing has been called.
     */
    open fun finishIfNoSession(): Boolean =
        if (!hasSession) {
            log.debug {
                "finishIfNoSession(): finishing"
            }
            finishAffinity()
            true
        } else {
            false
        }

    /**
     * Goes to the [AuthActivity] finishing this activity and all the underlying.
     */
    protected open fun goToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finishAffinity()
    }
}
