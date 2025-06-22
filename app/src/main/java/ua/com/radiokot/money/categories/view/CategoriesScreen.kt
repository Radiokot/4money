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

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.history.view.PeriodBar
import java.math.BigInteger

@Composable
fun CategoriesScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: CategoriesScreenViewModel,
) = CategoriesScreen(
    isIncome = viewModel.isIncome.collectAsStateWithLifecycle(),
    period = viewModel.historyStatsPeriod.collectAsStateWithLifecycle(),
    totalAmount = viewModel.totalAmount.collectAsStateWithLifecycle(),
    incomeCategoryItemList = viewModel.incomeCategoryItemList.collectAsStateWithLifecycle(),
    expenseCategoryItemList = viewModel.expenseCategoryItemList.collectAsStateWithLifecycle(),
    onTitleClicked = remember { viewModel::onTitleClicked },
    onCategoryItemClicked = remember { viewModel::onCategoryItemClicked },
    onCategoryItemLongClicked = remember { viewModel::onCategoryItemLongClicked },
    onPeriodClicked = {},
    onPreviousPeriodClicked = remember { viewModel::onPreviousHistoryStatsPeriodClicked },
    onNextPeriodClicked = remember { viewModel::onNextHistoryStatsPeriodClicked },
    modifier = modifier,
)

@Composable
private fun CategoriesScreen(
    modifier: Modifier = Modifier,
    isIncome: State<Boolean>,
    period: State<HistoryPeriod>,
    totalAmount: State<ViewAmount?>,
    incomeCategoryItemList: State<List<ViewCategoryListItem>>,
    expenseCategoryItemList: State<List<ViewCategoryListItem>>,
    onTitleClicked: () -> Unit,
    onCategoryItemClicked: (ViewCategoryListItem) -> Unit,
    onCategoryItemLongClicked: (ViewCategoryListItem) -> Unit,
    onPeriodClicked: () -> Unit,
    onNextPeriodClicked: () -> Unit,
    onPreviousPeriodClicked: () -> Unit,
) = Column(
    modifier = modifier
        .padding(
            vertical = 16.dp,
        )
) {
    PeriodBar(
        period = period,
        onPeriodClicked = onPeriodClicked,
        onNextPeriodClicked = onNextPeriodClicked,
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
        itemList =
        if (isIncome.value)
            incomeCategoryItemList
        else
            expenseCategoryItemList,
        onItemClicked = onCategoryItemClicked,
        onItemLongClicked = onCategoryItemLongClicked,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    )
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview
private fun CategoriesScreenPreview(

) {
    val categories = ViewCategoryListItemPreviewParameterProvider().values.toList()

    CategoriesScreen(
        isIncome = mutableStateOf(true),
        totalAmount = ViewAmount(
            value = BigInteger("10100000"),
            currency = ViewCurrency(
                symbol = "$",
                precision = 2,
            )
        ).let(::mutableStateOf),
        period = HistoryPeriod.Month().let(::mutableStateOf),
        incomeCategoryItemList = mutableStateOf(categories),
        expenseCategoryItemList = mutableStateOf(categories),
        onTitleClicked = {},
        onCategoryItemClicked = {},
        onCategoryItemLongClicked = {},
        onPreviousPeriodClicked = {},
        onNextPeriodClicked = {},
        onPeriodClicked = {},
    )
}
