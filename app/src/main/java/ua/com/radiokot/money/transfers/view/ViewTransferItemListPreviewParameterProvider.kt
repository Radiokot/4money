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
import ua.com.radiokot.money.currency.view.ViewAmount
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
                    amount = ViewAmount(
                        value = BigInteger("-15000"),
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        )
                    )
                ),
                ViewTransferListItem.Transfer(
                    primaryCounterparty = ViewTransferCounterparty(
                        title = "🍔 Food",
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        ),
                        type = ViewTransferCounterparty.Type.Category,
                    ),
                    primaryAmount = BigInteger("10000"),
                    secondaryCounterparty = ViewTransferCounterparty(
                        title = "Card 4455",
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        ),
                        type = ViewTransferCounterparty.Type.Account,
                    ),
                    secondaryAmount = BigInteger("10000"),
                    type = ViewTransferListItem.Transfer.Type.Expense,
                    source = null,
                ),
                ViewTransferListItem.Transfer(
                    primaryCounterparty = ViewTransferCounterparty(
                        title = "🏠 Home",
                        currency = ViewCurrency(
                            symbol = "$",
                            precision = 2,
                        ),
                        type = ViewTransferCounterparty.Type.Category,
                    ),
                    primaryAmount = BigInteger("5000"),
                    secondaryCounterparty = ViewTransferCounterparty(
                        title = "Cash",
                        currency = ViewCurrency(
                            symbol = "г",
                            precision = 2,
                        ),
                        type = ViewTransferCounterparty.Type.Account,
                    ),
                    secondaryAmount = BigInteger("217000"),
                    type = ViewTransferListItem.Transfer.Type.Expense,
                    source = null,
                )
            )
        )
}
