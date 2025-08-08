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

package ua.com.radiokot.money.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.BuildConfig
import ua.com.radiokot.money.R
import ua.com.radiokot.money.auth.logic.KoinScopeUserSessionHolder
import ua.com.radiokot.money.auth.logic.SignInWithCredentialsUseCase
import ua.com.radiokot.money.auth.logic.SignInWithPhraseUseCase
import ua.com.radiokot.money.auth.logic.SignOutUseCase
import ua.com.radiokot.money.auth.logic.UserSessionHolder
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.auth.view.PhraseAuthScreenViewModel
import ua.com.radiokot.money.auth.view.TempAuthScreenViewModel

val authModule = module {

    single {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Functions)
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = androidContext().getString(R.string.uri_scheme)
                host = androidContext().getString(R.string.auth_uri_host)

                autoLoadFromStorage = true
                autoSaveToStorage = true
                alwaysAutoRefresh = true
                // Enabling this makes the session unavailable
                // when the app is in background.
                enableLifecycleCallbacks = false
            }
        }
    } bind SupabaseClient::class

    single {
        KoinScopeUserSessionHolder(
            koin = getKoin(),
        )
    } bind UserSessionHolder::class

    factory {
        SignInWithCredentialsUseCase(
            supabaseClient = get(),
            userSessionHolder = get(),
        )
    } bind SignInWithCredentialsUseCase::class

    factory {
        SignInWithPhraseUseCase(
            supabaseClient = get(),
            userSessionHolder = get(),
        )
    } bind SignInWithPhraseUseCase::class

    viewModel {
        TempAuthScreenViewModel(
            signInUseCase = get(),
        )
    } bind TempAuthScreenViewModel::class

    viewModel {
        PhraseAuthScreenViewModel(
            signInWithPhraseUseCase = get(),
        )
    } bind PhraseAuthScreenViewModel::class

    sessionScope {

        factory {
            SignOutUseCase(
                supabaseClient = get(),
                userSessionHolder = get(),
                database = get(),
            )
        } bind SignOutUseCase::class
    }
}
