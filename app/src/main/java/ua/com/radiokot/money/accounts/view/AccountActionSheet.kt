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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.uikit.TextButton
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider
import java.math.BigInteger

@Composable
fun AccountActionSheet(
    accountDetails: ViewAccountDetails,
    mode: ViewAccountActionSheetMode,
    balanceInputValueFlow: StateFlow<BigInteger>,
    onBalanceClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onBalanceInputValueUpdated: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .shadow(8.dp)
        .safeDrawingPadding()
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .background(Color(0xFFF9FBE7))
        .padding(
            horizontal = 16.dp,
            vertical = 32.dp,
        )
) {
    BackHandler(onBack = onBackPressed)

    BasicText(
        text = accountDetails.title,
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    val clickableBalanceModifier = remember {
        Modifier.clickable { onBalanceClicked() }
    }

    val locale = LocalConfiguration.current.locales[0]
    val amountFormat = remember(locale) {
        ViewAmountFormat(locale)
    }

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
            .then(clickableBalanceModifier)
    )

    Spacer(modifier = Modifier.height(24.dp))

    when (mode) {
        ViewAccountActionSheetMode.Actions ->
            ActionsModeContent(
                onBalanceClicked = onBalanceClicked,
            )

        ViewAccountActionSheetMode.Balance ->
            BalanceModeContent(
                accountDetails = accountDetails,
                balanceInputValueFlow = balanceInputValueFlow,
                amountFormat = amountFormat,
                onBalanceInputValueUpdated = onBalanceInputValueUpdated,
                onBalanceInputSubmit = onBalanceInputSubmit,
            )
    }
}

@Composable
@Preview
private fun AccountActionSheetPreview(
    @PreviewParameter(ViewAmountPreviewParameterProvider::class, limit = 1)
    amount: ViewAmount,
) = Column {
    listOf(
        ViewAccountActionSheetMode.Actions,
        ViewAccountActionSheetMode.Balance,
    ).forEach { mode ->
        AccountActionSheet(
            accountDetails = ViewAccountDetails(
                title = "Account #1",
                balance = amount,
            ),
            mode = mode,
            balanceInputValueFlow = MutableStateFlow(BigInteger("9856")),
            onBalanceClicked = {},
            onBackPressed = {},
            onBalanceInputValueUpdated = {},
            onBalanceInputSubmit = {},
        )
    }
}

@Composable
private fun ActionsModeContent(
    onBalanceClicked: () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    val clickableBalanceModifier = remember {
        Modifier.clickable { onBalanceClicked() }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(
            text = "Edit",
            isEnabled = false,
            modifier = Modifier
                .weight(1f)
        )

        TextButton(
            text = "Balance",
            modifier = Modifier
                .then(clickableBalanceModifier)
                .weight(1f)
        )

        TextButton(
            text = "Activity",
            isEnabled = false,
            modifier = Modifier
                .weight(1f)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(
            text = "Top up",
            isEnabled = false,
            modifier = Modifier
                .weight(1f)
        )

        TextButton(
            text = "Deduct",
            isEnabled = false,
            modifier = Modifier
                .weight(1f)
        )

        TextButton(
            text = "Transfer",
            isEnabled = true,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
private fun BalanceModeContent(
    accountDetails: ViewAccountDetails,
    balanceInputValueFlow: StateFlow<BigInteger>,
    amountFormat: ViewAmountFormat,
    onBalanceInputValueUpdated: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
) {
    Row {
        val balanceInputCurrency = accountDetails.balance.currency
        var balanceInputTextFieldValue by remember {
            val initialValue = amountFormat
                .formatForInput(
                    value = balanceInputValueFlow.value,
                    currency = balanceInputCurrency,
                )

            mutableStateOf(
                TextFieldValue(
                    text = initialValue,
                    selection = TextRange(initialValue.length),
                )
            )
        }
        val focusRequester = remember {
            FocusRequester()
        }

        BasicTextField(
            value = balanceInputTextFieldValue,
            onValueChange = { newValue ->
                val cleanedUpText = amountFormat.unifyDecimalSeparators(newValue.text)
                val parsedValue =
                    amountFormat.parseInput(
                        input = cleanedUpText,
                        currency = balanceInputCurrency,
                    )

                if (parsedValue != null) {
                    onBalanceInputValueUpdated(parsedValue)

                    balanceInputTextFieldValue = newValue.copy(
                        text = cleanedUpText,
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions {
                onBalanceInputSubmit()
            },
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = Color.DarkGray,
                )
                .padding(12.dp)
                .focusRequester(focusRequester)
        )

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        Spacer(modifier = Modifier.width(24.dp))

        val clickableBalanceSaveModifier = remember {
            Modifier.clickable { onBalanceInputSubmit() }
        }

        TextButton(
            text = "Save",
            modifier = Modifier
                .then(clickableBalanceSaveModifier)
        )
    }

}
