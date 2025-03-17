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

import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.connector.supabase.SupabaseConnector
import com.powersync.db.Queries
import com.powersync.db.schema.Schema
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import ua.com.radiokot.money.BuildConfig
import ua.com.radiokot.money.auth.authModule

@OptIn(DelicateCoroutinesApi::class)
val powerSyncModule = module {
    includes(authModule)

    singleOf(::moneyAppPowerSyncSchema) bind Schema::class

    single {
        PowerSyncDatabase(
            factory = DatabaseDriverFactory(androidApplication()),
            schema = get(),
        ).apply {
            GlobalScope.launch {
                connect(
                    connector = SupabaseConnector(
                        supabaseClient = get(),
                        powerSyncEndpoint = BuildConfig.POWERSYNC_URL,
                    ).ignoreEmptyUpdates()
                )
            }
        }
    } binds arrayOf(
        PowerSyncDatabase::class,
        Queries::class,
    )
}
