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
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import com.powersync.db.Queries
import com.powersync.db.SqlCursor
import com.powersync.db.internal.PowerSyncTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.plus
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class PowerSyncTransferHistoryRepository(
    private val database: Queries,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : TransferHistoryRepository {

    private val log by lazyLogger("PowerSyncTransferHistoryRepo")
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val createdPagingSources: MutableList<WeakReference<PagingSource<*, *>>> =
        mutableListOf()

    override suspend fun getTransferHistoryPage(
        cursor: TransferHistoryPage.Cursor?,
        limit: Int,
        withinPeriod: HistoryPeriod,
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ): TransferHistoryPage = withContext(Dispatchers.IO) {

        val timeCondition = buildString {
            append("$UNIX_TIME >= ${withinPeriod.startTimeInclusive.epochSeconds} ")
            append("AND $UNIX_TIME < ${withinPeriod.endTimeExclusive.epochSeconds} ")
            when {
                cursor?.isBefore == true ->
                    append("AND $UNIX_TIME < ${cursor.timeExclusive.epochSeconds} ")

                cursor?.isAfter == true ->
                    append("AND $UNIX_TIME > ${cursor.timeExclusive.epochSeconds} ")
            }
        }

        val orderCondition = buildString {
            append("ORDER BY $UNIX_TIME ")
            when {
                cursor?.isAfter == true ->
                    append("ASC ")

                else ->
                    append("DESC ")
            }
        }

        val limitCondition = "LIMIT $limit "

        val records: List<TransferHistoryRecord> = when {
            sourceId == null && destinationId == null ->
                database.getAll(
                    sql = buildString {
                        append(SELECT_TRANSFERS)
                        append(" WHERE ")
                        append(timeCondition)
                        append(orderCondition)
                        append(limitCondition)
                    },
                    parameters = listOf(),
                    mapper = ::toTransferHistoryRecord,
                )

            sourceId != null && destinationId == null ->
                database.getAll(
                    sql = buildString {
                        append(SELECT_TRANSFERS_FOR_SOURCE)
                        append(" AND ")
                        append(timeCondition)
                        append(orderCondition)
                        append(limitCondition)
                    },
                    parameters = listOf(
                        sourceId.toString(),
                        sourceId.toString(),
                    ),
                    mapper = ::toTransferHistoryRecord,
                )

            sourceId == null && destinationId != null ->
                database.getAll(
                    sql = buildString {
                        append(SELECT_TRANSFERS_FOR_DESTINATION)
                        append(" AND ")
                        append(timeCondition)
                        append(orderCondition)
                        append(limitCondition)
                    },
                    parameters = listOf(
                        destinationId.toString(),
                        destinationId.toString(),
                        limit.toLong(),
                    ),
                    mapper = ::toTransferHistoryRecord,
                )

            else ->
                throw IllegalArgumentException("Can only filter by either source or destination")
        }

        val counterpartyById = getCounterpartiesById()
        val transfers = records
            .run {
                if (cursor?.isAfter == true)
                    sortedByDescending(TransferHistoryRecord::time)
                else
                    this
            }
            .map { record ->
                toTransfer(
                    record = record,
                    counterpartyById = counterpartyById,
                )
            }

        return@withContext TransferHistoryPage(
            data = transfers,
            nextPageCursor = transfers
                .lastOrNull()
                ?.time
                ?.takeUnless { transfers.size < limit }
                ?.let { oldestTransferTime ->
                    TransferHistoryPage.Cursor(
                        timeExclusive = oldestTransferTime,
                        isBefore = true,
                    )
                },
            previousPageCursor = transfers
                .firstOrNull()
                ?.takeUnless { cursor == null || cursor.isAfter && transfers.size < limit }
                ?.time
                ?.let { newestTransferTime ->
                    TransferHistoryPage.Cursor(
                        timeExclusive = newestTransferTime,
                        isBefore = false,
                    )
                }
        )
    }

    override fun getTransferHistoryPagingSource(
        period: HistoryPeriod,
        sourceId: TransferCounterpartyId?,
        destinationId: TransferCounterpartyId?,
    ) = object : PagingSource<TransferHistoryPage.Cursor, Transfer>() {

        override fun getRefreshKey(state: PagingState<TransferHistoryPage.Cursor, Transfer>): TransferHistoryPage.Cursor? =
            state
                .anchorPosition
                ?.let(state::closestPageToPosition)
                ?.data
                ?.firstOrNull()
                ?.time
                ?.let { newestTransferTime ->
                    TransferHistoryPage.Cursor(
                        timeExclusive = newestTransferTime.plus(1.seconds),
                        isBefore = true,
                    )
                }

        override suspend fun load(params: LoadParams<TransferHistoryPage.Cursor>): LoadResult<TransferHistoryPage.Cursor, Transfer> =
            getTransferHistoryPage(
                cursor = params.key,
                limit = params.loadSize,
                withinPeriod = period,
                sourceId = sourceId,
                destinationId = destinationId,
            ).toLoadResultPage()
    }.also { createdPagingSources += WeakReference(it) }

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

    fun addOrUpdateTransfer(
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        time: Instant,
        metadata: String,
        transaction: PowerSyncTransaction,
        transferId: String = UUID.randomUUID().toString(),
    ) {
        val timeString = time.toDbTimeString()

        log.debug {
            "addOrUpdateTransfer(): executing:" +
                    "\nid=$transferId," +
                    "\ntime=$timeString," +
                    "\nsourceId=$sourceId," +
                    "\nsourceAmount=$sourceAmount," +
                    "\ndestinationId=$destinationId," +
                    "\ndestinationAmount=$destinationAmount," +
                    "\nmetadata=$metadata," +
                    "\nmemo=$memo"
        }

        transaction.execute(
            sql = INSERT_OR_REPLACE_TRANSFER,
            parameters = listOf(
                transferId,
                timeString,
                sourceId.toString(),
                sourceAmount.toString(),
                destinationId.toString(),
                destinationAmount.toString(),
                memo,
                metadata,
            )
        )

        invalidatePagingSourcesWhen {
            database
                .watch(
                    sql = SELECT_BY_ID,
                    parameters = listOf(transferId),
                    mapper = ::toTransferHistoryRecord,
                )
                .first()
        }
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

        invalidatePagingSourcesWhen {
            database
                .watch(
                    sql = "SELECT transfers.id FROM transfers WHERE transfers.id = ?",
                    parameters = listOf(transferId),
                    mapper = { },
                )
                .first(List<*>::isEmpty)
        }
    }

    private fun invalidatePagingSourcesWhen(
        whenWhat: suspend () -> Any,
    ) = coroutineScope.launch {
        try {
            withTimeout(1.seconds) {
                whenWhat()

                val invalidatedCount = createdPagingSources.sumOf { weakReference ->
                    weakReference.get()?.invalidate()?.let { 1L } ?: 0L
                }

                log.debug {
                    "invalidatePagingSourcesWhen(): invalidated paging sources:" +
                            "\ncount=$invalidatedCount"
                }
            }
        } catch (e: TimeoutCancellationException) {
            log.error {
                "invalidatePagingSourcesWhen(): condition not reached in time"
            }
        }
    }

    /**
     * @return exact time to be set for a transfer
     * that must be logged at the given [date]
     */
    suspend fun getTimeForTransfer(date: LocalDate): Instant {
        val lastTransferOfTheDay: Transfer? =
            getTransferHistoryPage(
                withinPeriod = HistoryPeriod.Day(
                    localDay = date,
                ),
                limit = 1,
                cursor = null,
                sourceId = null,
                destinationId = null,
            )
                .data
                .firstOrNull()

        return (lastTransferOfTheDay?.time ?: date.atStartOfDayIn(TimeZone.currentSystemDefault()))
            .plus(1, DateTimeUnit.SECOND)
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

    private fun TransferHistoryPage.toLoadResultPage() = LoadResult.Page(
        data = data,
        prevKey = previousPageCursor,
        nextKey = nextPageCursor,
    )

    /**
     * @return ISO-8601 datetime with T, without millis,
     * with explicitly specified UTC timezone (Z).
     * For example, 2025-02-22T08:37:23Z.
     */
    private fun Instant.toDbTimeString(): String =
        Instant.fromEpochSeconds(epochSeconds).toString()
}

private const val UNIX_TIME = "unix_time"

private const val SELECT_TRANSFERS =
    "SELECT transfers.id, transfers.time, " +
            "transfers.source_id, transfers.source_amount, " +
            "transfers.destination_id, transfers.destination_amount, transfers.memo, " +
            "unixepoch(transfers.time) AS $UNIX_TIME " +
            "FROM transfers"


private const val SELECT_SUBCATEGORIES_IDS_FOR_CATEGORY =
    "SELECT categories.id FROM categories WHERE categories.parent_id = ?"

/**
 * Params:
 * 1. Source ID
 * 2. Source ID once again
 */
private const val SELECT_TRANSFERS_FOR_SOURCE =
    "$SELECT_TRANSFERS " +
            "WHERE (transfers.source_id = ? OR transfers.source_id in " +
            "($SELECT_SUBCATEGORIES_IDS_FOR_CATEGORY)) "

/**
 * Params:
 * 1. Destination ID
 * 2. Destination ID once again
 */
private const val SELECT_TRANSFERS_FOR_DESTINATION =
    "$SELECT_TRANSFERS " +
            "WHERE (transfers.destination_id = ? OR transfers.destination_id in " +
            "($SELECT_SUBCATEGORIES_IDS_FOR_CATEGORY)) "

/**
 * Params:
 * 1. Transfer ID
 */
private const val SELECT_BY_ID =
    "$SELECT_TRANSFERS WHERE transfers.id = ?"

/**
 * Params:
 * 1. Transfer ID
 * 2. Time string
 * 3. Source ID
 * 4. Source amount
 * 5. Destination ID
 * 6. Destination amount
 * 7. Memo
 * 8. Metadata
 */
private const val INSERT_OR_REPLACE_TRANSFER =
    "INSERT OR REPLACE INTO transfers " +
            "(id, time, source_id, source_amount, destination_id, destination_amount, memo, _metadata) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
