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

import androidx.compose.runtime.Immutable
import ua.com.radiokot.money.currency.view.ViewAmount
import kotlin.random.Random

@Immutable
sealed interface ViewAccountListItem {

    val type: String
    val key: Any

    class Header(
        val title: String,
        val amount: ViewAmount,
        override val key: Any,
    ) : ViewAccountListItem {
        override val type = "header"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Header) return false

            if (title != other.title) return false
            if (amount != other.amount) return false
            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + amount.hashCode()
            result = 31 * result + key.hashCode()
            return result
        }
    }

    class Account(
        val title: String,
        val balance: ViewAmount,
        val isIncognito: Boolean,
        val source: ua.com.radiokot.money.accounts.data.Account? = null,
        override val key: Any = source?.hashCode() ?: Random.nextInt(),
    ) : ViewAccountListItem {

        override val type = "account"

        constructor(
            account: ua.com.radiokot.money.accounts.data.Account,
            isIncognito: Boolean = false,
        ) : this(
            title = account.title,
            balance = ViewAmount(
                value = account.balance,
                currency = account.currency,
            ),
            isIncognito = isIncognito,
            source = account,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Account) return false

            if (title != other.title) return false
            if (balance != other.balance) return false
            if (isIncognito != other.isIncognito) return false
            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + balance.hashCode()
            result = 31 * result + isIncognito.hashCode()
            result = 31 * result + key.hashCode()
            return result
        }
    }
}
