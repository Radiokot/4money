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

import ua.com.radiokot.money.currency.view.ViewAmount

sealed interface ViewAccountListItem {
    data class Header(
        val title: String,
        val amount: ViewAmount,
        val key: Any,
    ) : ViewAccountListItem

    data class Account(
        val title: String,
        val balance: ViewAmount,
        val source: Any,
    ) : ViewAccountListItem {

        constructor(account: ua.com.radiokot.money.accounts.data.Account) : this(
            title = account.title,
            balance = ViewAmount(
                value = account.balance,
                currency = account.currency,
            ),
            source = account,
        )
    }
}
