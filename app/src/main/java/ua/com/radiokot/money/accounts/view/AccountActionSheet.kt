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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.com.radiokot.money.categories.view.ViewCategoryListItemPreviewParameterProvider
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.stableClickable
import ua.com.radiokot.money.uikit.AmountInputField
import ua.com.radiokot.money.uikit.TextButton
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider
import java.math.BigInteger

@Composable
fun AccountActionSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: AccountActionSheetViewModel,
) {
    val accountDetailsState = viewModel.accountDetails.collectAsState()
    val modeState = viewModel.mode.collectAsState()

    AccountActionSheet(
        accountDetails = accountDetailsState.value ?: return,
        mode = modeState.value,
        balanceInputValue = viewModel.balanceInputValue.collectAsState(),
        onBalanceClicked = viewModel::onBalanceClicked,
        onNewBalanceInputValueParsed = viewModel::onNewBalanceInputValueParsed,
        onBalanceInputSubmit = viewModel::onBalanceInputSubmit,
        onTransferClicked = viewModel::onTransferClicked,
        onIncomeClicked = viewModel::onIncomeClicked,
        onExpenseClicked = viewModel::onExpenseClicked,
        modifier = modifier,
    )
}

@Composable
private fun AccountActionSheet(
    modifier: Modifier = Modifier,
    accountDetails: ViewAccountDetails,
    mode: ViewAccountActionSheetMode,
    balanceInputValue: State<BigInteger>,
    onBalanceClicked: () -> Unit,
    onNewBalanceInputValueParsed: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
    onTransferClicked: () -> Unit,
    onIncomeClicked: () -> Unit,
    onExpenseClicked: () -> Unit,
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

        var headerHeight = 0

        Column(
            modifier = Modifier
                .onSizeChanged { headerHeight = it.height }
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            BasicText(
                text = accountDetails.title,
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
                text = amountFormat(accountDetails.balance),
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
                    .stableClickable(
                        onClick = onBalanceClicked,
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        when (mode) {
            ViewAccountActionSheetMode.Actions ->
                ActionsModeContent(
                    onBalanceClicked = onBalanceClicked,
                    onTransferClicked = onTransferClicked,
                    onIncomeClicked = onIncomeClicked,
                    onExpenseClicked = onExpenseClicked,
                )

            ViewAccountActionSheetMode.Balance ->
                BalanceModeContent(
                    accountDetails = accountDetails,
                    balanceInputValue = balanceInputValue,
                    amountFormat = amountFormat,
                    onNewBalanceInputValueParsed = onNewBalanceInputValueParsed,
                    onBalanceInputSubmit = onBalanceInputSubmit,
                )
        }
    }
}

@Composable
@Preview(
    heightDp = 2000,
)
private fun AccountActionSheetPreview(
) = Column {
    val amount = ViewAmountPreviewParameterProvider().values.first()
    val categories = ViewCategoryListItemPreviewParameterProvider().values.toList()

    ViewAccountActionSheetMode.entries.forEach { mode ->
        BasicText(
            text = mode.name + ": ",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        AccountActionSheet(
            accountDetails = ViewAccountDetails(
                title = "Account #1",
                balance = amount,
            ),
            mode = mode,
            balanceInputValue = BigInteger("9856").let(::mutableStateOf),
            onBalanceClicked = {},
            onNewBalanceInputValueParsed = {},
            onBalanceInputSubmit = {},
            onTransferClicked = {},
            onIncomeClicked = {},
            onExpenseClicked = {},
        )
    }
}

@Composable
private fun ActionsModeContent(
    onBalanceClicked: () -> Unit,
    onTransferClicked: () -> Unit,
    onIncomeClicked: () -> Unit,
    onExpenseClicked: () -> Unit,
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
            isEnabled = false,
            modifier = Modifier
                .weight(1f)
        )

        TextButton(
            text = "⚖️ Balance",
            modifier = Modifier
                .then(
                    remember {
                        Modifier.clickable { onBalanceClicked() }
                    }
                )
                .weight(1f)
        )

        TextButton(
            text = "📃 Activity",
            isEnabled = false,
            modifier = Modifier
                .weight(1f)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(
            text = "📩 Income",
            modifier = Modifier
                .weight(1f)
                .then(
                    remember {
                        Modifier.clickable { onIncomeClicked() }
                    }
                )
        )

        TextButton(
            text = "📨 Expense",
            modifier = Modifier
                .weight(1f)
                .then(
                    remember {
                        Modifier.clickable { onExpenseClicked() }
                    }
                )
        )

        TextButton(
            text = "↔️ Transfer",
            isEnabled = true,
            modifier = Modifier
                .then(
                    remember {
                        Modifier.clickable { onTransferClicked() }
                    }
                )
                .weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun BalanceModeContent(
    accountDetails: ViewAccountDetails,
    balanceInputValue: State<BigInteger>,
    amountFormat: ViewAmountFormat,
    onNewBalanceInputValueParsed: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
) = Row(
    modifier = Modifier
        .padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        )
) {
    val balanceInputCurrency = accountDetails.balance.currency
    val focusRequester = remember {
        FocusRequester()
    }

    AmountInputField(
        value = balanceInputValue,
        currency = balanceInputCurrency,
        amountFormat = amountFormat,
        onNewValueParsed = onNewBalanceInputValueParsed,
        onKeyboardSubmit = onBalanceInputSubmit,
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Spacer(modifier = Modifier.width(24.dp))

    TextButton(
        text = "Save",
        modifier = Modifier
            .stableClickable(
                onClick = onBalanceInputSubmit,
            )
    )
}
