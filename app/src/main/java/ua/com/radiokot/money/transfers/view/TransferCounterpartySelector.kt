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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.com.radiokot.money.accounts.view.AccountList
import ua.com.radiokot.money.accounts.view.ViewAccountListItem
import ua.com.radiokot.money.categories.view.CategoryGrid
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.categories.view.ViewCategoryListItemPreviewParameterProvider
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider

@Composable
fun TransferCounterpartySelector(
    modifier: Modifier = Modifier,
    isForSource: Boolean,
    accountItemList: State<List<ViewAccountListItem>>,
    categoryItemList: State<List<ViewCategoryListItem>>?,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onCategoryItemClicked: (ViewCategoryListItem) -> Unit,
) = Column(
    modifier = modifier
) {
    val showCategories = categoryItemList != null
    val pagerState = rememberPagerState(
        pageCount = { if (showCategories) 2 else 1 },
    )
    val coroutineScope = rememberCoroutineScope()
    val scrollToFirstPageOnClickModifier = remember {
        Modifier.clickable {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = 0,
                )
            }
        }
    }
    val scrollToLastPageOnClickModifier = remember {
        Modifier.clickable {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = pagerState.pageCount - 1,
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (showCategories) {
            BasicText(
                text = if (isForSource) "Income" else "Expense",
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    textDecoration =
                    if (pagerState.currentPage == 0)
                        TextDecoration.Underline
                    else
                        null,
                ),
                modifier = Modifier
                    .then(scrollToFirstPageOnClickModifier)
            )
        }

        BasicText(
            text = if (isForSource) "From account" else "To account",
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                textDecoration =
                if (pagerState.currentPage == 1 || !showCategories)
                    TextDecoration.Underline
                else
                    null,
            ),
            modifier = Modifier
                .then(scrollToLastPageOnClickModifier)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
    ) { page ->
        when {
            page == 0 && showCategories -> {
                CategoryGrid(
                    itemList = categoryItemList!!,
                    onItemClicked = onCategoryItemClicked,
                )
            }

            else -> {
                AccountList(
                    itemList = accountItemList,
                    onAccountItemClicked = onAccountItemClicked,
                )
            }
        }
    }
}

@Composable
@Preview
private fun TransferCounterpartySelectorPreview(
    @PreviewParameter(TransferCounterpartySelectorPreviewParameterProvider::class) parameter: TransferCounterpartySelectorPreviewParameterProvider.Parameter,
) = TransferCounterpartySelector(
    isForSource = parameter.isForSource,
    accountItemList = parameter.accounts.let(::mutableStateOf),
    categoryItemList = parameter.categories?.let(::mutableStateOf),
    onAccountItemClicked = {},
    onCategoryItemClicked = {},
)

private class TransferCounterpartySelectorPreviewParameterProvider :
    PreviewParameterProvider<TransferCounterpartySelectorPreviewParameterProvider.Parameter> {

    val amount = ViewAmountPreviewParameterProvider().values.first()
    val categories = ViewCategoryListItemPreviewParameterProvider().values.toList()

    override val values: Sequence<Parameter> = sequenceOf(
        Parameter(
            isForSource = true,
            accounts = listOf(
                ViewAccountListItem.Account(
                    title = "Source account",
                    balance = amount,
                )
            ),
            categories = null,
        ),
        Parameter(
            isForSource = false,
            accounts = listOf(
                ViewAccountListItem.Account(
                    title = "Dest account",
                    balance = amount,
                )
            ),
            categories = null,
        ),
        Parameter(
            isForSource = false,
            accounts = listOf(
                ViewAccountListItem.Account(
                    title = "Dest account",
                    balance = amount,
                )
            ),
            categories = categories,
        ),
        Parameter(
            isForSource = true,
            accounts = listOf(
                ViewAccountListItem.Account(
                    title = "Source account",
                    balance = amount,
                )
            ),
            categories = categories,
        )
    )

    class Parameter(
        val isForSource: Boolean,
        val accounts: List<ViewAccountListItem>,
        val categories: List<ViewCategoryListItem>?,
    )
}
