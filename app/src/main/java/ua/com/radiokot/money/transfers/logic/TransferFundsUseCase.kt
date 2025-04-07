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

package ua.com.radiokot.money.transfers.logic

import kotlinx.datetime.LocalDate
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import java.math.BigInteger

interface TransferFundsUseCase {

    /**
     * Executes the transfer and adds it to the history.
     *
     * @param sourceId source of the funds
     * @param sourceAmount what amount is consumed with this transfer from the source.
     * If [sourceId] is an account, the amount is deducted from its balance
     * @param destinationId destination of the funds
     * @param destinationAmount what amount is produced with this transfer for the destination.
     * If [destinationId] is an account, the amount is added to its balance
     * @param memo a text note to add to the transfer
     * @param date a day to log the transfer
     */
    suspend operator fun invoke(
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        date: LocalDate,
    ): Result<Unit>
}
