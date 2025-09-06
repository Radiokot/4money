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

package ua.com.radiokot.money.accounts.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.view.ItemLogo
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider
import java.math.BigInteger

@Composable
fun AccountList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        vertical = 8.dp,
    ),
    itemList: State<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
) = LazyColumn(
    contentPadding = contentPadding,
    modifier = modifier,
) {
    items(
        items = itemList.value,
        key = ViewAccountListItem::key,
        contentType = ViewAccountListItem::type,
    ) { item ->
        when (item) {
            is ViewAccountListItem.Header -> {
                HeaderItem(
                    title = item.title,
                    amount = item.amount,
                    modifier = Modifier
                        .padding(
                            vertical = 8.dp,
                        )
                        .fillMaxWidth()
                )
            }

            is ViewAccountListItem.Account -> {
                AccountItem(
                    item = item,
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                onAccountItemClicked(item)
                            },
                        )
                        .padding(
                            vertical = 8.dp,
                        )
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MovableAccountList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        vertical = 8.dp,
    ),
    itemList: State<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onAccountItemMoved: (
        itemToMove: ViewAccountListItem.Account,
        itemToPlaceBefore: ViewAccountListItem.Account?,
        itemToPlaceAfter: ViewAccountListItem.Account?,
    ) -> Unit,
    bottomContent: (LazyListScope.() -> Unit)? = null,
) {
    val movableItemList = remember {
        mutableStateListOf<ViewAccountListItem>()
    }
    var itemToMove by remember {
        mutableStateOf<ViewAccountListItem.Account?>(null)
    }
    var itemToPlaceBefore by remember {
        mutableStateOf<ViewAccountListItem.Account?>(null)
    }
    var itemToPlaceAfter by remember {
        mutableStateOf<ViewAccountListItem.Account?>(null)
    }
    var skipOriginalListUpdates by remember {
        mutableIntStateOf(0)
    }
    val currentItemList = remember {
        derivedStateOf {
            itemList.value
                .takeUnless { skipOriginalListUpdates-- > 0 }
                ?: movableItemList
        }
    }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            if (itemToMove == null) {
                itemToMove = movableItemList[from.index]
                        as? ViewAccountListItem.Account
            }
            movableItemList.add(
                to.index,
                movableItemList.removeAt(from.index),
            )
            itemToPlaceBefore = movableItemList.getOrNull(to.index + 1)
                    as? ViewAccountListItem.Account
            itemToPlaceAfter = movableItemList.getOrNull(to.index - 1)
                    as? ViewAccountListItem.Account
        },
    )

    LazyColumn(
        contentPadding = contentPadding,
        state = listState,
        modifier = modifier,
    ) {
        items(
            items = currentItemList.value,
            key = ViewAccountListItem::key,
            contentType = ViewAccountListItem::type,
        ) { item ->
            when (item) {
                is ViewAccountListItem.Header -> {
                    HeaderItem(
                        title = item.title,
                        amount = item.amount,
                        modifier = Modifier
                            .padding(
                                vertical = 8.dp,
                            )
                            .fillMaxWidth()
                    )
                }

                is ViewAccountListItem.Account -> {
                    ReorderableItem(
                        state = reorderableState,
                        key = item.key,
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    onAccountItemClicked(item)
                                },
                            )
                    ) { isDragging ->
                        AccountItem(
                            item = item,
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        movableItemList.clear()
                                        movableItemList.addAll(itemList.value)
                                        skipOriginalListUpdates = Int.MAX_VALUE
                                        itemToMove = null
                                    },
                                    onDragStopped = {
                                        if (itemToMove != null) {
                                            skipOriginalListUpdates = 1
                                            onAccountItemMoved(
                                                itemToMove!!,
                                                itemToPlaceBefore,
                                                itemToPlaceAfter,
                                            )
                                        } else {
                                            skipOriginalListUpdates = 0
                                        }
                                    },
                                )
                                .graphicsLayer {
                                    alpha =
                                        if (isDragging)
                                            0.7f
                                        else
                                            1f
                                }
                                .padding(
                                    top = 8.dp,
                                    bottom = 12.dp,
                                )
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        bottomContent?.invoke(this)
    }
}

@Composable
@Preview(
    widthDp = 200
)
private fun AccountListPreview() {
    val colorSchemesByName = HardcodedItemColorSchemeRepository()
        .getItemColorSchemesByName()
    val iconsByName = DrawableResItemIconRepository()
        .getItemIconsByName()

    AccountList(
        itemList = listOf(
            ViewAccountListItem.Header(
                title = "Accounts",
                amount = ViewAmount(
                    value = BigInteger("10000"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    ),
                ),
                key = "header1",
            ),
            ViewAccountListItem.Account(
                title = "Account #1",
                balance = ViewAmount(
                    value = BigInteger("7500"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    ),
                ),
                isIncognito = false,
                colorScheme = colorSchemesByName.getValue("Purple1"),
                icon = null,
                key = "acc1",
            ),
            ViewAccountListItem.Account(
                title = "Account #2",
                balance = ViewAmount(
                    value = BigInteger("2500"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    ),
                ),
                isIncognito = false,
                colorScheme = colorSchemesByName.getValue("Red4"),
                icon = null,
                key = "acc2",
            ),
            ViewAccountListItem.Header(
                title = "Savings",
                amount = ViewAmount(
                    value = BigInteger("9900000"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    ),
                ),
                key = "header2",
            ),
            ViewAccountListItem.Account(
                title = "Account #3",
                balance = ViewAmount(
                    value = BigInteger("100000000"),
                    currency = ViewCurrency(
                        symbol = "₿",
                        precision = 8,
                    ),
                ),
                isIncognito = true,
                colorScheme = colorSchemesByName.getValue("Green3"),
                icon = iconsByName["finances_34"],
                key = "acc3",
            ),
        ).let(::mutableStateOf),
        onAccountItemClicked = {},
    )
}

@Composable
private fun HeaderItem(
    modifier: Modifier = Modifier,
    title: String,
    amount: ViewAmount?,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight(500)
            ),
            modifier = Modifier
                .weight(1f),
        )

        if (amount != null) {
            val locale = LocalConfiguration.current.locales[0]
            val amountFormat = remember(locale) {
                ViewAmountFormat(locale)
            }

            BasicText(
                text = amountFormat(amount),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 18.sp,
                ),
            )
        }
    }
}

@Composable
@Preview(
    widthDp = 140,
)
private fun HeaderItemPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class) amount: ViewAmount,
) {
    HeaderItem(
        title = "Savings",
        amount = amount,
    )
}

@Composable
private fun AccountItem(
    modifier: Modifier = Modifier,
    item: ViewAccountListItem.Account,
) = Row(
    modifier = modifier,
) {

    ItemLogo(
        title = item.title,
        colorScheme = item.colorScheme,
        icon = item.icon,
        modifier = Modifier
            .size(38.dp)
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column(
        modifier = Modifier
            .heightIn(
                min = 38.dp,
            )
    ) {
        BasicText(
            text = item.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 14.sp,
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (!item.isIncognito) {
            val locale = LocalConfiguration.current.locales[0]
            val amountFormat = remember(locale) {
                ViewAmountFormat(locale)
            }

            BasicText(
                text = amountFormat(item.balance),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 16.sp,
                ),
            )
        } else {
            BasicText(
                text = item.balance.currency.symbol,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 16.sp,
                ),
            )
        }
    }
}
