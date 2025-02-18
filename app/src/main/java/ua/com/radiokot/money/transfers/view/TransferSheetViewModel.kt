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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.transfers.logic.TransferFundsUseCase
import java.math.BigInteger

class TransferSheetViewModel(
    private val accountRepository: AccountRepository,
    private val categoriesRepository: CategoryRepository,
    private val transferFundsUseCase: TransferFundsUseCase,
) : ViewModel() {

    private val log by lazyLogger("TransferSheetVM")
    private var accountSubscriptionJob: Job? = null
    private val _isOpened: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOpened = _isOpened.asStateFlow()
    private val _isSourceInputShown: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSourceInputShown = _isSourceInputShown.asStateFlow()
    private val _source: MutableStateFlow<ViewTransferCounterparty?> = MutableStateFlow(null)
    val source = _source.asStateFlow()
    private val _sourceAmountValue: MutableStateFlow<BigInteger> = MutableStateFlow(BigInteger.ZERO)
    val sourceAmountValue = _sourceAmountValue.asStateFlow()
    private val _destination: MutableStateFlow<ViewTransferCounterparty?> = MutableStateFlow(null)
    val destination = _destination.asStateFlow()
    private val _destinationAmountValue: MutableStateFlow<BigInteger> =
        MutableStateFlow(BigInteger.ZERO)
    val destinationAmountValue = _destinationAmountValue.asStateFlow()
    private val _isSaveEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSaveEnabled = _isSaveEnabled.asStateFlow()
    private lateinit var sourceAccount: Account
    private lateinit var destinationAccount: Account

    init {
        // Only require source input if currencies are different.
        viewModelScope.launch {
            source.combine(destination, ::Pair)
                .collect { (source, destination) ->
                    _isSourceInputShown.emit(source?.currency != destination?.currency)
                }
        }

        // Only enable save if the input is valid.
        viewModelScope.launch {
            val amountValues = sourceAmountValue.combine(destinationAmountValue, ::Pair)
            amountValues.combine(isSourceInputShown, ::Pair)
                .collect { (amounts, isSourceInputRequired) ->
                    val (sourceAmountValue, destAmountValue) = amounts
                    _isSaveEnabled.emit(
                        (!isSourceInputRequired || sourceAmountValue.signum() > 0)
                                && destAmountValue.signum() > 0
                    )
                }
        }
    }

    fun open(
        sourceAccount: Account,
        destinationAccount: Account,
    ) {
        log.debug {
            "open(): opening: " +
                    "\nsourceAccount=$sourceAccount," +
                    "\ndestinationAccount=$destinationAccount"
        }

        accountSubscriptionJob?.cancel()
        accountSubscriptionJob = viewModelScope.launch {
            launch {
                accountRepository
                    .getAccountFlow(sourceAccount.id)
                    .onStart { emit(sourceAccount) }
                    .collect { freshSourceAccount ->
                        this@TransferSheetViewModel.sourceAccount = freshSourceAccount
                        _source.emit(ViewTransferCounterparty(freshSourceAccount))
                    }
            }

            launch {
                accountRepository
                    .getAccountFlow(destinationAccount.id)
                    .onStart { emit(destinationAccount) }
                    .collect { freshDestAccount ->
                        this@TransferSheetViewModel.destinationAccount = freshDestAccount
                        _destination.emit(ViewTransferCounterparty(freshDestAccount))
                    }
            }
        }

        _sourceAmountValue.tryEmit(BigInteger.ZERO)
        _destinationAmountValue.tryEmit(BigInteger.ZERO)
        _isOpened.tryEmit(true)
    }

    fun onBackPressed() {
        val isOpened = _isOpened.value
        if (!isOpened) {
            log.warn {
                "onBackPressed(): ignoring back press as the sheet is already closed"
            }
            return
        }

        close()
    }

    private fun close() {
        log.debug {
            "close(): closing"
        }

        accountSubscriptionJob?.cancel()
        _isOpened.tryEmit(false)
    }

    fun onNewSourceAmountValueParsed(value: BigInteger) {
        log.debug {
            "onNewSourceAmountValueParsed(): updating source amount:" +
                    "value=$value"
        }

        _sourceAmountValue.tryEmit(value)
    }

    fun onNewDestinationAmountValueParsed(value: BigInteger) {
        log.debug {
            "onNewDestinationAmountValueParsed(): updating dest amount:" +
                    "value=$value"
        }

        _destinationAmountValue.tryEmit(value)
    }

    fun onSaveClicked() {
        if (!isSaveEnabled.value) {
            log.warn {
                "onSaveClicked(): ignoring as save is not enabled"
            }
            return
        }

        transferFunds()
    }

    private var transferJob: Job? = null
    private fun transferFunds() {
        val destinationAccountId = destinationAccount.id
        val destinationAmount = destinationAmountValue.value
        val sourceAccountId = sourceAccount.id
        val sourceAmount =
            if (isSourceInputShown.value)
                sourceAmountValue.value
            else
                destinationAmount

        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            log.debug {
                "transferFunds(): transferring:" +
                        "\nsourceAccountId=$sourceAccountId," +
                        "\nsourceAmount=$sourceAmount," +
                        "\ndestinationAccountId=$destinationAccountId," +
                        "\ndestinationAmount=$destinationAmount"
            }

            transferFundsUseCase(
                sourceAccountId = sourceAccountId,
                sourceAmount = sourceAmount,
                destinationAccountId = destinationAccountId,
                destinationAmount = destinationAmount,
            )
                .onFailure { error ->
                    log.error(error) {
                        "transferFunds(): failed to transfer funds"
                    }
                }
                .onSuccess {
                    log.debug {
                        "transferFunds(): funds transferred, closing"
                    }

                    close()
                }
        }
    }
}
