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

import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials

/**
 * A [PowerSyncBackendConnector] decorator which skips empty row updates
 *
 * @see <a href="https://github.com/powersync-ja/powersync-kotlin/pull/144#issuecomment-2729125336">The discussion</a>
 */
class IgnoreEmptyUpdatesDecorator(
    private val delegate: PowerSyncBackendConnector,
) : PowerSyncBackendConnector() {

    override suspend fun fetchCredentials(): PowerSyncCredentials? =
        delegate.fetchCredentials()

    override suspend fun uploadData(database: PowerSyncDatabase) {
        database.execute(
            "DELETE FROM ps_crud " +
                    "WHERE data->>'op' = 'PATCH' " +
                    "AND NOT EXISTS (SELECT 1 from json_each(data->'data'))"
        )
        delegate.uploadData(database)
    }
}

fun PowerSyncBackendConnector.ignoreEmptyUpdates(): PowerSyncBackendConnector =
    IgnoreEmptyUpdatesDecorator(this)
