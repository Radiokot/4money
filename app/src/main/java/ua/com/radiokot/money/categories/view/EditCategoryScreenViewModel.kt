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

package ua.com.radiokot.money.categories.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.CategoryRepository
import ua.com.radiokot.money.categories.data.SubcategoryToUpdate
import ua.com.radiokot.money.categories.logic.AddCategoryUseCase
import ua.com.radiokot.money.categories.logic.EditCategoryUseCase
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.currency.data.CurrencyPreferences
import ua.com.radiokot.money.currency.data.CurrencyRepository
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map

class EditCategoryScreenViewModel(
    parameters: Parameters,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyPreferences: CurrencyPreferences,
    itemColorSchemeRepository: ItemColorSchemeRepository,
    private val editCategoryUseCase: EditCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
) : ViewModel() {

    private val log by lazyLogger("EditCategoryScreenVM")
    private val categoryToEdit: Category? = runBlocking {
        if (parameters.categoryToEditId != null) {
            categoryRepository.getCategory(parameters.categoryToEditId)
                ?: error("Category to edit not found")
        } else {
            null
        }
    }
    val isNewCategory: Boolean = categoryToEdit == null
    val isIncome: Boolean = parameters.isIncome
    private val _title: MutableStateFlow<String> = MutableStateFlow(
        categoryToEdit?.title ?: ""
    )
    val title = _title.asStateFlow()
    private val _colorScheme: MutableStateFlow<ItemColorScheme> = MutableStateFlow(
        categoryToEdit?.colorScheme
            ?: itemColorSchemeRepository
                .getItemColorSchemesByName()
                .getValue(
                    if (isIncome)
                        "Turquoise2"
                    else
                        "Purple2"
                )
    )
    val colorScheme = _colorScheme.asStateFlow()
    private val _currency: MutableStateFlow<Currency> = MutableStateFlow(runBlocking {
        categoryToEdit?.currency
            ?: (currencyRepository
                .getCurrencyByCode(
                    code = currencyPreferences.primaryCurrencyCode.value
                )
                ?: currencyRepository.getCurrencies().first())
    })
    private val _subcategories: MutableStateFlow<List<SubcategoryToUpdate>> =
        MutableStateFlow(runBlocking {
            if (categoryToEdit != null)
                categoryRepository
                    .getSubcategoriesFlow(
                        categoryId = categoryToEdit.id,
                    )
                    .first()
                    .sorted()
                    .map(::SubcategoryToUpdate)
            else
                emptyList()
        })
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val currencyCode: StateFlow<String> =
        _currency
            .map(viewModelScope, Currency::code)

    val isCurrencyChangeEnabled: Boolean =
        isNewCategory

    val subcategories: StateFlow<List<ViewSubcategoryToUpdateListItem>> =
        _subcategories
            .map(viewModelScope) { subcategories ->
                subcategories.map(::ViewSubcategoryToUpdateListItem)
            }

    val isSaveEnabled: StateFlow<Boolean> =
        _title
            .map(viewModelScope, String::isNotBlank)

    fun onTitleChanged(newValue: String) {
        _title.value = newValue
    }

    fun onLogoClicked() {
        _events.tryEmit(
            Event.ProceedToLogoCustomization(
                currentTitle = _title.value,
                currentColorScheme = _colorScheme.value,
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

    fun onAddSubcategoryClicked() {

        log.debug {
            "onAddSubcategoryClicked(): proceeding to new subcategory edit"
        }

        _events.tryEmit(
            Event.ProceedToSubcategoryEdit(
                subcategoryToUpdate = SubcategoryToUpdate.new(),
                colorScheme = colorScheme.value,
            )
        )
    }

    suspend fun onSubcategoryItemMoved(
        fromIndex: Int,
        toIndex: Int,
    ) {
        log.debug {
            "onSubcategoryItemMoved(): moving:" +
                    "\nfromIndex=$fromIndex," +
                    "\ntoIndex=$toIndex"
        }

        _subcategories.emit(
            _subcategories
                .value
                .toMutableList()
                .apply {
                    add(
                        toIndex,
                        removeAt(fromIndex)
                    )
                }
        )
    }

    fun onSubcategoryItemClicked(
        clickedItem: ViewSubcategoryToUpdateListItem,
    ) {
        val subcategoryToUpdate = clickedItem.source
        if (subcategoryToUpdate == null) {
            log.warn {
                "onSubcategoryItemClicked(): missing subcategory source"
            }
            return
        }

        log.debug {
            "onSubcategoryItemClicked(): proceeding to subcategory edit:" +
                    "\nsubcategoryToUpdate=$subcategoryToUpdate"
        }

        _events.tryEmit(
            Event.ProceedToSubcategoryEdit(
                subcategoryToUpdate = subcategoryToUpdate,
                colorScheme = colorScheme.value,
            )
        )
    }

    fun onSubcategoryEdited(
        subcategoryToUpdate: SubcategoryToUpdate,
    ) {
        log.debug {
            "onSubcategoryEdited(): updating subcategories:" +
                    "\nsubcategoryToUpdate=$subcategoryToUpdate"
        }

        _subcategories.update { subcategories ->
            subcategories
                .toMutableList()
                .apply {
                    val index = subcategories
                        .indexOfFirst { it.id == subcategoryToUpdate.id }
                    if (index >= 0) {
                        set(index, subcategoryToUpdate)
                    } else {
                        add(subcategoryToUpdate)
                    }
                }
        }
    }

    fun onSaveClicked() {

        if (!isSaveEnabled.value) {
            return
        }

        if (categoryToEdit != null) {
            editCategory(
                categoryId = categoryToEdit.id,
            )
        } else {
            addCategory()
        }
    }

    private var editJob: Job? = null
    private fun editCategory(
        categoryId: String,
    ) {
        editJob?.cancel()
        editJob = viewModelScope.launch {

            val title = _title.value
            val colorScheme = _colorScheme.value
            val subcategories = _subcategories.value

            log.debug {
                "editCategory(): editing:" +
                        "\ncategoryId=$categoryId," +
                        "\ntitle=$title," +
                        "\ncolorScheme=$colorScheme," +
                        "\nsubcategories=${subcategories.size}"
            }

            editCategoryUseCase
                .invoke(
                    categoryId = categoryId,
                    newTitle = title,
                    newColorScheme = colorScheme,
                    subcategories = subcategories,
                )
                .onFailure { error ->
                    log.error(error) {
                        "editCategory(): failed to edit category"
                    }
                }
                .onSuccess {
                    log.debug {
                        "editCategory(): category edited"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    private var addJob: Job? = null
    private fun addCategory() {

        addJob?.cancel()
        addJob = viewModelScope.launch {

            val title = _title.value
            val currency = _currency.value
            val colorScheme = _colorScheme.value
            val isIncome = isIncome
            val subcategories = _subcategories.value

            log.debug {
                "addCategory(): adding:" +
                        "\ntitle=$title," +
                        "\ncurrency=$currency," +
                        "\ncolorScheme=$colorScheme," +
                        "\nisIncome=$isIncome," +
                        "\nsubcategories=${subcategories.size}"
            }

            addCategoryUseCase
                .invoke(
                    title = title,
                    currency = currency,
                    isIncome = isIncome,
                    colorScheme = colorScheme,
                    subcategories = subcategories,
                )
                .onFailure { error ->
                    log.error(error) {
                        "addCategory(): failed to add category"
                    }
                }
                .onSuccess {
                    log.debug {
                        "addCategory(): category added"
                    }

                    _events.emit(Event.Done)
                }
        }
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    sealed interface Event {

        class ProceedToLogoCustomization(
            val currentTitle: String,
            val currentColorScheme: ItemColorScheme,
        ) : Event

        class ProceedToCurrencySelection(
            val currentCurrency: Currency,
        ) : Event

        class ProceedToSubcategoryEdit(
            val subcategoryToUpdate: SubcategoryToUpdate,
            val colorScheme: ItemColorScheme,
        ) : Event

        object Close : Event

        object Done : Event
    }

    class Parameters(
        val categoryToEditId: String?,
        val isIncome: Boolean,
    )
}
