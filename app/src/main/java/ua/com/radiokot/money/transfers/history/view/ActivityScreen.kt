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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ua.com.radiokot.money.transfers.view.TransferList
import ua.com.radiokot.money.transfers.view.ViewTransferCounterparty
import ua.com.radiokot.money.transfers.view.ViewTransferListItem

@Composable
fun ActivityScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel,
) = ActivityScreen(
    itemPagingFlow = viewModel.transferItemPagingFlow,
    onTransferItemClicked = remember { viewModel::onTransferItemClicked },
    onTransferItemLongClicked = remember { viewModel::onTransferItemLongClicked },
    period = viewModel.viewHistoryStatsPeriod.collectAsState(),
    onPeriodClicked = {},
    isPreviousPeriodButtonEnabled = viewModel.isPreviousHistoryStatsPeriodButtonEnabled.collectAsState(),
    onPreviousPeriodClicked = remember { viewModel::onPreviousHistoryStatsPeriodClicked },
    isNextPeriodButtonEnabled = viewModel.isNextHistoryStatsPeriodButtonEnabled.collectAsState(),
    onNextPeriodClicked = remember { viewModel::onNextHistoryStatsPeriodClicked },
    isBackHandlerEnabled = viewModel.isBackHandlerEnabled.collectAsState(),
    onBack = remember { viewModel::onBack },
    counterparties = viewModel.activityFilterCounterparties.collectAsState(),
    modifier = modifier,
)

@Composable
private fun ActivityScreen(
    modifier: Modifier = Modifier,
    itemPagingFlow: Flow<PagingData<ViewTransferListItem>>,
    onTransferItemClicked: (ViewTransferListItem.Transfer) -> Unit,
    onTransferItemLongClicked: (ViewTransferListItem.Transfer) -> Unit,
    counterparties: State<List<ViewTransferCounterparty>>,
    period: State<ViewHistoryPeriod>,
    onPeriodClicked: () -> Unit,
    isNextPeriodButtonEnabled: State<Boolean>,
    onNextPeriodClicked: () -> Unit,
    isPreviousPeriodButtonEnabled: State<Boolean>,
    onPreviousPeriodClicked: () -> Unit,
    isBackHandlerEnabled: State<Boolean>,
    onBack: () -> Unit,
) = Column(
    modifier = modifier,
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
                vertical = 16.dp,
            )
    )

    val areCounterpartiesShown by remember {
        derivedStateOf {
            counterparties.value.isNotEmpty()
        }
    }

    if (areCounterpartiesShown) {
        BasicText(
            text = buildString {
                counterparties.value.forEachIndexed { i, counterparty ->
                    append(counterparty.title)
                    if (i != counterparties.value.size - 1) {
                        append(", ")
                    }
                }
            },
            style = TextStyle(
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                )
        )
    }

    val transferListState = remember(period.value) {
        LazyListState()
    }

    TransferList(
        itemPagingFlow = itemPagingFlow,
        onTransferItemClicked = onTransferItemClicked,
        onTransferItemLongClicked = onTransferItemLongClicked,
        state = transferListState,
        modifier = modifier
            .padding(
                horizontal = 16.dp,
            )
    )

    BackHandler(
        enabled = isBackHandlerEnabled.value,
        onBack = onBack,
    )
}
