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

package ua.com.radiokot.money.accounts.logic

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountsOfTypeWithTotal
import ua.com.radiokot.money.accounts.data.AccountsWithTotal
import ua.com.radiokot.money.currency.data.Amount
import ua.com.radiokot.money.currency.data.CurrencyPairMap
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class GetVisibleAccountsWithTotalUseCase(
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
    private val currencyPreferences: CurrencyPreferences,
    private val currencyRepository: CurrencyRepository,
) {

    operator fun invoke(): Flow<AccountsWithTotal> =
        combine(
            getVisibleAccountsUseCase(),
            currencyPreferences
                .primaryCurrencyCode
                .mapLatest(currencyRepository::getCurrencyByCode),
            transform = ::Pair,
        ).mapLatest { (accounts, primaryCurrency) ->

            val latestPrices: CurrencyPairMap? =
                if (primaryCurrency != null)
                    currencyRepository
                        .getLatestPrices(
                            currencyCodes = accounts
                                .mapTo(mutableSetOf(primaryCurrency.code)) { it.currency.code },
                        )
                else
                    null

            val types: List<AccountsOfTypeWithTotal> = accounts
                .groupBy(Account::type)
                .map { (type, accountsOfType) ->

                    AccountsOfTypeWithTotal(
                        type = type,
                        accountsOfType = accountsOfType,
                        totalInPrimaryCurrency =
                            if (primaryCurrency != null)
                                Amount(
                                    currency = primaryCurrency,
                                    value = accountsOfType
                                        .sumOf { account ->
                                            latestPrices
                                                ?.get(
                                                    base = account.currency,
                                                    quote = primaryCurrency,
                                                )
                                                ?.baseToQuote(account.balance.value)
                                                ?: BigInteger.ZERO
                                        }
                                )
                            else
                                null,
                    )
                }

            AccountsWithTotal(
                accountsOfTypes = types,
                totalInPrimaryCurrency =
                    if (primaryCurrency != null)
                        Amount(
                            currency = primaryCurrency,
                            value = types.sumOf { typeWithTotal ->
                                typeWithTotal.totalInPrimaryCurrency?.value
                                    ?: BigInteger.ZERO
                            },
                        )
                    else
                        null,
            )
        }
}
