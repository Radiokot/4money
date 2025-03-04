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

package ua.com.radiokot.money.transfers.data

import ua.com.radiokot.money.categories.data.Subcategory

sealed class TransferCounterparty {

    abstract val id: TransferCounterpartyId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransferCounterparty) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    class Account(
        val account: ua.com.radiokot.money.accounts.data.Account,
    ) : TransferCounterparty() {

        override val id: TransferCounterpartyId.Account
            get() = TransferCounterpartyId.Account(account.id)

        override fun toString(): String {
            return "Account(account=$account)"
        }
    }

    class Category(
        val category: ua.com.radiokot.money.categories.data.Category,
        val subcategory: Subcategory? = null,
    ) : TransferCounterparty() {

        override val id: TransferCounterpartyId.Category
            get() = TransferCounterpartyId.Category(
                categoryId = category.id,
                subcategoryId = subcategory?.id,
            )

        override fun toString(): String {
            return "Category(category=$category, subcategory=$subcategory)"
        }
    }
}
