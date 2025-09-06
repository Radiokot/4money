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

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.composeunstyled.Icon
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.ItemIcon

@Composable
fun IconPicker(
    modifier: Modifier = Modifier,
    iconList: State<List<ItemIcon>>,
    selectedIcon: State<ItemIcon?>,
    onIconClicked: (ItemIcon?) -> Unit,
    contentPadding: PaddingValues = PaddingValues(8.dp),
) {
    val noIconAndIcons = remember {
        derivedStateOf {
            listOf<ItemIcon?>(null) + iconList.value
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
        modifier = modifier,
    ) {

        items(
            items = noIconAndIcons.value,
            key = { it?.name ?: "noicon" },
        ) { icon ->

            val isThisIconSelected = icon == selectedIcon.value

            BoxWithConstraints {
                val circleSize = min(maxWidth, maxHeight)

                val backgroundColor = animateColorAsState(
                    if (isThisIconSelected)
                        Color.DarkGray
                    else
                        Color(0xFFFBEBE7)
                )

                val iconColor = animateColorAsState(
                    if (isThisIconSelected)
                        Color.White
                    else
                        Color.DarkGray
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(circleSize)
                        .then(
                            if (icon != null)
                                Modifier.background(
                                    color = backgroundColor.value,
                                    shape = CircleShape,
                                )
                            else
                                Modifier.border(
                                    color = backgroundColor.value,
                                    width = circleSize * 0.08f,
                                    shape = CircleShape,
                                )
                        )
                        .clickable(
                            onClick = {
                                onIconClicked(icon)
                            },
                        )
                ) {
                    if (icon != null) {
                        Icon(
                            painter = painterResource(icon.resId),
                            contentDescription = icon.name,
                            tint = iconColor.value,
                            modifier = Modifier
                                .size(circleSize * 0.525f)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview(

) {

    val icons = DrawableResItemIconRepository().getItemIcons()
    val selectedIcon = remember { mutableStateOf<ItemIcon?>(null) }

    IconPicker(
        iconList = icons.let(::mutableStateOf),
        selectedIcon = selectedIcon,
        onIconClicked = { selectedIcon.value = it },
    )
}
