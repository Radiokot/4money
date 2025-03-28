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
import androidx.paging.PagingState
import com.powersync.db.Queries
import com.powersync.db.SqlCursor
import com.powersync.db.internal.PowerSyncTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.format.DateTimeComponents
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import java.math.BigInteger
import java.util.UUID

class PowerSyncTransferHistoryRepository(
    private val database: Queries,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : TransferHistoryRepository {

    private val log by lazyLogger("PowerSyncTransferHistoryRepo")

    override suspend fun getTransferHistoryPage(
        pageBefore: Instant?,
        pageLimit: Int,
        period: HistoryPeriod,
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ): List<Transfer> = withContext(Dispatchers.IO) {

        val endTimeExclusive =
            if (pageBefore == null)
                period.endTimeExclusive
            else
                minOf(pageBefore, period.endTimeExclusive)

        val records: List<TransferHistoryRecord> = when {
            sourceId == null && destinationId == null ->
                database.getAll(
                    sql = SELECT_FOR_ALL_SOURCES_AND_DESTINATIONS,
                    parameters = listOf(
                        period.startTimeInclusive.epochSeconds,
                        endTimeExclusive.epochSeconds,
                        pageLimit.toLong(),
                    ),
                    mapper = ::toTransferHistoryRecord,
                )

            sourceId != null && destinationId == null ->
                database.getAll(
                    sql = SELECT_FOR_SOURCE,
                    parameters = listOf(
                        period.startTimeInclusive.epochSeconds,
                        endTimeExclusive.epochSeconds,
                        sourceId.toString(),
                        sourceId.toString(),
                        pageLimit.toLong(),
                    ),
                    mapper = ::toTransferHistoryRecord,
                )

            sourceId == null && destinationId != null ->
                database.getAll(
                    sql = SELECT_FOR_DESTINATION,
                    parameters = listOf(
                        period.startTimeInclusive.epochSeconds,
                        endTimeExclusive.epochSeconds,
                        destinationId.toString(),
                        destinationId.toString(),
                        pageLimit.toLong(),
                    ),
                    mapper = ::toTransferHistoryRecord,
                )

            else ->
                throw IllegalArgumentException("Can only filter by either source or destination")
        }

        val counterpartyById = getCounterpartiesById()

        return@withContext records.map { record ->
            toTransfer(
                record = record,
                counterpartyById = counterpartyById,
            )
        }
    }

    override fun getTransferHistoryPagingSource(
        period: HistoryPeriod,
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ) = object : PagingSource<Instant, Transfer>() {

        override fun getRefreshKey(state: PagingState<Instant, Transfer>): Instant? {
            TODO("Invalidating is not yet supported")
        }

        override suspend fun load(params: LoadParams<Instant>): LoadResult<Instant, Transfer> =
            getTransferHistoryPage(
                pageBefore = params.key,
                pageLimit = params.loadSize,
                period = period,
                sourceId = sourceId,
                destinationId = destinationId,
            ).let { transfers ->
                LoadResult.Page(
                    data = transfers,
                    prevKey = null,
                    nextKey = transfers
                        .lastOrNull()
                        ?.time
                        ?.takeUnless { transfers.size < params.loadSize },
                )
            }
    }

    override suspend fun getTransfer(transferId: String): Transfer =
        database
            .get(
                sql = SELECT_BY_ID,
                parameters = listOf(
                    transferId,
                ),
                mapper = ::toTransferHistoryRecord
            )
            .let { transferHistoryRecord ->
                toTransfer(
                    record = transferHistoryRecord,
                    counterpartyById = getCounterpartiesById(),
                )
            }

    fun logTransfer(
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        time: Instant,
        transaction: PowerSyncTransaction,
    ) {
        val id = UUID.randomUUID().toString()

        // ISO-8601 datetime with T, without millis,
        // with explicitly specified UTC timezone (Z).
        // For example, 2025-02-22T08:37:23Z
        val timeString = Instant.fromEpochSeconds(time.epochSeconds).toString()

        log.debug {
            "logTransfer(): logging transfer:" +
                    "\nid=$id," +
                    "\ntime=$timeString," +
                    "\nsourceId=$sourceId," +
                    "\nsourceAmount=$sourceAmount," +
                    "\ndestinationId=$destinationId," +
                    "\ndestinationAmount=$destinationAmount," +
                    "\nmemo=$memo"
        }

        transaction.execute(
            sql = "INSERT INTO transfers " +
                    "(id, time, source_id, source_amount, destination_id, destination_amount, memo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
            parameters = listOf(
                id,
                timeString,
                sourceId.toString(),
                sourceAmount.toString(),
                destinationId.toString(),
                destinationAmount.toString(),
                memo,
            )
        )
    }

    fun deleteTransfer(
        transferId: String,
        transaction: PowerSyncTransaction,
    ) {
        log.debug {
            "deleteTransfer(): deleting:" +
                    "\ntransferId=$transferId"
        }

        transaction.execute(
            sql = "DELETE FROM transfers WHERE transfers.id = ?",
            parameters = listOf(
                transferId,
            )
        )
    }

    private suspend fun getCounterpartiesById(): Map<String, TransferCounterparty> {
        val subcategoriesByCategories = categoryRepository
            .getSubcategoriesByCategoriesFlow()
            .first()
        val accounts = accountRepository.getAccounts()

        return buildMap {
            subcategoriesByCategories.forEach { (category, subcategories) ->
                val categoryCounterparty = TransferCounterparty.Category(category)
                put(categoryCounterparty.id.toString(), categoryCounterparty)

                subcategories.forEach { subcategory ->
                    val subcategoryCounterparty = TransferCounterparty.Category(
                        category = category,
                        subcategory = subcategory,
                    )
                    put(subcategoryCounterparty.id.toString(), subcategoryCounterparty)
                }
            }
            accounts.forEach { account ->
                val accountCounterparty = TransferCounterparty.Account(account)
                put(accountCounterparty.id.toString(), accountCounterparty)
            }
        }
    }

    private fun toTransferHistoryRecord(sqlCursor: SqlCursor) = with(sqlCursor) {
        var column = 0

        TransferHistoryRecord(
            id = getString(column)!!,
            time = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
                .parse(
                    getString(++column)!!
                        // 🤦🏻
                        .replace(' ', 'T')
                )
                .toInstantUsingOffset(),
            sourceId = getString(++column)!!,
            sourceAmount = BigInteger(getString(++column)!!.trim()),
            destinationId = getString(++column)!!,
            destinationAmount = BigInteger(getString(++column)!!.trim()),
            memo = getString(++column)?.trim(),
        )
    }

    private fun toTransfer(
        record: TransferHistoryRecord,
        counterpartyById: Map<String, TransferCounterparty>,
    ): Transfer = Transfer(
        id = record.id,
        source = counterpartyById[record.sourceId]
            ?: error("Source ${record.sourceId} not found"),
        sourceAmount = record.sourceAmount,
        destination = counterpartyById[record.destinationId]
            ?: error("Destination ${record.sourceId} not found"),
        destinationAmount = record.destinationAmount,
        time = record.time,
        memo = record.memo,
    )
}

private const val SELECT_TRANSFERS =
    "SELECT transfers.id, transfers.time, " +
            "transfers.source_id, transfers.source_amount, " +
            "transfers.destination_id, transfers.destination_amount, transfers.memo, " +
            "unixepoch(transfers.time) AS unix_time " +
            "FROM transfers"

private const val UNIX_TIME_IN_PERIOD =
    "unix_time >= ? AND unix_time < ?"

private const val ORDER =
    "ORDER BY unix_time DESC, transfers.id ASC"

private const val SELECT_SUBCATEGORIES_IDS_FOR_CATEGORY =
    "SELECT categories.id FROM categories WHERE categories.parent_id = ?"

/**
 * Params:
 * 1. Period start time seconds inclusive
 * 2. Period end time seconds exclusive
 * 3. Limit
 */
private const val SELECT_FOR_ALL_SOURCES_AND_DESTINATIONS =
    "$SELECT_TRANSFERS " +
            "WHERE $UNIX_TIME_IN_PERIOD " +
            "$ORDER " +
            "LIMIT ?"

/**
 * Params:
 * 1. Period start time seconds inclusive
 * 2. Period end time seconds exclusive
 * 3. Source ID
 * 4. Source ID once again
 * 5. Limit
 */
private const val SELECT_FOR_SOURCE =
    "$SELECT_TRANSFERS " +
            "WHERE $UNIX_TIME_IN_PERIOD " +
            "AND (transfers.source_id = ? OR transfers.source_id in " +
            "($SELECT_SUBCATEGORIES_IDS_FOR_CATEGORY)) " +
            "$ORDER " +
            "LIMIT ?"

/**
 * Params:
 * 1. Period start time seconds inclusive
 * 2. Period end time seconds exclusive
 * 3. Destination ID
 * 4. Destination ID once again
 * 5. Limit
 */
private const val SELECT_FOR_DESTINATION =
    "$SELECT_TRANSFERS " +
            "WHERE $UNIX_TIME_IN_PERIOD " +
            "AND (transfers.destination_id = ? OR transfers.destination_id in " +
            "($SELECT_SUBCATEGORIES_IDS_FOR_CATEGORY)) " +
            "$ORDER " +
            "LIMIT ?"

/**
 * Params:
 * 1. Transfer ID
 */
private const val SELECT_BY_ID =
    "$SELECT_TRANSFERS WHERE transfers.id = ?"
