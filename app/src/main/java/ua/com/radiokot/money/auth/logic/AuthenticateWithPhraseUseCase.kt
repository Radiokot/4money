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

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import ua.com.radiokot.money.auth.data.SignatureAuthData
import ua.com.radiokot.money.auth.data.UserSession
import ua.com.radiokot.money.lazyLogger

/**
 * Authenticates using the seed phrase,
 * utilizing edge functions for signature auth.
 *
 * If no user exists with this phrase, it is created.
 *
 * On success, [supabaseClient] and [userSessionHolder] have the session.
 *
 * @see [Ed25519Auth]
 */
class AuthenticateWithPhraseUseCase(
    private val supabaseClient: SupabaseClient,
    private val userSessionHolder: UserSessionHolder,
) {

    private val log by lazyLogger("AuthenticateWithPhraseUC")

    /**
     * @param phraseSeed a 64 byte long seed derived from the seed phrase.
     */
    suspend operator fun invoke(
        phraseSeed: ByteArray,
    ): Result<Unit> = runCatching {

        val authChallenge: String = try {
            supabaseClient
                .functions
                .invoke(
                    function = "signature-auth-challenge",
                )
                .bodyAsText()
        } catch (e: Exception) {

            log.error(e) {
                "invoke(): failed to get the challenge"
            }

            throw e
        }

        val authData: SignatureAuthData = try {
            Ed25519Auth.authenticate(
                phraseSeed = phraseSeed,
                authChallenge = authChallenge,
            )
        } catch (e: Exception) {

            log.error(e) {
                "invoke(): failed to sign the challenge"
            }

            throw e
        }

        val supabaseSession: io.github.jan.supabase.auth.user.UserSession = try {
            supabaseClient
                .functions
                .invoke(
                    function = "signature-auth",
                    body = authData,
                )
                .body()
        } catch (e: Exception) {

            log.error(e) {
                "invoke(): failed to get the session:" +
                        "\nauthData=$authData"
            }

            throw e
        }

        supabaseClient.auth.importSession(
            session = supabaseSession,
            autoRefresh = true,
            source = SessionSource.External,
        )

        userSessionHolder.set(
            session = UserSession(
                supabaseUserSession = supabaseSession,
            )
        )
    }
}
