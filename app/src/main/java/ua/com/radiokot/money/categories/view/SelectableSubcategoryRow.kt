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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ua.com.radiokot.money.stableClickable

@Composable
fun SelectableSubcategoryRow(
    modifier: Modifier = Modifier,
    itemList: State<List<ViewSelectableSubcategoryListItem>>,
    onItemClicked: (ViewSelectableSubcategoryListItem) -> Unit,
) {
    val rowState = rememberLazyListState()
    val space = 12.dp

    LazyRow(
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(space, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(
            horizontal = space,
        ),
        modifier = modifier
    ) {

        items(
            items = itemList.value,
            key = ViewSelectableSubcategoryListItem::key,
        ) { item ->
            SelectableSubcategoryListItem(
                item = item,
                modifier = Modifier
                    .stableClickable(
                        key = item.key,
                        onClick = { onItemClicked(item) },
                    )
            )
        }
    }
}

@Composable
@Preview(
    widthDp = 300,
)
private fun SelectableSubcategoryRowPreview() {
    val items = ViewSelectableSubcategoryListItemPreviewParameterProvider().values.toList()

    SelectableSubcategoryRow(
        itemList = items.let(::mutableStateOf),
        onItemClicked = {},
    )
}

@Composable
private fun SelectableSubcategoryListItem(
    modifier: Modifier = Modifier,
    item: ViewSelectableSubcategoryListItem,
) {
    val primaryColor = Color(0xff67ad5b)
    val shape = RoundedCornerShape(
        percent = 50,
    )

    BasicText(
        text = item.title,
        style = TextStyle(
            color =
            if (item.isSelected)
                Color.White
            else
                primaryColor
        ),
        modifier = modifier
            .run {
                if (item.isSelected)
                    background(
                        color = primaryColor,
                        shape = shape,
                    )
                else
                    border(
                        width = 1.dp,
                        color = primaryColor,
                        shape = shape,
                    )
            }
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            )
    )
}

@Composable
@Preview
private fun SelectableSubcategoryListItemPreview(
    @PreviewParameter(ViewSelectableSubcategoryListItemPreviewParameterProvider::class) item: ViewSelectableSubcategoryListItem,
) = SelectableSubcategoryListItem(item = item)
