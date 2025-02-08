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

package ua.com.radiokot.money.uikit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.math.BigInteger

@Composable
fun AccountList(
    items: List<ViewAccountListItem>,
    modifier: Modifier = Modifier,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(
            items = items,
            key = ViewAccountListItem::hashCode,
        ) { item ->
            when (item) {
                is ViewAccountListItem.Header -> {
                    AccountListHeader(
                        title = item.title,
                        amount = item.amount,
                        modifier = Modifier
                            .padding(
                                vertical = 12.dp,
                            )
                            .fillMaxWidth()
                    )
                }

                is ViewAccountListItem.Account -> {
                    AccountListItem(
                        title = item.title,
                        balance = item.balance,
                        modifier = Modifier
                            .clickable {
                                onAccountItemClicked(item)
                            }
                            .padding(
                                vertical = 4.dp,
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
        items = listOf(
            ViewAccountListItem.Header(
                title = "Accounts",
                amount = ViewAmount(
                    value = BigInteger("10000"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    ),
                ),
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
            ),
            ViewAccountListItem.Account(
                title = "Account #2",
                balance = ViewAmount(
                    value = BigInteger("100000000"),
                    currency = ViewCurrency(
                        symbol = "₿",
                        precision = 8,
                    ),
                ),
            ),
        )
    )
}
