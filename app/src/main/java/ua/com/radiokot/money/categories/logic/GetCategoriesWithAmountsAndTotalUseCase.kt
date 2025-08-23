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

package ua.com.radiokot.money.categories.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import ua.com.radiokot.money.categories.data.CategoriesWithAmountAndTotal
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.CategoryWithAmount
import ua.com.radiokot.money.currency.data.Amount
import ua.com.radiokot.money.currency.data.CurrencyPairMap
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyPriceRepository
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class GetCategoriesWithAmountsAndTotalUseCase(
    private val currencyPreferences: CurrencyPreferences,
    private val currencyRepository: CurrencyRepository,
    private val currencyPriceRepository: CurrencyPriceRepository,
    private val categoryRepository: CategoryRepository,
    private val historyStatsRepository: HistoryStatsRepository,
) {

    /**
     * @return existing categories with their total amounts for the given [period]
     * and, if the primary currency exists, a total amount in the primary currency
     * calculated with daily prices.
     */
    operator fun invoke(
        isIncome: Boolean,
        period: HistoryPeriod,
    ): Flow<CategoriesWithAmountAndTotal> =
        combine(
            // Primary currency or null.
            currencyPreferences
                .primaryCurrencyCode
                .mapLatest(currencyRepository::getCurrencyByCode),

            // Categories.
            categoryRepository
                .getCategoriesFlow(
                    isIncome = isIncome,
                ),

            // Daily amounts.
            historyStatsRepository
                .getCategoryDailyAmountsFlow(
                    isIncome = isIncome,
                    period = period,
                ),

            transform = ::Triple,
        ).mapLatest { (primaryCurrency, categories, dailyAmountsByCategoryId) ->

            val dailyPrices: Map<String, CurrencyPairMap> =
                if (primaryCurrency != null)
                    currencyPriceRepository
                        .getDailyPrices(
                            period = period,
                            currencyCodes = categories
                                .filter { dailyAmountsByCategoryId.containsKey(it.id) }
                                .mapTo(mutableSetOf(primaryCurrency.code)) { it.currency.code },
                        )
                else
                    emptyMap()

            val categoriesWithTotal = mutableListOf<CategoryWithAmount>()
            var totalInPrimaryCurrency: BigInteger = BigInteger.ZERO

            categories.forEach { category ->

                val categoryDailyAmounts: Collection<Pair<String, BigInteger>> =
                    dailyAmountsByCategoryId[category.id]
                        ?.entries
                        ?.map { it.key to it.value }
                        ?: emptySet()

                categoriesWithTotal += CategoryWithAmount(
                    category = category,
                    amount = categoryDailyAmounts.sumOf { it.second },
                )

                if (primaryCurrency != null) {

                    categoryDailyAmounts.forEach { (dayString, amount) ->

                        var pricesForTheDay: CurrencyPairMap? = dailyPrices[dayString]

                        // If there's no price for this day,
                        // which could happen due to time zone differences,
                        // try the previous day which must exist at this moment.
                        if (pricesForTheDay == null) {
                            val previousDayString =
                                LocalDate
                                    .parse(dayString, LocalDate.Formats.ISO)
                                    .minus(1, DateTimeUnit.DAY)
                                    .toString()
                            pricesForTheDay = dailyPrices[previousDayString]
                        }

                        totalInPrimaryCurrency +=
                            pricesForTheDay
                                ?.get(
                                    base = category.currency,
                                    quote = primaryCurrency,
                                )
                                ?.baseToQuote(amount)
                                ?: BigInteger.ZERO
                    }
                }
            }

            CategoriesWithAmountAndTotal(
                totalInPrimaryCurrency = primaryCurrency?.let {
                    Amount(
                        currency = it,
                        value = totalInPrimaryCurrency
                    )
                },
                categories = categoriesWithTotal,
            )
        }.flowOn(Dispatchers.Default)
}
