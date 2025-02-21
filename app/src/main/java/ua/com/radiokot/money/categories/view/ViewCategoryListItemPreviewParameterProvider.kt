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

package ua.com.radiokot.money.categories.view

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewCurrency
import java.math.BigInteger

class ViewCategoryListItemPreviewParameterProvider :
    PreviewParameterProvider<ViewCategoryListItem> {
    val income: Sequence<ViewCategoryListItem>
        get() = sequenceOf(
            ViewCategoryListItem(
                title = "Sales",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    )
                )
            ),
            ViewCategoryListItem(
                title = "Gifts",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    )
                )
            ),
        )

    val expense: Sequence<ViewCategoryListItem>
        get() = sequenceOf(
            ViewCategoryListItem(
                title = "Food",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "a",
                        precision = 0,
                    )
                )
            ),
            ViewCategoryListItem(
                title = "😽 Грушá",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "c",
                        precision = 0,
                    )
                )
            ),
            ViewCategoryListItem(
                title = "Something long with long name",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "b",
                        precision = 0,
                    )
                )
            )
        )

    override val values: Sequence<ViewCategoryListItem>
        get() = income + expense
}
