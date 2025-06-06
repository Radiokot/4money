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

package ua.com.radiokot.money.transfers.data

import kotlinx.datetime.LocalDateTime
import java.math.BigInteger
import java.util.UUID

class Transfer(
    val source: TransferCounterparty,
    val sourceAmount: BigInteger,
    val destination: TransferCounterparty,
    val destinationAmount: BigInteger,
    val dateTime: LocalDateTime,
    val memo: String?,
    val id: String = UUID.randomUUID().toString(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transfer) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Transfer(id='$id')"
    }
}
