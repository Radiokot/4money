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

package ua.com.radiokot.money.syncerrors

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.money.auth.logic.sessionScope
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.powersync.powerSyncModule
import ua.com.radiokot.money.syncerrors.data.PowerSyncSyncErrorRepository
import ua.com.radiokot.money.syncerrors.data.SyncErrorRepository

val syncErrorsModule = module {

    includes(
        powerSyncModule,
    )

    sessionScope {

        scoped {
            PowerSyncSyncErrorRepository(
                database = get(),
            ).apply {

                val log = KotlinLogging.logger("SyncErrorsWarning")

                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch {
                    getErrorCountFlow()
                        .filter { it > 0 }
                        .distinctUntilChanged()
                        .collect { errorCount ->
                            log.warn {
                                "There are data upload errors on the server: $errorCount"
                            }
                        }
                }
            }
        } bind SyncErrorRepository::class
    }
}
