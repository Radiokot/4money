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

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.powersync.PowerSyncDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.scope.Scope
import ua.com.radiokot.money.auth.logic.DI_SCOPE_SESSION
import ua.com.radiokot.money.lazyLogger
import kotlin.time.Duration.Companion.minutes
import kotlin.time.measureTime

class BackgroundPowerSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params),
    KoinComponent {

    private val log by lazyLogger("BackgroundPowerSyncWorker")

    override suspend fun doWork(
    ): Result = try {

        withTimeout(1.minutes) {

            val sessionScope: Scope? = getKoin().getScopeOrNull(DI_SCOPE_SESSION)
            if (sessionScope == null) {
                log.debug {
                    "doWork(): skipping, there is no session"
                }

                return@withTimeout Result.success()
            }

            val syncDuration = measureTime {

                log.info {
                    "Background sync started"
                }

                log.debug {
                    "doWork(): refreshing Supabase auth session"
                }

                sessionScope
                    .get<SupabaseClient>()
                    .auth
                    .refreshCurrentSession()

                log.debug {
                    "doWork(): waiting for PowerSync full sync"
                }

                // Sync kicks in here.
                sessionScope
                    .get<PowerSyncDatabase>()
                    .currentStatus
                    .asFlow()
                    .first { it.hasSynced == true }
            }

            log.info {
                "Background sync done in $syncDuration"
            }

            log.debug {
                "doWork(): synchronized:" +
                        "\nduration=$syncDuration"
            }

            return@withTimeout Result.success()
        }
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()

        log.error(e) {
            "doWork(): failed"
        }

        Result.retry()
    }
}
