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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.view.AccountList
import ua.com.radiokot.money.accounts.view.ViewAccountListItem
import ua.com.radiokot.money.categories.view.CategoryGrid
import ua.com.radiokot.money.categories.view.ViewCategoryListItem

@Composable
fun TransferCounterpartySelector(
    modifier: Modifier = Modifier,
    isForSource: Boolean?,
    accountItemList: State<List<ViewAccountListItem>>?,
    incomeCategoryItemList: State<List<ViewCategoryListItem>>?,
    expenseCategoryItemList: State<List<ViewCategoryListItem>>?,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onCategoryItemClicked: (ViewCategoryListItem) -> Unit,
) = Column(
    modifier = modifier
) {
    val pages: List<Page> = remember(
        accountItemList,
        incomeCategoryItemList,
        expenseCategoryItemList,
    ) {
        buildList {
            if (incomeCategoryItemList != null) {
                add(Page.Income)
            }
            if (expenseCategoryItemList != null) {
                add(Page.Expense)
            }
            if (accountItemList != null) {
                add(Page.Account)
            }
        }
    }
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(Page.Expense).coerceAtLeast(0),
        pageCount = pages::size,
    )
    val coroutineScope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (Page.Income in pages) {
            val pageIndex = pages.indexOf(Page.Income)
            BasicText(
                text = "Income",
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

        if (Page.Expense in pages) {
            val pageIndex = pages.indexOf(Page.Expense)
            BasicText(
                text = "Expense",
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

        if (Page.Account in pages) {
            val pageIndex = pages.indexOf(Page.Account)
            BasicText(
                text =
                if (isForSource == false)
                    "To account"
                else
                    "From account",
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

    Spacer(modifier = Modifier.height(12.dp))

    val noOpLongClick: (ViewCategoryListItem) -> Unit = remember { {} }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = pages.size - 1,
        verticalAlignment = Alignment.Top,
        key = Int::unaryPlus,
        modifier = Modifier
            .fillMaxWidth()
    ) { pageIndex ->
        when (pages[pageIndex]) {
            Page.Income -> {
                CategoryGrid(
                    itemList = incomeCategoryItemList!!,
                    onItemClicked = onCategoryItemClicked,
                    onItemLongClicked = noOpLongClick,
                )
            }

            Page.Expense -> {
                CategoryGrid(
                    itemList = expenseCategoryItemList!!,
                    onItemClicked = onCategoryItemClicked,
                    onItemLongClicked = noOpLongClick,
                )
            }

            Page.Account -> {
                AccountList(
                    itemList = accountItemList!!,
                    contentPadding = PaddingValues(
                        vertical = 8.dp,
                        horizontal = 16.dp,
                    ),
                    onAccountItemClicked = onAccountItemClicked,
                )
            }
        }
    }
}

private enum class Page {
    Income,
    Expense,
    Account,
}

@Composable
@Preview
private fun TransferCounterpartySelectorPreview(
) = TransferCounterpartySelector(
    isForSource = null,
    accountItemList = emptyList<ViewAccountListItem>().let(::mutableStateOf),
    incomeCategoryItemList = emptyList<ViewCategoryListItem>().let(::mutableStateOf),
    expenseCategoryItemList = emptyList<ViewCategoryListItem>().let(::mutableStateOf),
    onAccountItemClicked = {},
    onCategoryItemClicked = {},
)
