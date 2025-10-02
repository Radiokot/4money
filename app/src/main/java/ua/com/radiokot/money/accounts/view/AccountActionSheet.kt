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

package ua.com.radiokot.money.accounts.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.AmountKeyboard
import ua.com.radiokot.money.currency.view.AmountKeyboardMainAction
import ua.com.radiokot.money.currency.view.AnimatedAmountInputText
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.currency.view.rememberAmountInputState
import ua.com.radiokot.money.uikit.TextButton
import java.math.BigInteger

@Composable
fun AccountActionSheet(
    modifier: Modifier = Modifier,
    viewModel: AccountActionSheetViewModel,
) {
    AccountActionSheet(
        title = viewModel.title,
        balance = viewModel.balance,
        colorScheme = viewModel.colorScheme,
        mode = viewModel.mode.collectAsState(),
        balanceInputValue = viewModel.balanceInputValue.collectAsState(),
        onBalanceClicked = remember { viewModel::onBalanceClicked },
        onNewBalanceInputValueParsed = remember { viewModel::onNewBalanceInputValueParsed },
        onBalanceInputSubmit = remember { viewModel::onBalanceInputSubmit },
        onTransferClicked = remember { viewModel::onTransferClicked },
        onIncomeClicked = remember { viewModel::onIncomeClicked },
        onExpenseClicked = remember { viewModel::onExpenseClicked },
        onActivityClicked = remember { viewModel::onActivityClicked },
        onEditClicked = remember { viewModel::onEditClicked },
        onUnarchiveClicked = remember { viewModel::onUnarchiveClicked },
        modifier = modifier,
    )
}

@Composable
private fun AccountActionSheet(
    modifier: Modifier = Modifier,
    title: String,
    balance: ViewAmount,
    colorScheme: ItemColorScheme,
    mode: State<ViewAccountActionSheetMode>,
    balanceInputValue: State<BigInteger>,
    onBalanceClicked: () -> Unit,
    onNewBalanceInputValueParsed: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
    onTransferClicked: () -> Unit,
    onIncomeClicked: () -> Unit,
    onExpenseClicked: () -> Unit,
    onActivityClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onUnarchiveClicked: () -> Unit,
) = BoxWithConstraints(
    modifier = modifier
        .background(Color(0xFFF9FBE7))
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
) {

    val maxSheetHeightDp =
        if (maxHeight < 400.dp)
            maxHeight
        else
            maxHeight * 0.8f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                max = maxSheetHeightDp,
            )
            .verticalScroll(rememberScrollState())
    ) {
        val locale = LocalConfiguration.current.locales[0]
        val amountFormat = remember(locale) {
            ViewAmountFormat(locale)
        }

        Column(
            modifier = Modifier
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            BasicText(
                text = title,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicText(
                text = amountFormat(balance),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                    )
                    .clickable(
                        onClick = onBalanceClicked,
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        when (mode.value) {
            ViewAccountActionSheetMode.DefaultActions ->
                DefaultActionsModeContent(
                    onBalanceClicked = onBalanceClicked,
                    onTransferClicked = onTransferClicked,
                    onIncomeClicked = onIncomeClicked,
                    onExpenseClicked = onExpenseClicked,
                    onActivityClicked = onActivityClicked,
                    onEditClicked = onEditClicked,
                )

            ViewAccountActionSheetMode.ArchivedActions -> {
                ArchivedActionsModeContent(
                    onBalanceClicked = onBalanceClicked,
                    onEditClicked = onEditClicked,
                    onUnarchiveClicked = onUnarchiveClicked,
                )
            }

            ViewAccountActionSheetMode.Balance ->
                BalanceModeContent(
                    currency = balance.currency,
                    colorScheme = colorScheme,
                    balanceInputValue = balanceInputValue,
                    onNewBalanceInputValueParsed = onNewBalanceInputValueParsed,
                    onBalanceInputSubmit = onBalanceInputSubmit,
                    keyboardHeight = maxSheetHeightDp / 2.5f,
                )
        }
    }
}

@Composable
@Preview(
    apiLevel = 34,
    heightDp = 2000,
)
private fun AccountActionSheetPreview(
) = Column {
    ViewAccountActionSheetMode.entries.forEach { mode ->
        BasicText(
            text = mode.name + ": ",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        AccountActionSheet(
            title = "My account",
            balance = ViewAmount(
                value = BigInteger("1050"),
                currency = ViewCurrency(
                    symbol = "$",
                    precision = 2,
                )
            ),
            colorScheme = HardcodedItemColorSchemeRepository()
                .getItemColorSchemes()[20],
            mode = mode.let(::mutableStateOf),
            balanceInputValue = BigInteger("9856").let(::mutableStateOf),
            onBalanceClicked = {},
            onNewBalanceInputValueParsed = {},
            onBalanceInputSubmit = {},
            onTransferClicked = {},
            onIncomeClicked = {},
            onExpenseClicked = {},
            onActivityClicked = {},
            onEditClicked = {},
            onUnarchiveClicked = {},
        )
    }
}

@Composable
private fun DefaultActionsModeContent(
    onBalanceClicked: () -> Unit,
    onTransferClicked: () -> Unit,
    onIncomeClicked: () -> Unit,
    onExpenseClicked: () -> Unit,
    onActivityClicked: () -> Unit,
    onEditClicked: () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier
        .padding(
            horizontal = 16.dp,
        )
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            text = "⚖️ Balance",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onBalanceClicked,
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

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(
            text = "📩 Income",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onIncomeClicked,
                )
        )

        TextButton(
            text = "📨 Expense",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onExpenseClicked,
                )
        )

        TextButton(
            text = "↔️ Transfer",
            isEnabled = true,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onTransferClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ArchivedActionsModeContent(
    onBalanceClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onUnarchiveClicked: () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier
        .padding(
            horizontal = 16.dp,
        )
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            text = "⚖️ Balance",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onBalanceClicked,
                )
        )

        TextButton(
            text = "⤴️ Restore",
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onUnarchiveClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun BalanceModeContent(
    currency: ViewCurrency,
    colorScheme: ItemColorScheme,
    balanceInputValue: State<BigInteger>,
    onNewBalanceInputValueParsed: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
    keyboardHeight: Dp,
) = Column(
    modifier = Modifier
        .padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        )
) {

    val balanceInputState = rememberAmountInputState(
        currency = currency,
        initialValue = balanceInputValue.value,
    )

    LaunchedEffect(balanceInputState) {
        balanceInputState
            .valueFlow
            .collect(onNewBalanceInputValueParsed)
    }

    Text(
        text = "New balance",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    AnimatedAmountInputText(
        amountInputState = balanceInputState,
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    AmountKeyboard(
        inputState = balanceInputState,
        colorScheme = colorScheme,
        mainAction = AmountKeyboardMainAction.Done,
        onMainActionClicked = {
            onBalanceInputSubmit()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(keyboardHeight)
    )
}
