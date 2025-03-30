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

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.money.lazyLogger

class TransferPreferencesOnPrefs(
    private val preferences: SharedPreferences,
) : TransfersPreferences {

    private val log by lazyLogger("TransferPreferencesOnPrefs")
    private val knownCategoriesKey = "known_categories"

    private fun getLastUsedAccountByCategoryKey(categoryId: String) =
        "transfer_last_used_acc_$categoryId"

    override fun getLastUsedAccountByCategory(
        categoryId: String,
    ): String? =
        preferences
            .getString(getLastUsedAccountByCategoryKey(categoryId), null)

    override fun setLastUsedAccountByCategory(
        categoryId: String,
        accountId: String,
    ) = preferences.edit {

        log.debug {
            "setLastUsedAccountByCategory(): setting new value:" +
                    "\ncategoryId=$categoryId," +
                    "\naccountId=$accountId"
        }

        putStringSet(
            knownCategoriesKey,
            preferences.getStringSet(knownCategoriesKey, emptySet())!! + categoryId
        )
        putString(getLastUsedAccountByCategoryKey(categoryId), accountId)
    }

    override val lastUsedAccountsByCategory: Map<String, String>
        get() = buildMap {
            preferences
                .getStringSet(knownCategoriesKey, emptySet())!!
                .forEach { categoryId ->
                    val accountId = getLastUsedAccountByCategory(categoryId)
                    if (accountId != null) {
                        put(categoryId, accountId)
                    }
                }
        }
}
