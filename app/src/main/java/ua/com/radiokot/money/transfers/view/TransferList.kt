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

package ua.com.radiokot.money.transfers.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat

@Composable
fun TransferList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    itemPagingFlow: Flow<PagingData<ViewTransferListItem>>,
    onTransferItemClicked: (ViewTransferListItem.Transfer) -> Unit,
    onTransferItemLongClicked: (ViewTransferListItem.Transfer) -> Unit,
) {
    val locale = LocalConfiguration.current.locales.get(0)
    val amountFormat = remember(locale) {
        ViewAmountFormat(locale)
    }
    val dayFormat = remember(locale) {
        LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
        }
    }
    val monthYearFormat = remember(locale) {
        LocalDate.Format {
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            year()
        }
    }
    val lazyPagingItems = itemPagingFlow.collectAsLazyPagingItems()

    LazyColumn(
        contentPadding = PaddingValues(
            vertical = 16.dp,
        ),
        state = state,
        modifier = modifier,
    ) {
        items(
            lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey(ViewTransferListItem::key),
            contentType = lazyPagingItems.itemContentType(ViewTransferListItem::itemType),
        ) { itemIndex ->
            when (val item = lazyPagingItems[itemIndex]) {
                is ViewTransferListItem.Header -> {
                    HeaderItem(
                        item = item,
                        dayFormat = dayFormat,
                        monthYearFormat = monthYearFormat,
                        modifier = Modifier
                            .padding(
                                bottom = 16.dp,
                            )
                    )
                }

                is ViewTransferListItem.Transfer -> {
                    val clickableModifier = remember(item) {
                        // Long click – experimental 🤡.
                        @OptIn(ExperimentalFoundationApi::class)
                        Modifier.combinedClickable(
                            onClick = {
                                onTransferItemClicked(item)
                            },
                            onLongClick = {
                                onTransferItemLongClicked(item)
                            },
                        )
                    }

                    TransferItem(
                        item = item,
                        amountFormat = amountFormat,
                        modifier = clickableModifier
                            .padding(
                                bottom = 16.dp
                            )
                    )
                }

                null ->
                    BasicText(text = "Loading $itemIndex")
            }
        }
    }
}

@Composable
@Preview(
    widthDp = 180,
)
private fun TransferListPreview(
    @PreviewParameter(ViewTransferItemListPreviewParameterProvider::class) itemList: List<ViewTransferListItem>,
) = TransferList(
    itemPagingFlow = flowOf(PagingData.from(itemList)),
    onTransferItemClicked = {},
    onTransferItemLongClicked = {},
)

@Composable
private fun HeaderItem(
    modifier: Modifier = Modifier,
    item: ViewTransferListItem.Header,
    dayFormat: DateTimeFormat<LocalDate>,
    monthYearFormat: DateTimeFormat<LocalDate>,
) = Row(
    verticalAlignment = Alignment.Bottom,
    modifier = modifier
        .fillMaxWidth(),
) {
    BasicText(
        text = item.date.localDate.day.toString(),
        style = TextStyle(
            fontSize = 30.sp,
            fontWeight = FontWeight(300),
        ),
        modifier = Modifier.alignByBaseline()
    )

    Spacer(modifier = Modifier.width(4.dp))

    Column(
        modifier = Modifier
            .alignBy(LastBaseline),
    ) {
        BasicText(
            text =
            when (item.date.specificType) {
                ViewDate.SpecificType.Today ->
                    "Today"

                ViewDate.SpecificType.Yesterday ->
                    "Yesterday"

                null ->
                    dayFormat.format(item.date.localDate)
            },
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight(200)
            ),
        )
        BasicText(
            text = monthYearFormat.format(item.date.localDate),
            style = TextStyle(
                fontSize = 14.sp,
            ),
        )
    }
}

@Composable
private fun TransferItem(
    modifier: Modifier = Modifier,
    item: ViewTransferListItem.Transfer,
    amountFormat: ViewAmountFormat,
) = Column(
    modifier = modifier
        .fillMaxWidth(),
) {
    val amountColor = remember {
        when (item.type) {
            ViewTransferListItem.Transfer.Type.Income ->
                Color(0xff50af99)

            ViewTransferListItem.Transfer.Type.Expense ->
                Color(0xffd85e8c)

            ViewTransferListItem.Transfer.Type.Other ->
                Color(0xff757575)
        }
    }

    Row(
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            text = item.primaryCounterparty.title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TextStyle(
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )

        BasicText(
            text = amountFormat(
                amount = ViewAmount(
                    value = item.primaryAmount,
                    currency = item.primaryCounterparty.currency,
                ),
                customColor = amountColor,
            ),
            style = TextStyle(
                fontSize = 18.sp,
            ),
            modifier = Modifier
                .alignBy(LastBaseline),
        )
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(
                top = 2.dp,
            )
    ) {
        BasicText(
            text = item.secondaryCounterparty.title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TextStyle(
                fontSize = 14.sp,
            ),
            modifier = Modifier.weight(1f),
        )

        if (item.primaryCounterparty.currency != item.secondaryCounterparty.currency) {
            BasicText(
                text = amountFormat(
                    amount = ViewAmount(
                        value = item.secondaryAmount,
                        currency = item.secondaryCounterparty.currency,
                    ),
                    customColor = amountColor,
                ),
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )
        }
    }

    if (item.memo != null) {
        BasicText(
            text = item.memo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = Color.Gray,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
            ),
            modifier = Modifier
                .padding(
                    top = 2.dp,
                )
        )
    }
}
