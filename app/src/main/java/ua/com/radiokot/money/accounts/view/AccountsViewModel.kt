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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import java.math.BigInteger

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val log by lazyLogger("AccountsVM")
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val accountListItems: StateFlow<List<ViewAccountListItem>> =
        accountRepository.getAccountsFlow()
            .combine(currencyRepository.getCurrencyPairMapFlow(), ::Pair)
            .map { (accounts, currencyPairMap) ->
                val mainCurrency = currencyRepository.getCurrencies()
                    // It may not be available yet.
                    .firstOrNull { it.code == "USD" }

                buildList {
                    if (mainCurrency != null) {
                        val totalInMainCurrency: BigInteger =
                            accounts.fold(BigInteger.ZERO) { sum, account ->
                                sum + (
                                        currencyPairMap
                                            .get(
                                                base = account.currency,
                                                quote = mainCurrency,
                                            )
                                            ?.baseToQuote(account.balance)
                                            ?: BigInteger.ZERO
                                        )
                            }

                        add(
                            ViewAccountListItem.Header(
                                title = "Accounts",
                                amount = ViewAmount(
                                    value = totalInMainCurrency,
                                    currency = mainCurrency,
                                ),
                                key = "total",
                            )
                        )
                    }

                    accounts.forEach { account ->
                        add(ViewAccountListItem.Account(account))
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onAccountItemClicked(item: ViewAccountListItem.Account) {
        val account = item.source
        if (account == null) {
            log.warn {
                "onAccountItemClicked(): missing account source"
            }
            return
        }

        log.debug {
            "onAccountItemClicked(): posting clicked:" +
                    "\naccount=$account"
        }

        _events.tryEmit(
            Event.AccountClicked(account)
        )
    }

    fun onAccountItemMoved(
        itemToMove: ViewAccountListItem.Account,
        itemToPlaceBefore: ViewAccountListItem.Account?,
    ) {
        val accountToMove = itemToMove.source
        val accountToPlaceBefore = itemToPlaceBefore?.source
        if (accountToMove == null || accountToPlaceBefore == null && itemToPlaceBefore != null) {
            log.warn {
                "onAccountItemMoved(): missing account source(s)"
            }
            return
        }

        val accountListItems = accountListItems.value
        if (itemToPlaceBefore == accountListItems.getOrNull(accountListItems.indexOf(itemToMove) + 1)) {
            log.debug {
                "onAccountItemMoved(): ignoring as position didn't change"
            }
            return
        }

        log.debug {
            "onAccountItemMoved(): moving:" +
                    "\naccount=$accountToMove," +
                    "\nbefore=$accountToPlaceBefore"
        }

        // TODO fockin' move
    }

    sealed interface Event {

        class AccountClicked(
            val account: Account,
        ) : Event
    }
}
