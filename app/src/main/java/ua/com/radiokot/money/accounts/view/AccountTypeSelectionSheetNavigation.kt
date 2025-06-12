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

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.bottomSheet

@Serializable
data class AccountTypeSelectionSheetRoute(
    private val selectedTypeSlug: String,
) {
    val selectedType: Account.Type
        get() = Account.Type.fromSlug(selectedTypeSlug)

    constructor(
        selectedType: Account.Type,
    ) : this(
        selectedTypeSlug = selectedType.slug
    )
}

fun NavGraphBuilder.accountTypeSelectionSheet(
    onDone: (Account.Type) -> Unit,
) = bottomSheet<AccountTypeSelectionSheetRoute> { entry ->

    val route: AccountTypeSelectionSheetRoute = entry.toRoute()

    AccountTypeSelectionSheet(
        selectedType = route.selectedType,
        onTypeClicked = onDone,
    )
}
