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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.com.radiokot.money.categories.view.CategoryGrid
import ua.com.radiokot.money.categories.view.ViewCategoryListItem
import ua.com.radiokot.money.categories.view.ViewCategoryListItemPreviewParameterProvider
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.uikit.AmountInputField
import ua.com.radiokot.money.uikit.TextButton
import ua.com.radiokot.money.uikit.ViewAmountPreviewParameterProvider
import java.math.BigInteger

@Composable
fun AccountActionSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: AccountActionSheetViewModel,
) {
    val isSheetOpened by viewModel.isOpened.collectAsState()
    AnimatedVisibility(
        visible = isSheetOpened,
        enter = slideInVertically(
            initialOffsetY = Int::unaryPlus,
        ),
        exit = slideOutVertically(
            targetOffsetY = Int::unaryPlus,
        ),
        modifier = modifier
            .widthIn(
                max = 400.dp,
            )
    ) {
        val accountDetailsState = viewModel.accountDetails.collectAsState()
        val modeState = viewModel.mode.collectAsState()

        AccountActionSheet(
            accountDetails = accountDetailsState.value
                ?: return@AnimatedVisibility,
            mode = modeState.value,
            balanceInputValue = viewModel.balanceInputValue.collectAsState(),
            onBalanceClicked = viewModel::onBalanceClicked,
            onBackPressed = viewModel::onBackPressed,
            onNewBalanceInputValueParsed = viewModel::onNewBalanceInputValueParsed,
            onBalanceInputSubmit = viewModel::onBalanceInputSubmit,
            onTransferClicked = viewModel::onTransferClicked,
            onIncomeClicked = viewModel::onIncomeClicked,
            onExpenseClicked = viewModel::onExpenseClicked,
            transferCounterpartyAccountItemList = viewModel.otherAccountListItems.collectAsState(),
            onTransferCounterpartyAccountItemClicked = viewModel::onTransferCounterpartyAccountItemClicked,
            transferCounterpartyIncomeCategoryItemList = viewModel.incomeCategoryListItems.collectAsState(),
            transferCounterpartyExpenseCategoryItemList = viewModel.expenseCategoryListItems.collectAsState(),
            onTransferCounterpartyCategoryItemClicked = viewModel::onTransferCounterpartyCategoryItemClicked,
        )
    }
}

@Composable
private fun AccountActionSheet(
    accountDetails: ViewAccountDetails,
    mode: ViewAccountActionSheetMode,
    balanceInputValue: State<BigInteger>,
    onBalanceClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onNewBalanceInputValueParsed: (BigInteger) -> Unit,
    onBalanceInputSubmit: () -> Unit,
    onTransferClicked: () -> Unit,
    onIncomeClicked: () -> Unit,
    onExpenseClicked: () -> Unit,
    transferCounterpartyAccountItemList: State<List<ViewAccountListItem>>,
    onTransferCounterpartyAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    transferCounterpartyIncomeCategoryItemList: State<List<ViewCategoryListItem>>,
    transferCounterpartyExpenseCategoryItemList: State<List<ViewCategoryListItem>>,
    onTransferCounterpartyCategoryItemClicked: (ViewCategoryListItem) -> Unit,
) = BoxWithConstraints {

    val maxSheetHeightDp =
        if (maxHeight < 400.dp)
            maxHeight
        else
            maxHeight * 0.8f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .shadow(8.dp)
            .safeDrawingPadding()
            .fillMaxWidth()
            .heightIn(
                max = maxSheetHeightDp,
            )
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9FBE7))
            .padding(
                horizontal = 16.dp,
            )
    ) {
        BackHandler(onBack = onBackPressed)

        val clickableBalanceModifier = remember {
            Modifier.clickable { onBalanceClicked() }
        }

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
                    .then(clickableBalanceModifier)
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

            else ->
                TransferCounterpartyContent(
                    accountItemList = transferCounterpartyAccountItemList,
                    onAccountItemClicked = onTransferCounterpartyAccountItemClicked,
                    incomeCategoryItemList = transferCounterpartyIncomeCategoryItemList,
                    expenseCategoryItemList = transferCounterpartyExpenseCategoryItemList,
                    onCategoryItemClicked = onTransferCounterpartyCategoryItemClicked,
                    isIncome = mode == ViewAccountActionSheetMode.IncomeSource,
                    showCategories = mode == ViewAccountActionSheetMode.IncomeSource ||
                            mode == ViewAccountActionSheetMode.ExpenseDestination,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxHeight = (maxSheetHeightDp.roundToPx() - headerHeight),
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
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
            onBackPressed = {},
            onNewBalanceInputValueParsed = {},
            onBalanceInputSubmit = {},
            onTransferClicked = {},
            onIncomeClicked = {},
            onExpenseClicked = {},
            transferCounterpartyAccountItemList = mutableStateOf(
                listOf(
                    ViewAccountListItem.Account(
                        title = "Dest account",
                        balance = amount,
                    )
                )
            ),
            transferCounterpartyExpenseCategoryItemList = categories.let(::mutableStateOf),
            transferCounterpartyIncomeCategoryItemList = categories.let(::mutableStateOf),
            onTransferCounterpartyAccountItemClicked = {},
            onTransferCounterpartyCategoryItemClicked = {},
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

    val clickableBalanceSaveModifier = remember {
        Modifier.clickable { onBalanceInputSubmit() }
    }

    TextButton(
        text = "Save",
        modifier = Modifier
            .then(clickableBalanceSaveModifier)
    )
}

@Composable
private fun TransferCounterpartyContent(
    modifier: Modifier = Modifier,
    isIncome: Boolean,
    showCategories: Boolean,
    accountItemList: State<List<ViewAccountListItem>>,
    incomeCategoryItemList: State<List<ViewCategoryListItem>>,
    expenseCategoryItemList: State<List<ViewCategoryListItem>>,
    onAccountItemClicked: (ViewAccountListItem.Account) -> Unit,
    onCategoryItemClicked: (ViewCategoryListItem) -> Unit,
) = Column(
    modifier = modifier
) {
    val pageCount = if (showCategories) 2 else 1
    val pagerState = rememberPagerState(
        pageCount = pageCount::unaryPlus,
    )
    val coroutineScope = rememberCoroutineScope()
    val scrollToFirstPageOnClickModifier = remember {
        Modifier.clickable {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = 0,
                )
            }
        }
    }
    val scrollToLastPageOnClickModifier = remember(pageCount) {
        Modifier.clickable {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = pageCount,
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (showCategories) {
            BasicText(
                text = if (isIncome) "Income" else "Expense",
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    textDecoration =
                    if (pagerState.currentPage == 0)
                        TextDecoration.Underline
                    else
                        null,
                ),
                modifier = Modifier
                    .then(scrollToFirstPageOnClickModifier)
            )
        }

        BasicText(
            text = if (isIncome) "From account" else "To account",
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                textDecoration =
                if (pagerState.currentPage == 1 || !showCategories)
                    TextDecoration.Underline
                else
                    null,
            ),
            modifier = Modifier
                .then(scrollToLastPageOnClickModifier)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
    ) { page ->
        when {
            page == 0 && showCategories -> {
                CategoryGrid(
                    itemList =
                    if (isIncome)
                        incomeCategoryItemList
                    else
                        expenseCategoryItemList,
                    onItemClicked = onCategoryItemClicked,
                )
            }

            else -> {
                AccountList(
                    itemList = accountItemList,
                    onAccountItemClicked = onAccountItemClicked,
                )
            }
        }
    }
}

