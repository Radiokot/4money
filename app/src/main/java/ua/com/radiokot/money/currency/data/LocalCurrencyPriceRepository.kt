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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.com.radiokot.money.MoneyAppDatabase
import ua.com.radiokot.money.powersync.DbSchema.toDbDayString
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import java.math.BigDecimal

class LocalCurrencyPriceRepository(
    private val database: MoneyAppDatabase,
) : CurrencyPriceRepository {

    override suspend fun getLatestPrices(
        currencyCodes: Collection<String>,
    ): CurrencyPairMap = withContext(Dispatchers.IO) {

        database
            .dailyPricesQueries
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

        database
            .dailyPricesQueries
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

    private fun newUsdPriceMap() =
        CurrencyPairMap(
            quoteCode = "USD",
        )
}
