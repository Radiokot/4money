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

package ua.com.radiokot.money.accounts.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.logic.UpdateAccountPositionUseCase
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import java.math.BigInteger

class AccountsViewModel(
    accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyPreferences: CurrencyPreferences,
    private val updateAccountPositionUseCase: UpdateAccountPositionUseCase,
) : ViewModel() {

    private val log by lazyLogger("AccountsVM")
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val accountListItems: StateFlow<List<ViewAccountListItem>> =
        combine(
            accountRepository.getAccountsFlow(),
            currencyRepository.getCurrencyPairMapFlow(),
            currencyPreferences.primaryCurrencyCode,
            transform = ::Triple
        )
            .map { (accounts, currencyPairMap, primaryCurrencyCode) ->
                val primaryCurrency: Currency? = currencyRepository
                    .getCurrencyByCode(primaryCurrencyCode)

                accounts
                    .sorted()
                    .groupBy(Account::type)
                    .flatMap { (type, accountsOfType) ->
                        buildList {
                            if (primaryCurrency != null) {
                                val totalInPrimaryCurrency =
                                    accountsOfType.fold(BigInteger.ZERO) { sum, account ->
                                        sum + (
                                                currencyPairMap
                                                    .get(
                                                        base = account.currency,
                                                        quote = primaryCurrency,
                                                    )
                                                    ?.baseToQuote(account.balance)
                                                    ?: BigInteger.ZERO
                                                )
                                    }
                                add(
                                    ViewAccountListItem.Header(
                                        title = type.name,
                                        amount = ViewAmount(
                                            value = totalInPrimaryCurrency,
                                            currency = primaryCurrency,
                                        ),
                                        key = type.slug,
                                    )
                                )
                            } else {
                                add(
                                    ViewAccountListItem.Header(
                                        title = type.name,
                                        amount = null,
                                        key = type.slug,
                                    )
                                )
                            }

                            accountsOfType.forEach { account ->
                                add(ViewAccountListItem.Account(account))
                            }
                        }
                    }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalAmountsPerCurrency: StateFlow<List<ViewAmount>> =
        accountRepository.getAccountsFlow()
            .map { accounts ->
                accounts
                    .groupBy(Account::currency)
                    .map { (currency, accountsInThisCurrency) ->
                        ViewAmount(
                            value = accountsInThisCurrency.sumOf(Account::balance),
                            currency = currency,
                        )
                    }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalAmount: StateFlow<ViewAmount?> =
        combine(
            accountRepository.getAccountsFlow(),
            currencyRepository.getCurrencyPairMapFlow(),
            currencyPreferences.primaryCurrencyCode,
            transform = ::Triple
        )
            .map { (accounts, currencyPairMap, primaryCurrencyCode) ->
                val primaryCurrency = currencyRepository
                    .getCurrencyByCode(primaryCurrencyCode)
                    ?: return@map null

                val totalInPrimaryCurrency: BigInteger =
                    accounts.fold(BigInteger.ZERO) { sum, account ->
                        sum + (
                                currencyPairMap
                                    .get(
                                        base = account.currency,
                                        quote = primaryCurrency,
                                    )
                                    ?.baseToQuote(account.balance)
                                    ?: BigInteger.ZERO
                                )
                    }

                ViewAmount(
                    value = totalInPrimaryCurrency,
                    currency = primaryCurrency,
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun onAccountItemClicked(item: ViewAccountListItem.Account) {
        val account = item.source
        if (account == null) {
            log.warn {
                "onAccountItemClicked(): missing account source"
            }
            return
        }

        log.debug {
            "onAccountItemClicked(): proceeding to account actions:" +
                    "\naccount=$account"
        }

        _events.tryEmit(
            Event.ProceedToAccountActions(account)
        )
    }

    private var updatePositionJob: Job? = null
    fun onAccountItemMoved(
        itemToMove: ViewAccountListItem.Account,
        itemToPlaceBefore: ViewAccountListItem.Account?,
    ) {
        val accountToMove = itemToMove.source
        val accountToPlaceBeforeId = itemToPlaceBefore?.source?.id
        if (accountToMove == null || accountToPlaceBeforeId == null && itemToPlaceBefore != null) {
            log.warn {
                "onAccountItemMoved(): missing account source(s)"
            }
            return
        }

        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch {
            log.debug {
                "onAccountItemMoved(): moving:" +
                        "\naccount=${accountToMove.id}," +
                        "\nbefore=$accountToPlaceBeforeId"
            }

            updateAccountPositionUseCase(
                withinType = accountToMove.type,
                accountToMoveId = accountToMove.id,
                accountToPlaceBeforeId = accountToPlaceBeforeId,
            )
                .onFailure { error ->
                    log.error(error) {
                        "onAccountItemMoved(): failed to move"
                    }
                }
                .onSuccess {
                    log.debug {
                        "onAccountItemMoved(): moved successfully"
                    }
                }
        }
    }

    sealed interface Event {

        class ProceedToAccountActions(
            val account: Account,
        ) : Event
    }
}
