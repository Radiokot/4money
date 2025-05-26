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
import ua.com.radiokot.money.transfers.data.Transfer

interface TransferHistoryRepository {

    /**
     * @return list of records within the period in reverse chronological order,
     * additionally limited by [cursor] and [limit].
     *
     * @param counterpartyIds if set, only transfers related to those counterparties are returned,
     * including ones within subcategories of given categories.
     */
    suspend fun getTransferHistoryPage(
        cursor: TransferHistoryPage.Cursor?,
        limit: Int,
        withinPeriod: HistoryPeriod,
        counterpartyIds: Set<String>?,
    ): TransferHistoryPage

    fun getTransferHistoryPagingSource(
        withinPeriod: HistoryPeriod,
        counterpartyIds: Set<String>?,
    ): PagingSource<TransferHistoryPage.Cursor, Transfer>

    suspend fun getTransfer(transferId: String): Transfer
}
