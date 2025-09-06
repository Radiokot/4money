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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.AccountRepository
import ua.com.radiokot.money.accounts.logic.AddAccountUseCase
import ua.com.radiokot.money.accounts.logic.ArchiveAccountUseCase
import ua.com.radiokot.money.accounts.logic.EditAccountUseCase
import ua.com.radiokot.money.accounts.logic.UnarchiveAccountUseCase
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map

class EditAccountScreenViewModel(
    parameters: Parameters,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyPreferences: CurrencyPreferences,
    itemColorSchemeRepository: ItemColorSchemeRepository,
    private val editAccountUseCase: EditAccountUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val archiveAccountUseCase: ArchiveAccountUseCase,
    private val unarchiveAccountUseCase: UnarchiveAccountUseCase,
) : ViewModel() {

    private val log by lazyLogger("EditAccountScreenVM")
    private val accountToEdit: Account? = runBlocking {
        if (parameters.accountToEditId != null) {
            accountRepository.getAccount(parameters.accountToEditId)
                ?: error("Account to edit not found")
        } else {
            null
        }
    }
    private val _title: MutableStateFlow<String> = MutableStateFlow(
        accountToEdit?.title ?: ""
    )
    private val _colorScheme: MutableStateFlow<ItemColorScheme> = MutableStateFlow(
        accountToEdit?.colorScheme
            ?: itemColorSchemeRepository.getItemColorSchemesByName().getValue("Green3")
    )
    private val _icon: MutableStateFlow<ItemIcon?> = MutableStateFlow(
        accountToEdit?.icon
    )
    private val _type: MutableStateFlow<Account.Type> = MutableStateFlow(
        accountToEdit?.type ?: Account.Type.Regular
    )
    private val _currency: MutableStateFlow<Currency> = MutableStateFlow(runBlocking {
        accountToEdit?.currency
            ?: (currencyRepository
                .getCurrencyByCode(
                    code = currencyPreferences.primaryCurrencyCode.value
                )
                ?: currencyRepository.getCurrencies().first())
    })
    private val _isArchived: MutableStateFlow<Boolean> = MutableStateFlow(
        accountToEdit?.isArchived ?: false
    )
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val isNewAccount: Boolean = accountToEdit == null

    val title = _title.asStateFlow()

    val colorScheme = _colorScheme.asStateFlow()

    val icon = _icon.asStateFlow()

    val type = _type.asStateFlow()

    val isArchived = _isArchived.asStateFlow()

    val currencyCode: StateFlow<String> =
        _currency
            .map(viewModelScope, Currency::code)

    val isTypeChangeEnabled: StateFlow<Boolean> =
        isArchived
            .map(viewModelScope, Boolean::not)

    val isCurrencyChangeEnabled: Boolean =
        isNewAccount

    val isArchivedVisible: Boolean =
        !isNewAccount

    val isSaveEnabled: StateFlow<Boolean> =
        _title
            .map(viewModelScope, String::isNotBlank)

    fun onTitleChanged(newValue: String) {
        _title.value = newValue
    }

    fun onTypeClicked() {
        _events.tryEmit(
            Event.ProceedToAccountTypeSelection(
                currentType = _type.value,
            )
        )
    }

    fun onLogoClicked() {
        _events.tryEmit(
            Event.ProceedToLogoCustomization(
                currentTitle = _title.value,
                currentColorScheme = _colorScheme.value,
                currentIcon = _icon.value,
            )
        )
    }

    fun onColorSchemeSelected(
        newColorScheme: ItemColorScheme,
    ) {
        log.debug {
            "onColorSchemeSelected(): changing color scheme:" +
                    "\nnewColorScheme=$newColorScheme"
        }

        _colorScheme.value = newColorScheme
    }

    fun onIconSelected(
        newIcon: ItemIcon?,
    ) {
        log.debug {
            "onIconSelected(): changing icon:" +
                    "\nnewIcon=$newIcon"
        }

        _icon.value = newIcon
    }

    fun onCurrencyClicked() {
        _events.tryEmit(
            Event.ProceedToCurrencySelection(
                currentCurrency = _currency.value,
            )
        )
    }

    fun onCurrencySelected(
        newCurrency: Currency,
    ) {
        log.debug {
            "onCurrencySelected(): changing currency:" +
                    "\nnewCurrency=$newCurrency"
        }

        _currency.value = newCurrency
    }

    fun onTypeSelected(
        newType: Account.Type,
    ) {
        log.debug {
            "onTypeSelected(): changing type:" +
                    "\nnewType=$newType"
        }

        _type.value = newType
    }

    fun onArchivedClicked() {

        log.debug {
            "onArchivedClicked(): flipping archived"
        }

        _isArchived.update(Boolean::not)
    }

    fun onSaveClicked() {

        if (!isSaveEnabled.value) {
            return
        }

        if (accountToEdit == null) {
            addAccount()
            return
        }

        if (isArchived.value && !accountToEdit.isArchived) {
            archiveAccount(accountToEdit)
        } else if (!isArchived.value && accountToEdit.isArchived) {
            unarchiveAccount(accountToEdit)
        } else {
            editAccount(accountToEdit)
        }
    }

    private var editJob: Job? = null
    private fun editAccount(
        accountToEdit: Account,
    ) {
        editJob?.cancel()
        editJob = viewModelScope.launch {

            val title = _title.value
            val type = _type.value
            val colorScheme = _colorScheme.value
            val icon = _icon.value

            log.debug {
                "editAccount(): editing:" +
                        "\naccountToEdit=$accountToEdit," +
                        "\ntitle=$title," +
                        "\ntype=$type," +
                        "\ncolorScheme=$colorScheme," +
                        "\nicon=$icon"
            }

            editAccountUseCase
                .invoke(
                    accountToEdit = accountToEdit,
                    newTitle = title,
                    newType = type,
                    newColorScheme = colorScheme,
                    newIcon = icon,
                )
                .onFailure { error ->
                    log.error(error) {
                        "editAccount(): failed to edit account"
                    }
                }
                .onSuccess {
                    log.info {
                        "Edited account $accountToEdit"
                    }

                    log.debug {
                        "editAccount(): account edited"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    private var archiveJob: Job? = null
    private fun archiveAccount(
        accountToArchive: Account,
    ) {
        archiveJob?.cancel()
        archiveJob = viewModelScope.launch {

            log.debug {
                "accountToArchive(): archiving:" +
                        "\naccountToArchive=$accountToArchive"
            }

            archiveAccountUseCase
                .invoke(
                    accountToArchive = accountToArchive,
                )
                .onFailure {
                    log.error(it) {
                        "archiveAccount(): failed to archive account"
                    }
                }
                .onSuccess {
                    log.info {
                        "Archived account $accountToArchive"
                    }

                    log.debug {
                        "archiveAccount(): account archived"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    private var unarchiveJob: Job? = null
    private fun unarchiveAccount(
        accountToUnarchive: Account,
    ) {
        unarchiveJob?.cancel()
        unarchiveJob = viewModelScope.launch {

            log.debug {
                "unarchiveAccount(): unarchiving:" +
                        "\naccountToUnarchive=$accountToUnarchive"
            }

            unarchiveAccountUseCase
                .invoke(
                    accountToUnrachive = accountToUnarchive,
                )
                .onFailure { error ->
                    log.error(error) {
                        "unarchiveAccount(): failed to unarchive account"
                    }
                }
                .onSuccess {
                    log.info {
                        "Unarchived account $accountToUnarchive"
                    }

                    log.debug {
                        "unarchiveAccount(): account unarchived"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    private var addJob: Job? = null
    private fun addAccount() {

        addJob?.cancel()
        addJob = viewModelScope.launch {

            val title = _title.value
            val type = _type.value
            val currency = _currency.value
            val colorScheme = _colorScheme.value
            val icon = _icon.value

            log.debug {
                "addAccount(): adding:" +
                        "\ntitle=$title," +
                        "\ntype=$type," +
                        "\ncurrency=$currency," +
                        "\ncolorScheme=$colorScheme," +
                        "\nicon=$icon"
            }

            addAccountUseCase(
                title = title,
                currency = currency,
                type = type,
                colorScheme = colorScheme,
                icon = icon,
            )
                .onFailure { error ->
                    log.error(error) {
                        "addAccount(): failed to add account"
                    }
                }
                .onSuccess {
                    log.info {
                        "Added new ${currency.code} account '$title'"
                    }

                    log.debug {
                        "addAccount(): account added"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    sealed interface Event {

        class ProceedToAccountTypeSelection(
            val currentType: Account.Type,
        ) : Event

        class ProceedToLogoCustomization(
            val currentTitle: String,
            val currentColorScheme: ItemColorScheme,
            val currentIcon: ItemIcon?,
        ) : Event

        class ProceedToCurrencySelection(
            val currentCurrency: Currency,
        ) : Event

        object Close : Event

        object Done : Event
    }

    class Parameters(
        val accountToEditId: String?,
    )
}
