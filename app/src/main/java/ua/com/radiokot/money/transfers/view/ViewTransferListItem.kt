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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import java.math.BigInteger
import kotlin.random.Random

@Immutable
sealed interface ViewTransferListItem {

    val itemType: String
    val key: Any

    class Header(
        val localDate: LocalDate,
        val dayType: DayType,
        val amount: ViewAmount,
        override val key: Any = localDate.toString(),
    ) : ViewTransferListItem {

        override val itemType: String = "header"

        enum class DayType {
            Today,
            Yesterday,
            DayOfWeek,
            ;
        }

        companion object {
            fun fromTransferTime(
                time: Instant,
                localTimeZone: TimeZone,
                amount: ViewAmount,
            ): Header {
                val now = Clock.System.now().toLocalDateTime(localTimeZone)
                val localDate = time.toLocalDateTime(localTimeZone).date
                val dayType = when {
                    localDate.year == now.year
                            && localDate.dayOfYear == now.dayOfYear ->
                        DayType.Today

                    localDate.year == now.year
                            && localDate.dayOfYear == now.dayOfYear - 1 ->
                        DayType.Yesterday

                    else ->
                        DayType.DayOfWeek
                }

                return Header(
                    localDate = localDate,
                    dayType = dayType,
                    amount = amount,
                )
            }
        }
    }

    class Transfer(
        val primaryCounterparty: ViewTransferCounterparty,
        val primaryAmount: BigInteger,
        val secondaryCounterparty: ViewTransferCounterparty,
        val secondaryAmount: BigInteger,
        val type: Type,
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
            if (source != other.source) return false

            return true
        }

        override fun hashCode(): Int {
            var result = primaryCounterparty.hashCode()
            result = 31 * result + primaryAmount.hashCode()
            result = 31 * result + secondaryCounterparty.hashCode()
            result = 31 * result + secondaryAmount.hashCode()
            result = 31 * result + (source?.hashCode() ?: 0)
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
                        source = transfer,
                    )
                }
        }
    }
}
