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

import android.app.Application
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.slf4j.impl.HandroidLoggerAdapter
import ua.com.radiokot.money.auth.authModule
import ua.com.radiokot.money.auth.data.UserSession
import ua.com.radiokot.money.auth.logic.UserSessionHolder
import ua.com.radiokot.money.home.homeModule
import ua.com.radiokot.money.powersync.BackgroundPowerSyncWorker
import java.util.concurrent.TimeUnit

class MoneyApp : Application() {
    private val log by lazyLogger("App")

    override fun onCreate() {
        super.onCreate()

        initLogging()

        startKoin {
            androidLogger()
            androidContext(this@MoneyApp)

            modules(
                authModule,
                homeModule,
            )
        }

        initSessionHolder()
        initBackgroundSync()
    }

    private fun initLogging() {
        HandroidLoggerAdapter.APP_NAME = "4MN"
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT
    }

    private fun initSessionHolder() {

        val supabaseSession = runBlocking {
            get<SupabaseClient>().run {
                auth.loadFromStorage(
                    autoRefresh = false,
                )
                auth.currentSessionOrNull()
            }
        }

        if (supabaseSession == null) {
            log.debug {
                "initSessionHolder(): no stored session found"
            }
            return
        }

        log.debug {
            "initSessionHolder(): setting the loaded session:" +
                    "\nuserId=${supabaseSession.user?.id}"
        }

        get<UserSessionHolder>().set(
            UserSession(
                supabaseUserSession = supabaseSession,
            )
        )
    }

    private fun initBackgroundSync() {

        val workRequest = PeriodicWorkRequestBuilder<BackgroundPowerSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkRequest(
                        networkRequest = NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build(),
                        networkType = NetworkType.CONNECTED,
                    )
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "BackgroundSync",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
    }
}
