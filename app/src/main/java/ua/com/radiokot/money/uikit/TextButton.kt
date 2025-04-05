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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    text: String,
    isEnabled: Boolean = true,
    padding: PaddingValues = PaddingValues(12.dp),
) {
    val shape = remember {
        RoundedCornerShape(12.dp)
    }
    val color = remember(isEnabled) {
        if (isEnabled)
            Color.DarkGray
        else
            Color.LightGray
    }

    BasicText(
        text = text,
        style = TextStyle(
            textAlign = TextAlign.Center,
            color = color,
        ),
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .border(
                width = 1.dp,
                color = color,
                shape = shape,
            )
            .padding(padding)
    )
}

@Composable
@Preview
private fun TextButtonPreview(
) = Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    TextButton(
        text = "A button :)",
    )

    TextButton(
        text = "I am disabled",
        isEnabled = false,
    )
}
