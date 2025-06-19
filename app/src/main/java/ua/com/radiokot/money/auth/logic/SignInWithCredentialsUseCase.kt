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

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import ua.com.radiokot.money.auth.data.UserSession
import ua.com.radiokot.money.lazyLogger

class SignInWithCredentialsUseCase(
    private val supabaseClient: SupabaseClient,
    private val userSessionHolder: UserSessionHolder,
) {
    private val log by lazyLogger("SignInWithCredentialsUC")

    suspend fun start(
        login: String,
        password: String,
    ): Result<StartResult> = runCatching {
        log.debug {
            "start(): starting:" +
                    "\nlogin=$login," +
                    "\npassword=$password"
        }

        supabaseClient.auth.signInWith(Email) {
            this.email = login
            this.password = password
        }

        val settledSession = supabaseClient.auth.currentSessionOrNull()

        if (settledSession != null) {
            log.debug {
                "start(): done, the session is settled already"
            }

            userSessionHolder.set(
                UserSession(
                    supabaseUserSession = settledSession,
                )
            )

            return@runCatching StartResult.Done
        } else {
            log.debug {
                "start(): started, but needs finishing"
            }

            return@runCatching StartResult.Started
        }
    }

    suspend fun finish(
        resultIntent: Intent,
    ): Result<Boolean> = runCatching {
        val code = resultIntent.data?.getQueryParameter("code")
        if (code == null) {
            log.warn {
                "finish(): URI has no code"
            }

            return@runCatching false
        }

        userSessionHolder.set(
            UserSession(
                supabaseUserSession = supabaseClient.auth.exchangeCodeForSession(
                    code = code,
                    saveSession = true,
                )
            )
        )

        log.warn {
            "finish(): done"
        }

        return@runCatching true
    }

    sealed interface StartResult {
        /**
         * Auth has been completed immediately,
         * no need to finish with redirect URI.
         */
        object Done : StartResult

        /**
         * Auth requires interaction with a web page,
         * need to call [finish] once the redirect URI is received.
         */
        object Started : StartResult
    }
}
