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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.plus
import ua.com.radiokot.money.powersync.DbSchema
import ua.com.radiokot.money.powersync.DbSchema.toDbString
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
        counterpartyIds: Set<String>?,
    ): TransferHistoryPage = withContext(Dispatchers.Default) {

        val counterpartiesById = getCounterpartiesById()

        val counterpartiesCondition: String? =
            if (counterpartyIds == null)
                null
            else
                buildString {
                    val counterpartyIdSetString = counterpartiesById
                        .values
                        .mapNotNull { counterparty ->
                            if (counterparty.id.toString() in counterpartyIds
                                || counterparty is TransferCounterparty.Category
                                && counterparty.subcategory != null
                                && counterparty.category.id in counterpartyIds
                            )
                                "'${counterparty.id}'"
                            else
                                null
                        }
                        .joinToString(
                            separator = ",",
                            prefix = "(",
                            postfix = ")",
                        )

                    append("(${DbSchema.TRANSFER_SELECTED_SOURCE_ID} IN ")
                    append(counterpartyIdSetString)
                    append(" OR ${DbSchema.TRANSFER_SELECTED_DESTINATION_ID} IN ")
                    append(counterpartyIdSetString)
                    append(')')
                }

        val timeCondition = buildString {
            append("${DbSchema.TRANSFER_SELECTED_DATETIME} >= datetime('${withinPeriod.startInclusive}') ")
            append("AND ${DbSchema.TRANSFER_SELECTED_DATETIME} < datetime('${withinPeriod.endExclusive}') ")
            when {
                cursor?.isBefore == true ->
                    append("AND ${DbSchema.TRANSFER_SELECTED_DATETIME} < datetime('${cursor.timeExclusive}') ")

                cursor?.isAfter == true ->
                    append("AND ${DbSchema.TRANSFER_SELECTED_DATETIME} > datetime('${cursor.timeExclusive}') ")
            }
        }

        val orderCondition = buildString {
            append("ORDER BY ${DbSchema.TRANSFER_SELECTED_DATETIME} ")
            when {
                cursor?.isAfter == true ->
                    append("ASC ")

                else ->
                    append("DESC ")
            }
        }

        val limitCondition = "LIMIT $limit "

        val transfers = database
            .getAll(
                sql = buildString {
                    append(SELECT_TRANSFERS)
                    append(" WHERE ")
                    if (counterpartiesCondition != null) {
                        append(counterpartiesCondition)
                        append(" AND ")
                    }
                    append(timeCondition)
                    append(orderCondition)
                    append(limitCondition)
                },
                parameters = listOf(),
                mapper = { sqlCursor ->
                    DbSchema.toTransfer(
                        sqlCursor = sqlCursor,
                        counterpartiesById = counterpartiesById,
                    )
                },
            )
            .run {
                if (cursor?.isAfter == true)
                    sortedByDescending(Transfer::dateTime)
                else
                    this
            }

        return@withContext TransferHistoryPage(
            data = transfers,
            nextPageCursor = transfers
                .lastOrNull()
                ?.dateTime
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
                ?.dateTime
                ?.let { newestTransferTime ->
                    TransferHistoryPage.Cursor(
                        timeExclusive = newestTransferTime,
                        isBefore = false,
                    )
                }
        )
    }

    override fun getTransferHistoryPagingSource(
        withinPeriod: HistoryPeriod,
        counterpartyIds: Set<String>?,
    ) = object : PagingSource<TransferHistoryPage.Cursor, Transfer>() {

        init {
            createdPagingSources += WeakReference(this)
        }

        override fun getRefreshKey(
            state: PagingState<TransferHistoryPage.Cursor, Transfer>,
        ): TransferHistoryPage.Cursor? = state
            .anchorPosition
            ?.let(state::closestPageToPosition)
            ?.data
            ?.firstOrNull()
            ?.dateTime
            ?.let { newestTransferTime ->
                TransferHistoryPage.Cursor(
                    timeExclusive = newestTransferTime + 1.seconds,
                    isBefore = true,
                )
            }

        override suspend fun load(
            params: LoadParams<TransferHistoryPage.Cursor>,
        ): LoadResult<TransferHistoryPage.Cursor, Transfer> =

            getTransferHistoryPage(
                cursor = params.key,
                limit = params.loadSize,
                withinPeriod = withinPeriod,
                counterpartyIds = counterpartyIds,
            ).toLoadResultPage()
    }

    override suspend fun getTransfer(
        transferId: String,
    ): Transfer {

        val counterpartiesById = getCounterpartiesById()

        return database
            .get(
                sql = SELECT_BY_ID,
                parameters = listOf(
                    transferId,
                ),
                mapper = { sqlCursor ->
                    DbSchema.toTransfer(
                        sqlCursor = sqlCursor,
                        counterpartiesById = counterpartiesById,
                    )
                }
            )
    }

    fun addOrUpdateTransfer(
        sourceId: TransferCounterpartyId,
        sourceAmount: BigInteger,
        destinationId: TransferCounterpartyId,
        destinationAmount: BigInteger,
        memo: String?,
        dateTime: LocalDateTime,
        metadata: String,
        transaction: PowerSyncTransaction,
        transferId: String = UUID.randomUUID().toString(),
    ) {
        val dateTimeString = dateTime.toDbString()

        log.debug {
            "addOrUpdateTransfer(): executing:" +
                    "\nid=$transferId," +
                    "\ndateTime=$dateTimeString," +
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
                dateTimeString,
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
                    mapper = (SqlCursor::columnCount),
                )
                .drop(1)
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
            sql = "DELETE FROM ${DbSchema.TRANSFERS_TABLE} " +
                    "WHERE ${DbSchema.ID} = ?",
            parameters = listOf(
                transferId,
            )
        )

        invalidatePagingSourcesWhen {
            database
                .watch(
                    sql = "SELECT ${DbSchema.ID} FROM ${DbSchema.TRANSFERS_TABLE} " +
                            "WHERE ${DbSchema.ID} = ?",
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

    private fun TransferHistoryPage.toLoadResultPage() = LoadResult.Page(
        data = data,
        prevKey = previousPageCursor,
        nextKey = nextPageCursor,
    )
}

private const val SELECT_TRANSFERS =
    "SELECT ${DbSchema.TRANSFER_SELECT_COLUMNS} " +
            "FROM ${DbSchema.TRANSFERS_TABLE}"

/**
 * Params:
 * 1. Transfer ID
 */
private const val SELECT_BY_ID =
    "$SELECT_TRANSFERS " +
            "WHERE ${DbSchema.TRANSFER_SELECTED_ID} = ?"

/**
 * Params:
 * 1. Transfer ID
 * 2. Date-time string
 * 3. Source ID
 * 4. Source amount
 * 5. Destination ID
 * 6. Destination amount
 * 7. Memo
 * 8. Metadata
 */
private const val INSERT_OR_REPLACE_TRANSFER =
    "INSERT OR REPLACE INTO ${DbSchema.TRANSFERS_TABLE} " +
            "(" +
            "${DbSchema.ID}, " +
            "${DbSchema.TRANSFER_TIME}, " +
            "${DbSchema.TRANSFER_SOURCE_ID}, " +
            "${DbSchema.TRANSFER_SOURCE_AMOUNT}, " +
            "${DbSchema.TRANSFER_DESTINATION_ID}, " +
            "${DbSchema.TRANSFER_DESTINATION_AMOUNT}, " +
            "${DbSchema.TRANSFER_MEMO}, " +
            "${DbSchema.METADATA} " +
            ") " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
