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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import ua.com.radiokot.money.R
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.transfers.data.TransferCounterparty

@Immutable
sealed interface ViewTransferCounterparty {

    val currency: ViewCurrency

    @get:Composable
    val title: String

    class Account(
        val accountTitle: String,
        override val currency: ViewCurrency,
    ) : ViewTransferCounterparty {

        override val title: String
            @Composable
            get() = accountTitle

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Account) return false

            if (accountTitle != other.accountTitle) return false
            if (currency != other.currency) return false

            return true
        }

        override fun hashCode(): Int {
            var result = accountTitle.hashCode()
            result = 31 * result + currency.hashCode()
            return result
        }
    }

    class Category(
        val categoryTitle: String,
        val subcategoryTitle: String?,
        override val currency: ViewCurrency,
    ) : ViewTransferCounterparty {

        override val title: String
            @Composable
            get() =
                if (subcategoryTitle != null)
                    stringResource(
                        id = R.string.template_category_subcategory,
                        categoryTitle,
                        subcategoryTitle
                    )
                else
                    categoryTitle

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Category) return false

            if (categoryTitle != other.categoryTitle) return false
            if (subcategoryTitle != other.subcategoryTitle) return false
            if (currency != other.currency) return false

            return true
        }

        override fun hashCode(): Int {
            var result = categoryTitle.hashCode()
            result = 31 * result + (subcategoryTitle?.hashCode() ?: 0)
            result = 31 * result + currency.hashCode()
            return result
        }
    }

    companion object {
        fun fromCounterparty(
            counterparty: TransferCounterparty,
        ): ViewTransferCounterparty = when (counterparty) {
            is TransferCounterparty.Account ->
                Account(
                    accountTitle = counterparty.account.title,
                    currency = ViewCurrency(
                        currency = counterparty.account.currency,
                    ),
                )

            is TransferCounterparty.Category ->
                Category(
                    categoryTitle = counterparty.category.title,
                    subcategoryTitle = counterparty.subcategory?.title,
                    currency = ViewCurrency(
                        currency = counterparty.category.currency,
                    ),
                )
        }
    }
}
