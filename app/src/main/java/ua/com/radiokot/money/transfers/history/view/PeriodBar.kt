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

package ua.com.radiokot.money.transfers.history.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import ua.com.radiokot.money.stableClickable
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.uikit.TextButton

@Composable
fun PeriodBar(
    modifier: Modifier = Modifier,
    period: State<HistoryPeriod>,
    onPeriodClicked: () -> Unit,
    onNextPeriodClicked: () -> Unit,
    onPreviousPeriodClicked: () -> Unit,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier,
) {
    val buttonPadding = remember {
        PaddingValues(6.dp)
    }

    TextButton(
        text = "⬅️",
        padding = buttonPadding,
        modifier = Modifier
            .stableClickable(
                onClick = onPreviousPeriodClicked,
            )
    )

    BasicText(
        // TODO Use Java localized formats.
        text = when (val periodValue = period.value) {
            is HistoryPeriod.Day ->
                periodValue.localDay.toString()

            is HistoryPeriod.Month ->
                LocalDate.Format {
                    monthName(MonthNames.ENGLISH_FULL)
                    chars(" ")
                    year()
                }.format(periodValue.localMonth)

            HistoryPeriod.Since70th ->
                "All time"
        },
        style = TextStyle(
            fontSize = 16.sp,
        ),
        modifier = Modifier
            .stableClickable(
                onClick = onPeriodClicked,
            )
            .padding(buttonPadding)
    )

    TextButton(
        text = "➡️",
        padding = buttonPadding,
        modifier = Modifier
            .stableClickable(
                onClick = onNextPeriodClicked,
            )
    )
}

@Preview(
    widthDp = 160,
)
@Composable
private fun PeriodBarPreview(
) = PeriodBar(
    modifier = Modifier
        .fillMaxWidth(),
    period = HistoryPeriod.Month().let(::mutableStateOf),
    onPeriodClicked = {

    },
    onNextPeriodClicked = { },
    onPreviousPeriodClicked = { }
)
