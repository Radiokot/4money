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

package ua.com.radiokot.money.colors.view

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.colors.data.ItemIconRepository
import ua.com.radiokot.money.colors.data.ItemLogoType
import ua.com.radiokot.money.eventSharedFlow

class ItemLogoScreenViewModel(
    parameters: Parameters,
    itemColorSchemeRepository: ItemColorSchemeRepository,
    itemIconRepository: ItemIconRepository,
) : ViewModel() {

    private val _colorSchemeList: MutableStateFlow<List<ItemColorScheme>> =
        MutableStateFlow(itemColorSchemeRepository.getItemColorSchemes())
    val colorSchemeList = _colorSchemeList.asStateFlow()
    private val _iconList: MutableStateFlow<List<ItemIcon>> =
        MutableStateFlow(itemIconRepository.getItemIcons())
    val iconList = _iconList.asStateFlow()
    private val _selectedColorScheme: MutableStateFlow<ItemColorScheme> =
        MutableStateFlow(
            itemColorSchemeRepository
                .getItemColorSchemesByName()
                .getValue(parameters.initialColorSchemeName)
        )
    val selectedColorScheme = _selectedColorScheme.asStateFlow()
    private val _selectedIcon: MutableStateFlow<ItemIcon?> =
        MutableStateFlow(
            itemIconRepository
                .getItemIconsByName()
                [parameters.initialIconName]
        )
    val selectedIcon = _selectedIcon.asStateFlow()
    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events = _events.asSharedFlow()
    val logoType = parameters.logoType
    val itemTitle = parameters.itemTitle

    fun onColorSchemeClicked(colorScheme: ItemColorScheme) {
        _selectedColorScheme.value = colorScheme
    }

    fun onIconClicked(icon: ItemIcon?) {
        _selectedIcon.value = icon
    }

    fun onCloseClicked() {
        _events.tryEmit(Event.Close)
    }

    fun onSaveClicked() {
        _events.tryEmit(
            Event.Done(
                colorScheme = _selectedColorScheme.value,
                icon = _selectedIcon.value,
            )
        )
    }

    sealed interface Event {

        object Close : Event

        class Done(
            val colorScheme: ItemColorScheme,
            val icon: ItemIcon?,
        ) : Event
    }

    class Parameters(
        val logoType: ItemLogoType,
        val itemTitle: String,
        val initialColorSchemeName: String,
        val initialIconName: String?,
    )
}
