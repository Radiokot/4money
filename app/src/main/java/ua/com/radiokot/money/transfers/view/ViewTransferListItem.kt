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

package ua.com.radiokot.money.transfers.view

import androidx.compose.runtime.Immutable
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import java.math.BigInteger
import kotlin.random.Random

@Immutable
sealed interface ViewTransferListItem {

    val itemType: String
    val key: Any

    class Header(
        val date: ViewDate,
        override val key: Any = date.hashCode(),
    ) : ViewTransferListItem {

        override val itemType: String = "header"
    }

    class Transfer(
        val primaryCounterparty: ViewTransferCounterparty,
        val primaryAmount: BigInteger,
        val secondaryCounterparty: ViewTransferCounterparty,
        val secondaryAmount: BigInteger,
        val type: Type,
        val memo: String?,
        /**
         * Although it is not shown in the list item,
         * the same transfer with updated date must be treated as a different one.
         */
        val dateTime: Any,
        val source: ua.com.radiokot.money.transfers.data.Transfer?,
        override val key: Any = source?.hashCode() ?: Random.nextInt(),
    ) : ViewTransferListItem {

        override val itemType: String = "transfer"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Transfer) return false

            if (primaryCounterparty != other.primaryCounterparty) return false
            if (primaryAmount != other.primaryAmount) return false
            if (secondaryCounterparty != other.secondaryCounterparty) return false
            if (secondaryAmount != other.secondaryAmount) return false
            if (type != other.type) return false
            if (memo != other.memo) return false
            if (dateTime != other.dateTime) return false
            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            var result = primaryCounterparty.hashCode()
            result = 31 * result + primaryAmount.hashCode()
            result = 31 * result + secondaryCounterparty.hashCode()
            result = 31 * result + secondaryAmount.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + (memo?.hashCode() ?: 0)
            result = 31 * result + dateTime.hashCode()
            result = 31 * result + key.hashCode()
            return result
        }

        enum class Type {
            Income,
            Expense,
            Other,
            ;
        }

        companion object {
            fun fromTransfer(
                transfer: ua.com.radiokot.money.transfers.data.Transfer,
            ): Transfer =
                if (transfer.source is TransferCounterparty.Category && transfer.source.category.isIncome) {
                    Transfer(
                        primaryCounterparty = ViewTransferCounterparty.fromCounterparty(transfer.source),
                        primaryAmount = transfer.sourceAmount,
                        secondaryCounterparty = ViewTransferCounterparty.fromCounterparty(transfer.destination),
                        secondaryAmount = transfer.destinationAmount,
                        type = Type.Income,
                        memo = transfer.memo,
                        dateTime = transfer.dateTime,
                        source = transfer,
                    )
                } else {
                    Transfer(
                        primaryCounterparty = ViewTransferCounterparty.fromCounterparty(transfer.destination),
                        primaryAmount = transfer.destinationAmount,
                        secondaryCounterparty = ViewTransferCounterparty.fromCounterparty(transfer.source),
                        secondaryAmount = transfer.sourceAmount,
                        type = if (transfer.destination is TransferCounterparty.Category
                            && !transfer.destination.category.isIncome
                        )
                            Type.Expense
                        else
                            Type.Other,
                        memo = transfer.memo,
                        dateTime = transfer.dateTime,
                        source = transfer,
                    )
                }
        }
    }
}
