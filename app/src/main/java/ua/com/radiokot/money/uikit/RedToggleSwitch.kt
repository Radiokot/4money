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

package ua.com.radiokot.money.uikit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composeunstyled.ToggleSwitch

@Composable
fun RedToggleSwitch(
    modifier: Modifier = Modifier,
    isToggled: State<Boolean>,
    onToggled: (Boolean) -> Unit,
    isEnabled: Boolean = true,
) {
    val switchShape = RoundedCornerShape(12.dp)
    val borderColor =
        if (isEnabled)
            Color.DarkGray
        else
            Color.LightGray

    ToggleSwitch(
        toggled = isToggled.value,
        onToggled = onToggled,
        shape = switchShape,
        contentPadding = PaddingValues(4.dp),
        modifier = modifier
            .border(
                shape = switchShape,
                color = borderColor,
                width = 1.dp,
            )
    ) {
        RedToggle(
            isToggled = isToggled,
        )
    }
}
