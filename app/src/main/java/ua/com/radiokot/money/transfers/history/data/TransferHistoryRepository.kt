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

package ua.com.radiokot.money.transfers.history.data

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId

interface TransferHistoryRepository {

    /**
     * A hot flow emitting updated/added transfers.
     */
    val updatedTransfers: Flow<Transfer>

    /**
     * A hot flow emitting IDs of deleted transfers.
     */
    val deletedTransferIds: Flow<String>

    /**
     * @return list of records within the period in reverse chronological order,
     * additionally limited by [pageBefore] (so all the records are older than it)
     * and [pageLimit] (so no more records than the limit are returned).
     *
     * @param sourceId if it is a category ID, then the result also includes transfers
     * from related subcategories
     * @param destinationId if it is a category ID, then the result also includes transfers
     * to related subcategories
     */
    suspend fun getTransferHistoryPage(
        pageBefore: Instant?,
        pageLimit: Int,
        period: HistoryPeriod,
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ): List<Transfer>

    fun getTransferHistoryPagingSource(
        period: HistoryPeriod,
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ): PagingSource<Instant, Transfer>

    suspend fun getTransfer(transferId: String): Transfer
}
