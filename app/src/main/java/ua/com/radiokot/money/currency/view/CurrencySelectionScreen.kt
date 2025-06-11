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

package ua.com.radiokot.money.currency.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun CurrencySelectionScreen(
    modifier: Modifier = Modifier,
    itemList: State<List<CurrencySelectionListItem>>,
    onItemClicked: (CurrencySelectionListItem) -> Unit,
    onCloseClicked: () -> Unit,
    onSaveClicked: () -> Unit,
) = Column(
    modifier = modifier
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .only(WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
        )
        .padding(
            horizontal = 16.dp,
        )
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 56.dp,
            )
    ) {
        val buttonPadding = remember {
            PaddingValues(6.dp)
        }

        TextButton(
            text = "❌",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onCloseClicked,
                )
        )

        Text(
            text = "Currency",
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )

        TextButton(
            text = "✔️",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onSaveClicked,
                )
        )
    }

    val selectionIndicatorTransitionSpec = remember {
        fun AnimatedContentTransitionScope<Boolean>.() =
            if (targetState) {
                ContentTransform(
                    targetContentEnter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                        )
                    ),
                    initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
                    sizeTransform = null,
                )
            } else {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = scaleOut(),
                    sizeTransform = null,
                )
            }
    }

    LazyColumn(

        contentPadding = PaddingValues(
            horizontal = 3.dp,
            vertical = 16.dp,
        )
    ) {

        items(
            items = itemList.value,
            key = CurrencySelectionListItem::key,
        ) { item ->

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(38.dp)
                    .fillMaxWidth()
                    .clickable(
                        onClick = {
                            onItemClicked(item)
                        },
                    )
            ) {

                AnimatedContent(
                    targetState = item.isSelected,
                    label = "selection-indicator",
                    transitionSpec = selectionIndicatorTransitionSpec,
                ) { isItemSelected ->
                    Text(
                        text =
                        if (isItemSelected)
                            "🔴"
                        else
                            "⭕",
                        modifier = Modifier
                            .padding(4.dp)
                    )
                }

                Text(
                    text = item.code,
                    modifier = Modifier
                        .padding(
                            horizontal = 8.dp,
                        )
                        .weight(1f)
                )

                Text(
                    text = item.symbol,
                )
            }
        }
    }
}

@Composable
fun CurrencySelectionScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: CurrencySelectionScreenViewModel,
) {
    CurrencySelectionScreen(
        itemList = viewModel.itemList.collectAsState(),
        onItemClicked = remember { viewModel::onItemClicked },
        onCloseClicked = remember { viewModel::onCloseClicked },
        onSaveClicked = remember { viewModel::onSaveClicked },
        modifier = modifier,
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {
    val itemList = listOf(
        CurrencySelectionListItem(
            code = "USD",
            symbol = "$",
            isSelected = false,
            source = null,
        ),
        CurrencySelectionListItem(
            code = "PLN",
            symbol = "zl",
            isSelected = true,
            source = null,
        ),
        CurrencySelectionListItem(
            code = "EUR",
            symbol = "e",
            isSelected = false,
            source = null,
        )
    )
    CurrencySelectionScreen(
        itemList = itemList.let(::mutableStateOf),
        onItemClicked = {},
        onCloseClicked = {},
        onSaveClicked = {},
    )
}
