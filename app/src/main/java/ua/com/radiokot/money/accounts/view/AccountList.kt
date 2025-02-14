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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider
import java.math.BigInteger

@Composable
fun AccountList(
    itemListFlow: StateFlow<List<ViewAccountListItem>>,
    modifier: Modifier = Modifier,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit = {},
) {
    val itemList by itemListFlow.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(
            vertical = 8.dp,
        ),
        modifier = modifier,
    ) {
        items(
            items = itemList,
            key = ViewAccountListItem::hashCode,
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
                        title = item.title,
                        balance = item.balance,
                        modifier = Modifier
                            .clickable {
                                onAccountItemClicked(item)
                            }
                            .padding(
                                vertical = 8.dp,
                            )
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
@Preview(
    widthDp = 200
)
private fun AccountListPreview() {
    AccountList(
        itemListFlow = listOf(
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
                key = "acc3",
            ),
        ).let(::MutableStateFlow)
    )
}

@Composable
private fun HeaderItem(
    title: String,
    amount: ViewAmount,
    modifier: Modifier = Modifier,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .weight(1f),
        )

        val locale = LocalConfiguration.current.locales[0]
        val amountFormat = remember(locale) {
            ViewAmountFormat(locale)
        }

        BasicText(
            text = amountFormat(amount),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 16.sp,
            ),
        )
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
    title: String,
    balance: ViewAmount,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        BasicText(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 14.sp,
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        val locale = LocalConfiguration.current.locales[0]
        val amountFormat = remember(locale) {
            ViewAmountFormat(locale)
        }

        BasicText(
            text = amountFormat(balance),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 16.sp,
            ),
        )
    }
}

@Composable
@Preview(
    widthDp = 200,
)
private fun AccountItemPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class) balance: ViewAmount,
) {
    AccountItem(
        title = "Melting cube bank – Visa 4422",
        balance = balance,
    )
}
