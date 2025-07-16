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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.uikit.TextButton
import java.math.BigInteger

@Composable
fun CategoryActionSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: CategoryActionSheetViewModel,
) {
    CategoryActionSheet(
        period = viewModel.period,
        amount = viewModel.amount.collectAsState(),
        colorScheme = viewModel.colorScheme.collectAsState(),
        title = viewModel.title.collectAsState(),
        onEditClicked = remember { viewModel::onEditClicked },
        onActivityClicked = remember { viewModel::onActivityClicked },
        modifier = modifier,
    )
}

@Composable
private fun CategoryActionSheet(
    modifier: Modifier = Modifier,
    period: HistoryPeriod,
    amount: State<ViewAmount>,
    colorScheme: State<ItemColorScheme>,
    title: State<String>,
    onEditClicked: () -> Unit,
    onActivityClicked: () -> Unit,
) = Column(
    modifier = modifier
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
) {

    Header(
        period = period,
        amount = amount,
        colorScheme = colorScheme,
        title = title,
        modifier = Modifier
            .fillMaxWidth()
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 32.dp,
            )
    ) {
        TextButton(
            text = "✏️ Edit",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onEditClicked,
                )
        )

        TextButton(
            text = "📃 Activity",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onActivityClicked,
                )
        )
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    period: HistoryPeriod,
    amount: State<ViewAmount>,
    colorScheme: State<ItemColorScheme>,
    title: State<String>,
) = Column(
    modifier = modifier
        .background(Color(colorScheme.value.primary))
        .padding(
            horizontal = 16.dp,
            vertical = 24.dp,
        )
) {
    val textColor by remember {
        derivedStateOf {
            Color(colorScheme.value.onPrimary)
        }
    }

    Text(
        text = title.value,
        fontSize = 24.sp,
        color = textColor,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row {
        Text(
            text = when (period) {
                is HistoryPeriod.Day ->
                    period.localDay.toString()

                is HistoryPeriod.Month ->
                    LocalDate.Format {
                        monthName(MonthNames.ENGLISH_FULL)
                        chars(" ")
                        year()
                    }.format(period.localMonth)

                HistoryPeriod.Since70th ->
                    "All time"
            },
            color = textColor,
        )

        val locale = LocalConfiguration.current.locales[0]
        val amountFormat = remember(locale) {
            ViewAmountFormat(locale)
        }

        Text(
            text = amountFormat(
                amount = amount.value,
                customColor = textColor,
            ),
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
@Preview(
    apiLevel = 34,
)
private fun Preview(

) {
    CategoryActionSheet(
        period = HistoryPeriod.Day(),
        amount = ViewAmount(
            value = BigInteger("15000"),
            currency = ViewCurrency(
                symbol = "$",
                precision = 2,
            )
        ).let(::mutableStateOf),
        colorScheme = HardcodedItemColorSchemeRepository()
            .getItemColorSchemesByName()
            .getValue("Purple2")
            .let(::mutableStateOf),
        title = "Health".let(::mutableStateOf),
        onEditClicked = {},
        onActivityClicked = {},
    )
}
