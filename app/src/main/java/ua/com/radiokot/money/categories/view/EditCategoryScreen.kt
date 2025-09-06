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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.colors.view.ItemLogo
import ua.com.radiokot.money.uikit.RedToggleSwitch
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun EditCategoryScreen(
    isNewCategory: Boolean,
    isIncome: Boolean,
    isSaveEnabled: State<Boolean>,
    onSaveClicked: () -> Unit,
    title: State<String>,
    onTitleChanged: (String) -> Unit,
    colorScheme: State<ItemColorScheme>,
    icon: State<ItemIcon?>,
    onLogoClicked: () -> Unit,
    currencyCode: State<String>,
    isCurrencyChangeEnabled: Boolean,
    onCurrencyClicked: () -> Unit,
    subcategoryItemList: State<List<ViewSubcategoryToUpdateListItem>>,
    isSubcategoryEditEnabled: State<Boolean>,
    onSubcategoryItemClicked: (ViewSubcategoryToUpdateListItem) -> Unit,
    onSubcategoryItemMoved: suspend (fromIndex: Int, toIndex: Int) -> Unit,
    onAddSubcategoryClicked: () -> Unit,
    isArchived: State<Boolean>,
    isArchivedVisible: Boolean,
    onArchivedClicked: () -> Unit,
    onCloseClicked: () -> Unit,
) = Column(
    modifier = Modifier
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .add(WindowInsets.statusBars)
        )
        .padding(
            horizontal = 16.dp,
        )
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 56.dp,
            )
    ) {
        val buttonPadding = remember {
            PaddingValues(6.dp)
        }

        TextButton(
            text = "❌",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onCloseClicked,
                )
        )

        Text(
            text =
                if (isNewCategory)
                    "New " + (if (isIncome) "income" else "expense") + " category"
                else
                    "Edit " + (if (isIncome) "income" else "expense") + " category",
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )

        TextButton(
            text = "✔️",
            isEnabled = isSaveEnabled.value,
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    enabled = isSaveEnabled.value,
                    onClick = onSaveClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    val subcategoryListState = rememberLazyListState()
    val subcategoryReorderableState = rememberReorderableLazyListState(
        lazyListState = subcategoryListState,
        onMove = { from, to ->
            // 1 is subtracted because the first item in the column
            // is the section with title and currency.
            onSubcategoryItemMoved(
                from.index - 1,
                to.index - 1,
            )
        },
    )

    LazyColumn(
        state = subcategoryListState,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        item {
            LogoAndTitleRow(
                title = title,
                onTitleChanged = onTitleChanged,
                colorScheme = colorScheme,
                icon = icon,
                onLogoClicked = onLogoClicked,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Currency",
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color =
                            if (isCurrencyChangeEnabled)
                                Color.DarkGray
                            else
                                Color.Gray,
                    )
                    .clickable(
                        enabled = isCurrencyChangeEnabled,
                        onClick = onCurrencyClicked,
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = currencyCode.value,
                    color =
                        if (isCurrencyChangeEnabled)
                            Color.Unspecified
                        else
                            Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                )

                if (isCurrencyChangeEnabled) {
                    Text(text = "▶️")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Subcategories",
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        items(
            items = subcategoryItemList.value,
            key = ViewSubcategoryToUpdateListItem::key,
        ) { item ->

            ReorderableItem(
                state = subcategoryReorderableState,
                key = item.key,
                modifier = Modifier
                    .clickable(
                        enabled = isSubcategoryEditEnabled.value,
                        onClick = {
                            onSubcategoryItemClicked(item)
                        },
                    )
            ) { isDragging ->
                Text(
                    text = item.title,
                    color =
                        if (isSubcategoryEditEnabled.value)
                            Color.Unspecified
                        else
                            Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .longPressDraggableHandle(
                            enabled = isSubcategoryEditEnabled.value,
                        )
                        .padding(
                            vertical = 12.dp,
                        )
                        .graphicsLayer {
                            alpha =
                                if (isDragging)
                                    0.7f
                                else
                                    1f
                        }
                )
            }
        }

        item(
            key = "add-subcategory",
        ) {
            Text(
                text = "➕ Add subcategory",
                color =
                    if (isSubcategoryEditEnabled.value)
                        Color.Unspecified
                    else
                        Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = isSubcategoryEditEnabled.value,
                        onClick = onAddSubcategoryClicked,
                    )
                    .padding(
                        vertical = 12.dp,
                    )
            )
        }

        if (isArchivedVisible) {
            item(
                key = "archived"
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            onClick = onArchivedClicked,
                        )
                        .padding(
                            vertical = 12.dp,
                        )
                ) {
                    Text(
                        text = "Archived",
                        modifier = Modifier
                            .weight(1f)
                    )

                    RedToggleSwitch(
                        isToggled = isArchived,
                        onToggled = { onArchivedClicked() }
                    )
                }
            }

        }

        item(
            key = "save",
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                text = "Save",
                isEnabled = isSaveEnabled.value,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .clickable(
                        enabled = isSaveEnabled.value,
                        onClick = onSaveClicked,
                    )
            )
        }
    }
}

