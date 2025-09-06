package ua.com.radiokot.money.categories.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import com.composeunstyled.Text
import ua.com.radiokot.money.colors.view.ItemLogo
import ua.com.radiokot.money.currency.view.ViewAmountFormat

@Composable
fun CategoryGrid(
    modifier: Modifier = Modifier,
    itemList: State<List<ViewCategoryListItem>>,
    onItemClicked: ((ViewCategoryListItem) -> Unit)? = null,
    onItemLongClicked: ((ViewCategoryListItem) -> Unit)? = null,
    isAddShown: Boolean,
    onAddClicked: (() -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    val space = 6.dp
    val visibleItemList = remember {
        derivedStateOf {
            itemList
                .value
                .fastFilter(ViewCategoryListItem::isNotArchived)
        }
    }
    val archiveItemList = remember {
        derivedStateOf {
            itemList
                .value
                .fastFilter(ViewCategoryListItem::isArchived)
        }
    }
    val isArchiveExpanded = remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        contentPadding = PaddingValues(
            vertical = space,
            horizontal = space,
        ),
        horizontalArrangement = Arrangement.spacedBy(space),
        verticalArrangement = Arrangement.spacedBy(space, Alignment.Top),
        state = gridState,
        modifier = modifier
    ) {
        categoryItems(
            itemList = visibleItemList,
            onItemClicked = onItemClicked,
            onItemLongClicked = onItemLongClicked,
        )

        if (isAddShown) {
            item(
                key = "add",
            ) {
                AddItem(
                    modifier = Modifier
                        .clickable(
                            onClick = { onAddClicked?.invoke() }
                        )
                )
            }
        }

        if (archiveItemList.value.isNotEmpty()) {
            item(
                key = "archive",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ArchiveHeader(
                    isArchiveExpanded = isArchiveExpanded,
                )
            }

            if (isArchiveExpanded.value) {
                categoryItems(
                    itemList = archiveItemList,
                    onItemClicked = onItemClicked,
                    onItemLongClicked = onItemLongClicked,
                )
            }
        }
    }
}

private fun LazyGridScope.categoryItems(
    itemList: State<List<ViewCategoryListItem>>,
    onItemClicked: ((ViewCategoryListItem) -> Unit)?,
    onItemLongClicked: ((ViewCategoryListItem) -> Unit)?,
) {
    items(
        items = itemList.value,
        ViewCategoryListItem::key,
    ) { item ->
        CategoryListItem(
            item = item,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        onItemClicked?.invoke(item)
                    },
                    onLongClick = {
                        onItemLongClicked?.invoke(item)
                    },
                )
        )
    }
}

@Composable
private fun ArchiveHeader(
    isArchiveExpanded: MutableState<Boolean>,
) {
    Row(
        modifier = Modifier
            .clickable(
                onClick = {
                    isArchiveExpanded.value = !isArchiveExpanded.value
                },
            )
            .padding(
                vertical = 10.dp,
                horizontal = 8.dp,
            )
            .fillMaxWidth()
    ) {
        Text(
            text = "Archive",
            fontSize = 16.sp,
            fontWeight = FontWeight(500),
            modifier = Modifier
                .weight(1f),
        )

        Text(
            text =
                if (isArchiveExpanded.value)
                    "🔼"
                else
                    "🔽",
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun CategoryListItem(
    modifier: Modifier = Modifier,
    item: ViewCategoryListItem,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier,
) {
    val title = item.title
    val amount = item.amount

    BasicText(
        text = title,
        style = TextStyle(
            textAlign = TextAlign.Center,
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(4.dp))

    ItemLogo(
        title = title,
        colorScheme = item.colorScheme,
        icon = item.icon,
        shape = CircleShape,
        modifier = Modifier
            .size(LOGO_SIZE_DP.dp)
    )

    Spacer(modifier = Modifier.height(4.dp))

    if (!item.isIncognito) {
        val locale = LocalConfiguration.current.locales[0]
        val amountFormat = remember(locale) {
            ViewAmountFormat(locale)
        }

        BasicText(
            text = amountFormat(amount),
            style = TextStyle(
                textAlign = TextAlign.Center,
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
        )
    } else {
        BasicText(
            text = amount.currency.symbol,
            style = TextStyle(
                textAlign = TextAlign.Center,
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
private fun AddItem(
    modifier: Modifier = Modifier,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier,
) {

    Text(
        text = "",
        modifier = Modifier
            .drawWithContent { }
    )

    Spacer(modifier = Modifier.height(4.dp))

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(LOGO_SIZE_DP.dp)
            .border(
                width = 1.dp,
                color = Color.DarkGray,
                shape = CircleShape,
            )
    ) {
        Text(
            text = "➕",
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "",
        modifier = Modifier
            .drawWithContent { }
    )
}

@Composable
@Preview(
    apiLevel = 34,
    widthDp = 200,
)
private fun CategoryGridPreview(
) {
    val itemList = ViewCategoryListItemPreviewParameterProvider()
        .values
        .toList()

    CategoryGrid(
        itemList = itemList.let(::mutableStateOf),
        onItemClicked = {},
        onItemLongClicked = {},
        isAddShown = true,
        onAddClicked = {},
        modifier = Modifier
            .fillMaxWidth()
    )
}

private const val LOGO_SIZE_DP = 52
