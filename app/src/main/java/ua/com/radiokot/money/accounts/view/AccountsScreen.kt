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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.uikit.TextButton

@Composable
fun AccountsScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel,
) = AccountsScreen(
    modifier = modifier,
    accountItemList = viewModel.accountListItems.collectAsState(),
    onAccountItemClicked = viewModel::onAccountItemClicked,
    onAccountItemMoved = viewModel::onAccountItemMoved,
    totalAmountPerCurrencyList = viewModel.totalAmountsPerCurrency.collectAsState(),
    totalAmount = viewModel.totalAmount.collectAsState(),
    onAddClicked = remember { viewModel::onAddClicked },
)

@Composable
private fun AccountsScreen(
    modifier: Modifier = Modifier,
    accountItemList: State<List<ViewAccountListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onAccountItemMoved: (
        itemToMove: ViewAccountListItem.Account,
        itemToPlaceBefore: ViewAccountListItem.Account?,
        itemToPlaceAfter: ViewAccountListItem.Account?,
    ) -> Unit,
    totalAmountPerCurrencyList: State<List<ViewAmount>>,
    totalAmount: State<ViewAmount?>,
    onAddClicked: () -> Unit,
) = Column(
    modifier = modifier
) {

    val pages: List<Page> = remember {
        listOf(
            Page.All,
            Page.Total,
        )
    }
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(Page.All).coerceAtLeast(0),
        pageCount = pages::size,
    )
    val coroutineScope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 22.dp,
                vertical = 16.dp,
            )
    ) {
        TextButton(
            text = "💨",
            padding = PaddingValues(6.dp),
            modifier = Modifier
                .drawWithContent {
                    // Keep this button only for space.
                }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .weight(1f)
        ) {
            if (Page.All in pages) {
                val pageIndex = pages.indexOf(Page.All)
                BasicText(
                    text = "Accounts",
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        textDecoration =
                        if (pagerState.currentPage == pageIndex)
                            TextDecoration.Underline
                        else
                            null,
                    ),
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = pageIndex,
                                )
                            }
                        }
                )
            }

            if (Page.Total in pages) {
                val pageIndex = pages.indexOf(Page.Total)
                BasicText(
                    text = "Total",
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        textDecoration =
                        if (pagerState.currentPage == pageIndex)
                            TextDecoration.Underline
                        else
                            null,
                    ),
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = pageIndex,
                                )
                            }
                        }
                )
            }
        }

        TextButton(
            text = "➕",
            padding = PaddingValues(6.dp),
            modifier = Modifier
                .clickable(
                    onClick = onAddClicked,
                )
        )
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = pages.size - 1,
        verticalAlignment = Alignment.Top,
        key = Int::unaryPlus,
        modifier = Modifier
            .fillMaxSize()
    ) { pageIndex ->
        when (pages[pageIndex]) {
            Page.All ->
                MovableAccountList(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                        ),
                    itemList = accountItemList,
                    onAccountItemClicked = onAccountItemClicked,
                    onAccountItemMoved = onAccountItemMoved,
                )

            Page.Total ->
                TotalPage(
                    amountPerCurrencyList = totalAmountPerCurrencyList,
                    totalAmount = totalAmount,
                )
        }
    }

    MovableAccountList(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
            ),
        itemList = accountItemList,
        onAccountItemClicked = onAccountItemClicked,
        onAccountItemMoved = onAccountItemMoved,
    )
}

@Composable
private fun TotalPage(
    modifier: Modifier = Modifier,
    amountPerCurrencyList: State<List<ViewAmount>>,
    totalAmount: State<ViewAmount?>,
) = Column(
    modifier = modifier
        .padding(
            horizontal = 16.dp,
        )
) {
    val locale = LocalConfiguration.current.locales[0]
    val amountFormat = remember(locale) {
        ViewAmountFormat(locale)
    }

    Row {
        val textStyle = remember {
            TextStyle(
                fontSize = 18.sp,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(IntrinsicSize.Max)
        ) {
            amountPerCurrencyList.value.forEach { amount ->
                key(amount.currency) {
                    BasicText(
                        text = amount.currency.symbol,
                        style = textStyle,
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp,
                            )
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .weight(1f)
        ) {
            amountPerCurrencyList.value.forEach { amount ->
                key(amount.currency) {
                    BasicText(
                        text = amountFormat(amount),
                        style = textStyle,
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp,
                            )
                    )
                }
            }
        }
    }

    val isTotalAmountVisible by remember {
        derivedStateOf {
            totalAmount.value != null
        }
    }

    if (isTotalAmountVisible) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = amountFormat(totalAmount.value!!),
            style = TextStyle(
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

private enum class Page {
    All,
    Total,
}
