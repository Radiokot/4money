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

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.datetime.LocalDate
import ua.com.radiokot.money.currency.view.ViewCurrency
import java.math.BigInteger

class ViewTransferItemListPreviewParameterProvider :
    PreviewParameterProvider<List<ViewTransferListItem>> {
    override val values: Sequence<List<ViewTransferListItem>>
        get() = sequenceOf(
            listOf(
                ViewTransferListItem.Header(
                    localDate = LocalDate(2025, 2, 26),
                    dayType = ViewTransferListItem.Header.DayType.Today,
                ),
                ViewTransferListItem.Transfer(
                    primaryCounterparty = ViewTransferCounterparty.Category(
                        categoryTitle = "🍔 Food",
                        subcategoryTitle = null,
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        ),
                    ),
                    primaryAmount = BigInteger("10000"),
                    secondaryCounterparty = ViewTransferCounterparty.Account(
                        accountTitle = "Card 4455",
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        ),
                    ),
                    secondaryAmount = BigInteger("10000"),
                    type = ViewTransferListItem.Transfer.Type.Expense,
                    memo = "My memo with long long content which most probably won't fit a single line of text",
                    source = null,
                ),
                ViewTransferListItem.Transfer(
                    primaryCounterparty = ViewTransferCounterparty.Category(
                        categoryTitle = "🏠 Home",
                        subcategoryTitle = null,
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        ),
                    ),
                    primaryAmount = BigInteger("5000"),
                    secondaryCounterparty = ViewTransferCounterparty.Account(
                        accountTitle = "Cash",
                        currency = ViewCurrency(
                            symbol = "г",
                            precision = 2,
                        ),
                    ),
                    secondaryAmount = BigInteger("217000"),
                    type = ViewTransferListItem.Transfer.Type.Expense,
                    memo = null,
                    source = null,
                )
            )
        )
}
