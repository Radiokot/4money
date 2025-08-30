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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme

@Composable
fun ColorSchemePicker(
    modifier: Modifier = Modifier,
    colorSchemeList: State<List<ItemColorScheme>>,
    selectedColorScheme: State<ItemColorScheme>,
    onColorSchemeClicked: (ItemColorScheme) -> Unit,
    contentPadding: PaddingValues = PaddingValues(8.dp),
) {
    val selectionIndicatorEnterTransition = remember {
        scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
            )
        )
    }
    val selectionIndicatorExitTransition = remember {
        scaleOut()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
        modifier = modifier,
    ) {

        items(
            items = colorSchemeList.value,
            key = ItemColorScheme::name,
        ) { colorScheme ->

            BoxWithConstraints {
                val circleSize = min(maxWidth, maxHeight)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(circleSize)
                        .background(
                            color = Color(colorScheme.primary),
                            shape = CircleShape,
                        )
                        .clickable(
                            onClick = {
                                onColorSchemeClicked(colorScheme)
                            },
                        )
                ) {

                    AnimatedVisibility(
                        visible = selectedColorScheme.value == colorScheme,
                        enter = selectionIndicatorEnterTransition,
                        exit = selectionIndicatorExitTransition,
                        label = "selection-indicator",
                    ) {
                        Box(
                            modifier = Modifier
                                .size(circleSize * 0.8f)
                                .border(
                                    width = circleSize * 0.08f,
                                    color = Color(colorScheme.onPrimary),
                                    shape = CircleShape,
                                )
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {

    val itemColorSchemeRepository = HardcodedItemColorSchemeRepository()

    ColorSchemePicker(
        colorSchemeList = itemColorSchemeRepository.getItemColorSchemes().let(::mutableStateOf),
        selectedColorScheme = itemColorSchemeRepository.getItemColorSchemes()[7].let(::mutableStateOf),
        onColorSchemeClicked = {},
    )
}
