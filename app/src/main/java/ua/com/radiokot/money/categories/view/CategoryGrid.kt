package ua.com.radiokot.money.categories.view

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.com.radiokot.money.colors.view.ItemLogo
import ua.com.radiokot.money.currency.view.ViewAmountFormat

@Composable
fun CategoryGrid(
    modifier: Modifier = Modifier,
    itemList: State<List<ViewCategoryListItem>>,
    onItemClicked: (ViewCategoryListItem) -> Unit,
    onItemLongClicked: (ViewCategoryListItem) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val space = 6.dp

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
        items(
            items = itemList.value,
            key = ViewCategoryListItem::key,
        ) { item ->
            CategoryListItem(
                item = item,
                modifier = Modifier
                    .combinedClickable(
                        onClick = { onItemClicked(item) },
                        onLongClick = { onItemLongClicked(item) },
                    )
            )
        }
    }
}

@Composable
@Preview(
    apiLevel = 34,
    widthDp = 200,
)
private fun CategoryGridPreview(
) {
    CategoryGrid(
        itemList = ViewCategoryListItemPreviewParameterProvider()
            .values
            .toList()
            .let(::mutableStateOf),
        onItemClicked = {},
        onItemLongClicked = {},
        modifier = Modifier
            .fillMaxWidth()
    )
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
        shape = CircleShape,
        modifier = Modifier
            .size(52.dp)
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
