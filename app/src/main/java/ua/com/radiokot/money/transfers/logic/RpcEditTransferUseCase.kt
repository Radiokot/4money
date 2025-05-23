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

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.history.data.PowerSyncTransferHistoryRepository
import java.math.BigInteger

class RpcEditTransferUseCase(
    private val supabaseClient: SupabaseClient,
    private val transferHistoryRepository: PowerSyncTransferHistoryRepository,
) : EditTransferUseCase {

    override suspend fun invoke(
        transferId: String,
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        date: LocalDate,
        exactTime: Instant?,
    ): Result<Unit> = runCatching {

        val time = exactTime
            ?: transferHistoryRepository.getTimeForTransfer(
                date = date,
            )

        supabaseClient.postgrest.rpc(
            function = "transfer_edit",
            parameters = TransferInput(
                id = transferId,
                sourceId = sourceId.toString(),
                sourceAmount = sourceAmount.toString(),
                destinationId = destinationId.toString(),
                destinationAmount = destinationAmount.toString(),
                memo = memo,
                time = Instant.fromEpochSeconds(time.epochSeconds).toString(),
            )
        )
    }

    @Serializable
    @Suppress("unused")
    private class TransferInput(
        @SerialName("id")
        val id: String,
        @SerialName("source_id")
        val sourceId: String,
        @SerialName("source_amount")
        val sourceAmount: String,
        @SerialName("destination_id")
        val destinationId: String,
        @SerialName("destination_amount")
        val destinationAmount: String,
        @SerialName("memo")
        val memo: String?,
        @SerialName("time")
        val time: String,
    )
}
