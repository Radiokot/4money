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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.logic.GetVisibleAccountsUseCase
import ua.com.radiokot.money.accounts.logic.MoveAccountUseCase
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import java.math.BigInteger

class AccountsViewModel(
    private val currencyRepository: CurrencyRepository,
    currencyPreferences: CurrencyPreferences,
    getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
    private val moveAccountUseCase: MoveAccountUseCase,
) : ViewModel() {

    private val log by lazyLogger("AccountsVM")
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    private val visibleAccounts: SharedFlow<List<Account>> =
        getVisibleAccountsUseCase()
            .shareIn(viewModelScope, SharingStarted.Lazily)

    val accountListItems: StateFlow<List<ViewAccountListItem>> =
        combine(
            visibleAccounts,
            currencyRepository.getCurrencyPairMapFlow(),
            currencyPreferences.primaryCurrencyCode,
            transform = ::Triple
        )
            .map { (accounts, currencyPairMap, primaryCurrencyCode) ->
                val primaryCurrency: Currency? = currencyRepository
                    .getCurrencyByCode(primaryCurrencyCode)

                accounts
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
        visibleAccounts
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
            visibleAccounts,
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
        itemToPlaceAfter: ViewAccountListItem.Account?,
    ) {
        val accountToMove = itemToMove.source
        if (accountToMove == null) {
            log.warn {
                "onAccountItemMoved(): missing moved account source"
            }
            return
        }

        val accountToPlaceBefore: Account? = itemToPlaceBefore?.source
        if (accountToPlaceBefore == null && itemToPlaceBefore != null) {
            log.warn {
                "onAccountItemMoved(): missing account to place before source"
            }
            return
        }

        val accountToPlaceAfter: Account? = itemToPlaceAfter?.source
        if (accountToPlaceAfter == null && itemToPlaceAfter != null) {
            log.warn {
                "onAccountItemMoved(): missing account to place after source"
            }
            return
        }

        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch {
            log.debug {
                "onAccountItemMoved(): moving:" +
                        "\naccount=$accountToMove," +
                        "\nbefore=$accountToPlaceBefore," +
                        "\nafter=$accountToPlaceAfter"
            }

            moveAccountUseCase(
                accountToMove = accountToMove,
                accountToPlaceBefore = accountToPlaceBefore,
                accountToPlaceAfter = accountToPlaceAfter,
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

    fun onAddClicked() {
        log.debug {
            "onAddClicked(): proceeding to adding new account"
        }

        _events.tryEmit(Event.ProceedToAccountAdd)
    }

    sealed interface Event {

        class ProceedToAccountActions(
            val account: Account,
        ) : Event

        object ProceedToAccountAdd : Event
    }
}