@Composable
private fun LogoAndTitleRow(
    modifier: Modifier = Modifier,
    title: State<String>,
    onTitleChanged: (String) -> Unit,
    colorScheme: State<ItemColorScheme>,
    icon: State<ItemIcon?>,
    onLogoClicked: () -> Unit,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.Bottom,
) {
    Column(
        modifier = Modifier
            .weight(1f)
    ) {
        Text(
            text = "Title",
        )

        Spacer(modifier = Modifier.height(6.dp))

        BasicTextField(
            value = title.value,
            onValueChange = onTitleChanged,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.DarkGray,
                )
                .padding(12.dp)
        )
    }

    ItemLogo(
        title = title.value,
        colorScheme = colorScheme.value,
        icon = icon.value,
        modifier = Modifier
            .padding(
                start = 16.dp,
            )
            .size(42.dp)
            .clickable(
                onClick = onLogoClicked,
            )
    )
}

@Composable
fun EditCategoryScreenRoot(
    viewModel: EditCategoryScreenViewModel,
) {
    EditCategoryScreen(
        isNewCategory = viewModel.isNewCategory,
        isIncome = viewModel.isIncome,
        isSaveEnabled = viewModel.isSaveEnabled.collectAsState(),
        onSaveClicked = remember { viewModel::onSaveClicked },
        title = viewModel.title.collectAsState(),
        onTitleChanged = remember { viewModel::onTitleChanged },
        colorScheme = viewModel.colorScheme.collectAsState(),
        icon = viewModel.icon.collectAsState(),
        onLogoClicked = remember { viewModel::onLogoClicked },
        currencyCode = viewModel.currencyCode.collectAsState(),
        isCurrencyChangeEnabled = viewModel.isCurrencyChangeEnabled,
        onCurrencyClicked = remember { viewModel::onCurrencyClicked },
        subcategoryItemList = viewModel.subcategories.collectAsState(),
        isSubcategoryEditEnabled = viewModel.isSubcategoryEditEnabled.collectAsState(),
        onSubcategoryItemClicked = remember { viewModel::onSubcategoryItemClicked },
        onSubcategoryItemMoved = remember { viewModel::onSubcategoryItemMoved },
        onAddSubcategoryClicked = remember { viewModel::onAddSubcategoryClicked },
        isArchived = viewModel.isArchived.collectAsState(),
        isArchivedVisible = viewModel.isArchivedVisible,
        onArchivedClicked = remember { viewModel::onArchivedClicked },
        onCloseClicked = remember { viewModel::onCloseClicked },
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {
    val isArchived = remember { mutableStateOf(false) }
    val isSubcategoryEditEnabled = remember {
        derivedStateOf {
            !isArchived.value
        }
    }

    EditCategoryScreen(
        isNewCategory = true,
        isIncome = false,
        isSaveEnabled = false.let(::mutableStateOf),
        onSaveClicked = {},
        title = "Hobbies".let(::mutableStateOf),
        onTitleChanged = {},
        colorScheme = HardcodedItemColorSchemeRepository()
            .getItemColorSchemesByName()
            .getValue("Pink3")
            .let(::mutableStateOf),
        icon = null.let(::mutableStateOf),
        onLogoClicked = {},
        currencyCode = "USD".let(::mutableStateOf),
        isCurrencyChangeEnabled = true,
        onCurrencyClicked = {},
        subcategoryItemList =
            listOf(
                ViewSubcategoryToUpdateListItem(
                    title = "Koshka",
                    source = null,
                ),
                ViewSubcategoryToUpdateListItem(
                    title = "Kartoshka",
                    source = null,
                )
            ).let(::mutableStateOf),
        isSubcategoryEditEnabled = isSubcategoryEditEnabled,
        onSubcategoryItemClicked = {},
        onSubcategoryItemMoved = { _, _ -> },
        onAddSubcategoryClicked = {},
        isArchived = isArchived,
        isArchivedVisible = true,
        onArchivedClicked = { isArchived.value = !isArchived.value },
        onCloseClicked = {},
    )
}
