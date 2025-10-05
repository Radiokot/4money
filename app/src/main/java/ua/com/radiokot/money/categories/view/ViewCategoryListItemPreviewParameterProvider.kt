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
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewCurrency
import java.math.BigInteger

class ViewCategoryListItemPreviewParameterProvider :
    PreviewParameterProvider<ViewCategoryListItem> {

    private val colorSchemesByName = HardcodedItemColorSchemeRepository()
        .getItemColorSchemesByName()
    private val iconsByName = DrawableResItemIconRepository()
        .getItemIconsByName()

    val income: Sequence<ViewCategoryListItem>
        get() = sequenceOf(
            ViewCategoryListItem(
                title = "Sales",
                amount = ViewAmount(
                    value = BigInteger("410"),
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    )
                ),
                colorScheme = colorSchemesByName.getValue("Blue2"),
                icon = iconsByName["other_14"],
                isArchived = false,
                isIncognito = false,
                period = null,
            ),
            ViewCategoryListItem(
                title = "Gifts",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "$",
                        precision = 2,
                    )
                ),
                colorScheme = colorSchemesByName.getValue("Turquoise3"),
                icon = iconsByName["socializing_15"],
                isArchived = true,
                isIncognito = true,
                period = null,
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
                ),
                colorScheme = colorSchemesByName.getValue("Red1"),
                icon = null,
                isArchived = false,
                isIncognito = false,
                period = null,
            ),
            ViewCategoryListItem(
                title = "😽 Грушá with long name",
                amount = ViewAmount(
                    value = BigInteger("5"),
                    currency = ViewCurrency(
                        symbol = "c",
                        precision = 0,
                    )
                ),
                colorScheme = colorSchemesByName.getValue("Green1"),
                icon = null,
                isArchived = false,
                isIncognito = false,
                period = null,
            ),
            ViewCategoryListItem(
                title = "Incognito",
                amount = ViewAmount(
                    value = BigInteger.ZERO,
                    currency = ViewCurrency(
                        symbol = "\$T",
                        precision = 0,
                    )
                ),
                colorScheme = colorSchemesByName.getValue("Orange2"),
                icon = null,
                isArchived = true,
                isIncognito = true,
                period = null,
            )
        )

    override val values: Sequence<ViewCategoryListItem>
        get() = income + expense
}
