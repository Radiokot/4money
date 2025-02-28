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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.money.auth.view.UserSessionScopeFragment
import ua.com.radiokot.money.transfers.view.TransferList
import ua.com.radiokot.money.transfers.view.ViewTransferListItem

class ActivityFragment : UserSessionScopeFragment() {

    private val viewModel: ActivityViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            ActivityScreenRoot(
                viewModel = viewModel,
            )
        }
    }
}

@Composable
fun ActivityScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel,
) = ActivityScreen(
    itemPagingFlow = viewModel.transferItemPagingFlow,
    modifier = modifier,
)

@Composable
private fun ActivityScreen(
    modifier: Modifier = Modifier,
    itemPagingFlow: Flow<PagingData<ViewTransferListItem>>,
) = TransferList(
    itemPagingFlow = itemPagingFlow,
    onTransferItemClicked = {},
    modifier = modifier
        .padding(
            horizontal = 16.dp,
        )
)
