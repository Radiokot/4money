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

package ua.com.radiokot.money.currency.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ua.com.radiokot.money.DailyPricesQueries
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.powersync.DbSchema.toDbDayString
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds

class LocalCurrencyPriceRepository(
    private val dailyPricesQueries: DailyPricesQueries,
    private val postgrest: Postgrest,
) : CurrencyPriceRepository {

    private val log by lazyLogger("LocalCurrencyPriceRepo")

    override suspend fun getLatestPrices(
        currencyCodes: Collection<String>,
    ): CurrencyPairMap = withContext(Dispatchers.IO) {

        dailyPricesQueries
            .getLatest(
                currencyCodes = currencyCodes,
            )
            .executeAsList()
            .let { prices ->
                val priceMap = newUsdPriceMap()

                prices.forEach { (baseCode, priceString) ->
                    priceMap.put(baseCode, BigDecimal(priceString))
                }

                priceMap
            }
    }

    override suspend fun getDailyPrices(
        period: HistoryPeriod,
        currencyCodes: Collection<String>,
    ): Map<String, CurrencyPairMap> = withContext(Dispatchers.IO) {

        dailyPricesQueries
            .getInPeriod(
                periodStartDayInclusive = period.startInclusive.toDbDayString(),
                periodEndDayExclusive = period.endExclusive.toDbDayString(),
                currencyCodes = currencyCodes,
            )
            .executeAsList()
            .let { prices ->
                val pairMapsByDay = mutableMapOf<String, CurrencyPairMap>()

                prices.forEach { (baseCode, priceString, dayString) ->
                    pairMapsByDay
                        .getOrPut(dayString, ::newUsdPriceMap)
                        .put(baseCode, BigDecimal(priceString))
                }

                pairMapsByDay
            }
    }

    override suspend fun updateFromRemote() {

        var latestDayBeforeLoading: String?
        var latestLoadedDay: String? = null

        log.debug {
            "updateFromRemote: starting update"
        }

        do {
            latestDayBeforeLoading = getLatestDay()

            // Include the latest local/loaded day to the next portion,
            // as there is no guarantee it is fully loaded.
            val portionStartDayInclusive: String? = latestLoadedDay ?: latestDayBeforeLoading

            log.debug {
                "updateFromRemote(): fetching chunk:" +
                        "\nstartDayInclusive=$portionStartDayInclusive"
            }

            val pricesChunk = getRemotePricesChunk(
                startDayInclusive = portionStartDayInclusive,
            )

            log.debug {
                "updateFromRemote(): inserting loaded chunk:" +
                        "\nsize=${pricesChunk.size}"
            }

            withContext(Dispatchers.IO) {

                dailyPricesQueries.transaction {

                    pricesChunk.forEach { priceRow ->

                        // Example: ["AED","2023-04-02",0.272283]
                        val (
                            baseCode,
                            dayString,
                            priceString,
                        ) = priceRow.map { it.jsonPrimitive.content }

                        dailyPricesQueries
                            .insertOrReplace(
                                baseCode = baseCode,
                                dayString = dayString,
                                priceString = priceString,
                            )

                        if (priceRow == pricesChunk.last()) {
                            latestLoadedDay = dayString
                        }
                    }
                }
            }

            log.debug {
                "updateFromRemote(): chunk inserted:" +
                        "\nlatestLoadedDay=$latestLoadedDay"
            }

            delay(333.milliseconds)

        } while (latestDayBeforeLoading != latestLoadedDay)
    }

    private suspend fun getLatestDay(): String? = withContext(Dispatchers.IO) {
        dailyPricesQueries
            .getLatestDay()
            .executeAsOneOrNull()
            ?.dayString
    }

    private suspend fun getRemotePricesChunk(
        startDayInclusive: String?,
    ): List<JsonArray> =
        postgrest
            .rpc(
                function = "get_daily_prices_array",
                parameters = buildJsonObject {
                    put("start_day_inclusive", startDayInclusive)
                },
            )
            .decodeList()

    private fun newUsdPriceMap() =
        CurrencyPairMap(
            quoteCode = "USD",
        )
}
