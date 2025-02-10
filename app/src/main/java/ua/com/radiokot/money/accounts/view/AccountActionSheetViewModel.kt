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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.lazyLogger

class AccountActionSheetViewModel : ViewModel() {

    private val log by lazyLogger("AccountActionSheetVM")
    val _accountDetails: MutableStateFlow<ViewAccountDetails?> = MutableStateFlow(null)
    val accountDetails = _accountDetails
    val _isOpened: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOpened = _isOpened.asStateFlow()

    fun open(account: Account) {
        log.debug {
            "open(): opening: " +
                    "\naccount=$account"
        }

        _accountDetails.tryEmit(ViewAccountDetails(account))
        _isOpened.tryEmit(true)
    }

    fun onBalanceClicked() {
        log.debug {
            "onBalanceClicked(): switching to balance editing"
        }
    }

    fun onBackPressed() {
        val isOpened = _isOpened.value
        if (!isOpened) {
            log.warn {
                "onBackPressed(): ignoring back press as the sheet is already closed"
            }
        }

        log.debug {
            "onBackPressed(): closing"
        }

        _isOpened.tryEmit(false)
    }
}
