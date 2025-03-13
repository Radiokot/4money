package ua.com.radiokot.money.categories.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.stableClickable

@Composable
fun CategoryGrid(
    modifier: Modifier = Modifier,
    itemList: State<List<ViewCategoryListItem>>,
    onItemClicked: (ViewCategoryListItem) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val space = 12.dp

    LazyVerticalGrid(
        columns = GridCells.Adaptive(64.dp),
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
    widthDp = 160,
)
private fun CategoryGridPreview(
) {
    CategoryGrid(
        itemList = ViewCategoryListItemPreviewParameterProvider()
            .values
            .toList()
            .let(::mutableStateOf),
        onItemClicked = {},
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

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .background(
                color = Color(0xfff0f2eb),
                shape = CircleShape,
            )
    ) {
        val fontSizeSp = (maxWidth * 0.5f).value.sp / LocalDensity.current.fontScale
        val firstSymbol = remember(title) {
            val firstCodepoint = title.codePoints()
                .findFirst()
                .orElse(8230) // …
            String(intArrayOf(firstCodepoint), 0, 1)
        }

        BasicText(
            text = firstSymbol,
            style = TextStyle(
                color = Color(0xffa6cb72),
                fontSize = fontSizeSp,
            )
        )
    }

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
@Preview(
    widthDp = 80,
)
private fun CategoryListItemPreview(
    @PreviewParameter(ViewCategoryListItemPreviewParameterProvider::class) item: ViewCategoryListItem,
) = CategoryListItem(
    item = item,
    modifier = Modifier
        .fillMaxWidth()
)
