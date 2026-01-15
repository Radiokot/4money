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

package ua.com.radiokot.money.powersync

import co.touchlab.kermit.Logger
import com.powersync.DatabaseDriverFactory
import com.powersync.ExperimentalPowerSyncAPI
import com.powersync.PowerSyncDatabase
import com.powersync.db.Queries
import com.powersync.db.schema.Schema
import com.powersync.sync.SyncOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.BuildConfig
import ua.com.radiokot.money.auth.authModule
import ua.com.radiokot.money.auth.logic.sessionScope

@OptIn(
    DelicateCoroutinesApi::class,
    ExperimentalPowerSyncAPI::class,
)
val powerSyncModule = module {
    includes(authModule)

    singleOf(DbSchema::getPowerSyncSchema) bind Schema::class

    sessionScope {

        scoped {
            PowerSyncDatabase(
                factory = DatabaseDriverFactory(androidApplication()),
                schema = get(),
                logger = Logger.withTag("PowerSync"),
            ).apply {
                GlobalScope.launch {
                    connect(
                        connector = AtomicCrudSupabaseConnector(
                            supabaseClient = get(),
                            powerSyncEndpoint = BuildConfig.POWERSYNC_URL,
                        ),
                        options = SyncOptions(
                            userAgent = "4Money/${BuildConfig.VERSION_NAME}",
                        ),
                        appMetadata = mapOf(
                            "v" to BuildConfig.VERSION_NAME,
                            "debug" to BuildConfig.DEBUG.toString(),
                        ),
                    )
                }
            }
        } bind Queries::class
    }
}
