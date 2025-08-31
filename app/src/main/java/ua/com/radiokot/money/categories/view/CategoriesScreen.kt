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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.transfers.history.view.PeriodBar
import ua.com.radiokot.money.transfers.history.view.ViewHistoryPeriod

@Composable
fun CategoriesScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: CategoriesScreenViewModel,
) = CategoriesScreen(
    isIncome = viewModel.isIncome.collectAsState(),
    period = viewModel.viewHistoryStatsPeriod.collectAsState(),
    totalAmount = viewModel.totalAmount.collectAsState(),
    categoryItemList = viewModel.categoryItemList.collectAsState(),
    onTitleClicked = remember { viewModel::onTitleClicked },
    onCategoryItemClicked = remember { viewModel::onCategoryItemClicked },
    onCategoryItemLongClicked = remember { viewModel::onCategoryItemLongClicked },
    onPeriodClicked = {},
    isPreviousPeriodButtonEnabled = viewModel.isPreviousHistoryStatsPeriodButtonEnabled.collectAsState(),
    onPreviousPeriodClicked = remember { viewModel::onPreviousHistoryStatsPeriodClicked },
    isNextPeriodButtonEnabled = viewModel.isNextHistoryStatsPeriodButtonEnabled.collectAsState(),
    onNextPeriodClicked = remember { viewModel::onNextHistoryStatsPeriodClicked },
    onAddClicked = remember { viewModel::onAddClicked },
    categoryArchiveItemList = viewModel.categoryArchiveItemList.collectAsState(),
    modifier = modifier,
)

@Composable
private fun CategoriesScreen(
    modifier: Modifier = Modifier,
    isIncome: State<Boolean>,
    period: State<ViewHistoryPeriod>,
    totalAmount: State<ViewAmount?>,
    categoryItemList: State<List<ViewCategoryListItem>>,
    onTitleClicked: () -> Unit,
    onCategoryItemClicked: (ViewCategoryListItem) -> Unit,
    onCategoryItemLongClicked: (ViewCategoryListItem) -> Unit,
    onPeriodClicked: () -> Unit,
    isNextPeriodButtonEnabled: State<Boolean>,
    onNextPeriodClicked: () -> Unit,
    isPreviousPeriodButtonEnabled: State<Boolean>,
    onPreviousPeriodClicked: () -> Unit,
    onAddClicked: () -> Unit,
    categoryArchiveItemList: State<List<ViewCategoryListItem>>,
) = Column(
    modifier = modifier
        .padding(
            vertical = 16.dp,
        )
) {
    PeriodBar(
        period = period,
        onPeriodClicked = onPeriodClicked,
        isNextButtonEnabled = isNextPeriodButtonEnabled,
        onNextPeriodClicked = onNextPeriodClicked,
        isPreviousButtonEnabled = isPreviousPeriodButtonEnabled,
        onPreviousPeriodClicked = onPreviousPeriodClicked,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 22.dp,
            )
    )

    Spacer(modifier = Modifier.height(12.dp))

    BasicText(
        text =
            if (isIncome.value)
                "Incomes"
            else
                "Expenses",
        style = TextStyle(
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onTitleClicked,
            )
    )

    val locale = LocalConfiguration.current.locales.get(0)
    val amountFormat = remember(locale) {
        ViewAmountFormat(locale)
    }
    val totalAmountText: AnnotatedString by remember {
        derivedStateOf {
            if (totalAmount.value != null)
                amountFormat(
                    amount = totalAmount.value!!,
                    customColor =
                        if (isIncome.value)
                            Color(0xff50af99)
                        else
                            Color(0xffd85e8c)
                )
            else
                AnnotatedString("")
        }
    }

    BasicText(
        text = totalAmountText,
        style = TextStyle(
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onTitleClicked,
            )
            .padding(
                vertical = 6.dp,
            )
    )

    CategoryGrid(
        itemList = categoryItemList,
        onItemClicked = onCategoryItemClicked,
        onItemLongClicked = onCategoryItemLongClicked,
        isAddShown = true,
        onAddClicked = onAddClicked,
        archiveItemList = categoryArchiveItemList,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    )
}
