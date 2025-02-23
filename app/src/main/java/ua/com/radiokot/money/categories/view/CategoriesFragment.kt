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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.money.auth.view.UserSessionScopeFragment

class CategoriesFragment : UserSessionScopeFragment() {

    private val viewModel: CategoriesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            CategoriesScreenRoot(
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun CategoriesScreenRoot(
    viewModel: CategoriesViewModel,
) = CategoriesScreen(
    isIncome = viewModel.isIncome.collectAsStateWithLifecycle(),
    incomeCategoryItemList = viewModel.incomeCategoryItemList.collectAsStateWithLifecycle(),
    expenseCategoryItemList = viewModel.expenseCategoryItemList.collectAsStateWithLifecycle(),
    onTitleClicked = viewModel::onTitleClicked,
)

@Composable
private fun CategoriesScreen(
    isIncome: State<Boolean>,
    incomeCategoryItemList: State<List<ViewCategoryListItem>>,
    expenseCategoryItemList: State<List<ViewCategoryListItem>>,
    onTitleClicked: () -> Unit,
) = Column(
    modifier = Modifier
        .padding(
            vertical = 16.dp,
        )
) {
    val clickableTitleModifier = remember {
        Modifier.clickable { onTitleClicked() }
    }

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
            .then(clickableTitleModifier)
    )

    CategoryGrid(
        itemList =
        if (isIncome.value)
            incomeCategoryItemList
        else
            expenseCategoryItemList,
        onItemClicked = { },
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
        incomeCategoryItemList = mutableStateOf(categories),
        expenseCategoryItemList = mutableStateOf(categories),
        onTitleClicked = {},
    )
}
