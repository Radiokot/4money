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

package ua.com.radiokot.money.transfers.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.view.ViewAccountListItem
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.categories.view.viewCategoryItemListFlow
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import ua.com.radiokot.money.transfers.data.TransferCounterpartyId
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.data.HistoryStatsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCounterpartySelectionSheetViewModel(
    accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val historyStatsRepository: HistoryStatsRepository,
) : ViewModel() {

    private val log by lazyLogger("TransferCounterpartySelectionSheetVM")
    private val _isForSource: MutableSharedFlow<Boolean> =
        MutableSharedFlow(replay = 1)
    val isForSource: StateFlow<Boolean> =
        _isForSource.stateIn(viewModelScope, SharingStarted.Lazily, false)
    private val alreadySelectedCounterpartyId: MutableSharedFlow<TransferCounterpartyId?> =
        MutableSharedFlow(replay = 1)
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val accountListItems: StateFlow<List<ViewAccountListItem>> =
        combine(
            accountRepository.getAccountsFlow(),
            alreadySelectedCounterpartyId,
            transform = ::Pair
        )
            .map { (accounts, alreadySelectedCounterpartyId) ->
                accounts
                    .sortedBy(Account::title)
                    .filterNot { it.id == alreadySelectedCounterpartyId.toString() }
                    .map(ViewAccountListItem::Account)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val areCategoriesVisible: StateFlow<Boolean> =
        alreadySelectedCounterpartyId
            .map { alreadySelectedCounterpartyId ->
                alreadySelectedCounterpartyId !is TransferCounterpartyId.Category
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val categoryListItems: StateFlow<List<ViewCategoryListItem>> =
        combine(
            _isForSource,
            areCategoriesVisible,
            ::Pair
        )
            .flatMapLatest { (isForSource, areCategoriesVisible) ->
                if (areCategoriesVisible)
                    viewCategoryItemListFlow(
                        isIncome = isForSource,
                        period = HistoryPeriod.Month(),
                        categoryRepository = categoryRepository,
                        historyStatsRepository = historyStatsRepository,
                    )
                else
                    flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setParameters(
        isForSource: Boolean,
        alreadySelectedCounterpartyId: TransferCounterpartyId?,
    ) {
        log.debug {
            "setParameters(): setting:" +
                    "\nisForSource=$isForSource" +
                    "\nalreadySelectedCounterpartyId=$alreadySelectedCounterpartyId"
        }

        this._isForSource.tryEmit(isForSource)
        this.alreadySelectedCounterpartyId.tryEmit(alreadySelectedCounterpartyId)
    }

    fun onAccountItemClicked(item: ViewAccountListItem.Account) {
        val account = item.source
        if (account == null) {
            log.warn {
                "onAccountItemClicked(): missing account source"
            }
            return
        }

        log.debug {
            "onAccountItemClicked(): posting selected:" +
                    "\naccount=$account"
        }

        _events.tryEmit(
            Event.CounterpartySelected(TransferCounterparty.Account(account))
        )
    }

    fun onCategoryItemClicked(item: ViewCategoryListItem) {
        val category = item.source
        if (category == null) {
            log.warn {
                "onCategoryItemClicked(): missing category source"
            }
            return
        }

        log.debug {
            "onCategoryItemClicked(): posting selected:" +
                    "\ncategory=$category"
        }

        _events.tryEmit(
            Event.CounterpartySelected(TransferCounterparty.Category(category))
        )
    }

    sealed interface Event {

        class CounterpartySelected(
            val counterparty: TransferCounterparty,
        ) : Event
    }
}
