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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ua.com.radiokot.money.home.view.HomeViewModel
import ua.com.radiokot.money.transfers.history.data.HistoryPeriod
import ua.com.radiokot.money.transfers.view.TransferList
import ua.com.radiokot.money.transfers.view.ViewTransferListItem

@Composable
fun ActivityScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel,
    homeViewModel: HomeViewModel,
) = ActivityScreen(
    itemPagingFlow = viewModel.transferItemPagingFlow,
    onTransferItemClicked = viewModel::onTransferItemClicked,
    period = homeViewModel.period.collectAsStateWithLifecycle(),
    onPeriodClicked = {},
    onPreviousPeriodClicked = homeViewModel::onPreviousPeriodClicked,
    onNextPeriodClicked = homeViewModel::onNextPeriodClicked,
    modifier = modifier,
)

@Composable
private fun ActivityScreen(
    modifier: Modifier = Modifier,
    itemPagingFlow: Flow<PagingData<ViewTransferListItem>>,
    onTransferItemClicked: (ViewTransferListItem.Transfer) -> Unit,
    period: State<HistoryPeriod>,
    onPeriodClicked: () -> Unit,
    onNextPeriodClicked: () -> Unit,
    onPreviousPeriodClicked: () -> Unit,
) = Column(
    modifier = modifier,
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
                vertical = 16.dp,
            )
    )

    TransferList(
        itemPagingFlow = itemPagingFlow,
        onTransferItemClicked = onTransferItemClicked,
        modifier = modifier
            .padding(
                horizontal = 16.dp,
            )
    )
}
