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

import kotlinx.datetime.Instant
import java.math.BigInteger

class TransferHistoryRecord(
    val id: String,
    val time: Instant,
    val sourceId: String,
    val sourceAmount: BigInteger,
    val destinationId: String,
    val destinationAmount: BigInteger,
    val memo: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransferHistoryRecord) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TransferHistoryRecord(id='$id')"
    }
}
