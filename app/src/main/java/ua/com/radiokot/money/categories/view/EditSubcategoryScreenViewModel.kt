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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.com.radiokot.money.categories.data.SubcategoryToUpdate
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.eventSharedFlow
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.map

class EditSubcategoryScreenViewModel(
    parameters: Parameters,
    itemColorSchemeRepository: ItemColorSchemeRepository,
) : ViewModel() {

    private val log by lazyLogger("EditSubcategoryScreenVM")
    private val subcategoryToUpdate = parameters.subcategoryToUpdate
    val isNewSubcategory: Boolean = subcategoryToUpdate is SubcategoryToUpdate.New
    private val _title: MutableStateFlow<String> =
        MutableStateFlow(subcategoryToUpdate.title)
    val title = _title.asStateFlow()
    private val _colorScheme: MutableStateFlow<ItemColorScheme> = MutableStateFlow(
        itemColorSchemeRepository
            .getItemColorSchemesByName()
            .getValue(parameters.colorSchemeName)
    )
    val colorScheme = _colorScheme.asStateFlow()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()

    val isSaveEnabled: StateFlow<Boolean> =
        _title
            .map(viewModelScope, String::isNotBlank)

    fun onTitleChanged(newValue: String) {
        _title.value = newValue
    }

    fun onSaveClicked() {

        if (!isSaveEnabled.value) {
            return
        }

        val result: SubcategoryToUpdate = when (subcategoryToUpdate) {
            is SubcategoryToUpdate.Existing ->
                SubcategoryToUpdate.Existing(
                    id = subcategoryToUpdate.id,
                    title = title.value,
                    index = subcategoryToUpdate.index,
                )

            is SubcategoryToUpdate.New ->
                SubcategoryToUpdate.New(
                    title = title.value,
                    index = subcategoryToUpdate.index,
                )
        }

        log.debug {
            "onSaveClicked(): done:" +
                    "\nresult=$result"
        }

        _events.tryEmit(Event.Done(result))
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    sealed interface Event {

        object Close : Event

        class Done(
            val subcategoryToUpdate: SubcategoryToUpdate,
        ) : Event
    }

    class Parameters(
        val subcategoryToUpdate: SubcategoryToUpdate,
        val colorSchemeName: String,
    )
}
