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

import com.powersync.PowerSyncDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ua.com.radiokot.money.lock.logic.DisableAppLockUseCase
import kotlin.time.Duration.Companion.seconds

/**
 * On success, [supabaseClient] is signed out,
 * [userSessionHolder] is cleared,
 * [database] is cleared and closed,
 * app lock is disabled via [disableAppLockUseCase].
 */
@OptIn(DelicateCoroutinesApi::class)
class SignOutUseCase(
    private val supabaseClient: SupabaseClient,
    private val userSessionHolder: UserSessionHolder,
    private val database: PowerSyncDatabase,
    private val disableAppLockUseCase: DisableAppLockUseCase,
) {

    suspend operator fun invoke(

    ): Result<Unit> = runCatching {

        GlobalScope.launch {
            withTimeout(30.seconds) {
                supabaseClient.auth.signOut()
            }
        }

        database.disconnectAndClear()

        userSessionHolder.clear()

        disableAppLockUseCase()
    }
}
