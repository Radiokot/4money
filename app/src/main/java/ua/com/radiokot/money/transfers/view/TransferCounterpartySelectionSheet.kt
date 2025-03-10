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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.com.radiokot.money.accounts.view.ViewAccountListItem
import ua.com.radiokot.money.categories.view.ViewCategoryListItem

@Composable
private fun TransferCounterpartySelectionSheet(
    modifier: Modifier = Modifier,
    isForSource: Boolean?,
    accountItemList: State<List<ViewAccountListItem>>?,
    incomeCategoryItemList: State<List<ViewCategoryListItem>>?,
    expenseCategoryItemList: State<List<ViewCategoryListItem>>?,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onCategoryItemClicked: (ViewCategoryListItem) -> Unit,
) = BoxWithConstraints(
    modifier = modifier
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
) {
    val maxSheetHeightDp =
        if (maxHeight < 400.dp)
            maxHeight
        else
            maxHeight * 0.8f

    TransferCounterpartySelector(
        isForSource = isForSource,
        accountItemList = accountItemList,
        incomeCategoryItemList = incomeCategoryItemList,
        expenseCategoryItemList = expenseCategoryItemList,
        onAccountItemClicked = onAccountItemClicked,
        onCategoryItemClicked = onCategoryItemClicked,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                max = maxSheetHeightDp,
            )
            .background(Color.White)
            .padding(
                vertical = 16.dp,
            )
    )
}

@Composable
fun TransferCounterpartySelectionSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: TransferCounterpartySelectionSheetViewModel,
) {
    val areIncomeCategoriesVisible = viewModel.areIncomeCategoriesVisible
        .collectAsStateWithLifecycle()
        .value
        ?: return
    val areExpenseCategoriesVisible = viewModel.areExpenseCategoriesVisible
        .collectAsStateWithLifecycle()
        .value
        ?: return
    val areAccountsVisible = viewModel.areAccountsVisible
        .collectAsStateWithLifecycle()
        .value
        ?: return

    TransferCounterpartySelectionSheet(
        isForSource = viewModel.isForSource.collectAsStateWithLifecycle().value,
        accountItemList =
        if (areAccountsVisible)
            viewModel.accountListItems.collectAsStateWithLifecycle()
        else
            null,
        incomeCategoryItemList =
        if (areIncomeCategoriesVisible)
            viewModel.incomeCategoryListItems.collectAsStateWithLifecycle()
        else
            null,
        expenseCategoryItemList =
        if (areExpenseCategoriesVisible)
            viewModel.expenseCategoryListItems.collectAsStateWithLifecycle()
        else
            null,
        onAccountItemClicked = viewModel::onAccountItemClicked,
        onCategoryItemClicked = viewModel::onCategoryItemClicked,
        modifier = modifier,
    )
}
