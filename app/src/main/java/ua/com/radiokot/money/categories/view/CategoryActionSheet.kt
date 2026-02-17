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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Icon
import com.composeunstyled.Text
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.transfers.history.view.ViewHistoryPeriod
import ua.com.radiokot.money.transfers.view.ViewDate
import ua.com.radiokot.money.uikit.TextButton
import java.math.BigInteger

@Composable
fun CategoryActionSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: CategoryActionSheetViewModel,
) {
    CategoryActionSheet(
        statsPeriod = viewModel.statsPeriod,
        statsAmount = viewModel.statsAmount.collectAsState(),
        subcategoryAmounts = viewModel.subcategoryAmounts.collectAsState(),
        colorScheme = viewModel.colorScheme.collectAsState(),
        title = viewModel.title.collectAsState(),
        icon = viewModel.icon.collectAsState(),
        onEditClicked = remember { viewModel::onEditClicked },
        onActivityClicked = remember { viewModel::onActivityClicked },
        isUnarchiveVisible = viewModel.isUnarchiveVisible.collectAsState(),
        onUnarchiveClicked = remember { viewModel::onUnarchiveClicked },
        modifier = modifier,
    )
}

@Composable
private fun CategoryActionSheet(
    modifier: Modifier = Modifier,
    statsPeriod: ViewHistoryPeriod,
    statsAmount: State<ViewAmount>,
    subcategoryAmounts: State<List<Pair<String?, ViewAmount>>>,
    colorScheme: State<ItemColorScheme>,
    title: State<String>,
    icon: State<ItemIcon?>,
    isUnarchiveVisible: State<Boolean>,
    onEditClicked: () -> Unit,
    onActivityClicked: () -> Unit,
    onUnarchiveClicked: () -> Unit,
) = Column(
    modifier = modifier
        .background(Color(0xFFF9FBE7))
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
) {

    Header(
        statsPeriod = statsPeriod,
        statsAmount = statsAmount,
        subcategoryAmounts = subcategoryAmounts,
        colorScheme = colorScheme,
        title = title,
        icon = icon,
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

        if (isUnarchiveVisible.value) {
            TextButton(
                text = "⤴️ Restore",
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        onClick = onUnarchiveClicked,
                    )
            )
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    statsPeriod: ViewHistoryPeriod,
    statsAmount: State<ViewAmount>,
    subcategoryAmounts: State<List<Pair<String?, ViewAmount>>>,
    colorScheme: State<ItemColorScheme>,
    title: State<String>,
    icon: State<ItemIcon?>,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = icon.value
        if (icon != null) {
            Icon(
                painter = painterResource(icon.resId),
                contentDescription = "icon",
                tint = Color(colorScheme.value.onPrimary),
                modifier = Modifier
                    .padding(
                        end = 8.dp,
                    )
                    .size(22.dp)
            )
        }

        Text(
            text = title.value,
            fontSize = 24.sp,
            color = textColor,
            modifier = Modifier
                .weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    val locale = LocalConfiguration.current.locales[0]
    val amountFormat = remember(locale) {
        ViewAmountFormat(locale)
    }

    Row {
        Text(
            text = statsPeriod.getText(),
            color = textColor,
            fontSize = 18.sp,
        )

        Text(
            text = amountFormat(
                amount = statsAmount.value,
                customColor = textColor,
            ),
            textAlign = TextAlign.End,
            fontSize = 18.sp,
            modifier = Modifier
                .weight(1f)
        )
    }

    if (subcategoryAmounts.value.isNotEmpty()) {
        Box(
            modifier = Modifier
                .padding(
                    vertical = 8.dp,
                )
                .height(1.dp)
                .fillMaxWidth()
                .background(textColor)
        )

        subcategoryAmounts.value.forEachIndexed { i, (title, amount) ->
            key(i, title) {
                Row(
                    modifier = Modifier
                        .padding(
                            top =
                                if (i != 0)
                                    4.dp
                                else
                                    0.dp
                        )
                ) {
                    Text(
                        text = title ?: "Other",
                        color = textColor,
                    )

                    Text(
                        text = amountFormat(
                            amount = amount,
                            customColor = textColor,
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
@Preview(
    apiLevel = 34,
)
private fun Preview(

) {
    CategoryActionSheet(
        statsPeriod = ViewHistoryPeriod.Day(
            day = ViewDate.today(),
        ),
        statsAmount = ViewAmount(
            value = BigInteger("15000"),
            currency = ViewCurrency(
                symbol = "$",
                precision = 2,
            )
        ).let(::mutableStateOf),
        subcategoryAmounts = listOf(
            "Pharmacy" to ViewAmount(
                value = BigInteger("10000"),
                currency = ViewCurrency(
                    symbol = "$",
                    precision = 2,
                )
            ),
            null to ViewAmount(
                value = BigInteger("5000"),
                currency = ViewCurrency(
                    symbol = "$",
                    precision = 2,
                )
            ),
        ).let(::mutableStateOf),
        colorScheme = HardcodedItemColorSchemeRepository()
            .getItemColorSchemesByName()
            .getValue("Purple2")
            .let(::mutableStateOf),
        title = "Health".let(::mutableStateOf),
        icon = DrawableResItemIconRepository()
            .getItemIcons()
            .get(22)
            .let(::mutableStateOf),
        isUnarchiveVisible = true.let(::mutableStateOf),
        onEditClicked = {},
        onActivityClicked = {},
        onUnarchiveClicked = {},
    )
}
