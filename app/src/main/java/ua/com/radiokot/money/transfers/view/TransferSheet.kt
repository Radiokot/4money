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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.com.radiokot.money.categories.view.SelectableSubcategoryRow
import ua.com.radiokot.money.categories.view.ViewSelectableSubcategoryListItem
import ua.com.radiokot.money.categories.view.ViewSelectableSubcategoryListItemPreviewParameterProvider
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.view.AmountKeyboard
import ua.com.radiokot.money.currency.view.AmountKeyboardMainAction
import ua.com.radiokot.money.currency.view.AnimatedAmountInputText
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.currency.view.rememberAmountInputState
import java.math.BigInteger

@Composable
fun TransferSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: TransferSheetViewModel,
) {
    TransferSheet(
        modifier = modifier,
        isSourceInputShown = viewModel.isSourceInputShown.collectAsState().value,
        source = viewModel.source.collectAsState().value,
        sourceAmountValue = viewModel.sourceAmountValue.collectAsState(),
        onNewSourceAmountValueParsed = remember { viewModel::onNewSourceAmountValueParsed },
        destination = viewModel.destination.collectAsState().value,
        destinationAmountValue = viewModel.destinationAmountValue.collectAsState(),
        onNewDestinationAmountValueParsed = remember { viewModel::onNewDestinationAmountValueParsed },
        memo = viewModel.memo.collectAsStateWithLifecycle(),
        date = viewModel.date.collectAsStateWithLifecycle(),
        onMemoUpdated = remember { viewModel::onMemoUpdated },
        subcategoryItemList = viewModel.subcategoryItemList.collectAsState(),
        subcategoriesColorScheme = viewModel.subcategoriesColorScheme.collectAsState(),
        onSubcategoryItemClicked = remember { viewModel::onSubcategoryItemClicked },
        onSaveClicked = remember { viewModel::onSaveClicked },
        onDateClicked = remember { viewModel::onDateClicked },
        onSourceClicked = remember { viewModel::onSourceClicked },
        onDestinationClicked = remember { viewModel::onDestinationClicked },
    )
}

@Composable
private fun TransferSheet(
    modifier: Modifier = Modifier,
    isSourceInputShown: Boolean,
    source: ViewTransferCounterparty,
    sourceAmountValue: State<BigInteger>,
    onNewSourceAmountValueParsed: (BigInteger) -> Unit,
    destination: ViewTransferCounterparty,
    destinationAmountValue: State<BigInteger>,
    onNewDestinationAmountValueParsed: (BigInteger) -> Unit,
    memo: State<String>,
    date: State<ViewDate>,
    onMemoUpdated: (String) -> Unit,
    subcategoryItemList: State<List<ViewSelectableSubcategoryListItem>>,
    subcategoriesColorScheme: State<ItemColorScheme?>,
    onSubcategoryItemClicked: (ViewSelectableSubcategoryListItem) -> Unit,
    onSaveClicked: () -> Unit,
    onDateClicked: () -> Unit,
    onSourceClicked: () -> Unit,
    onDestinationClicked: () -> Unit,
) = BoxWithConstraints(
    modifier = modifier
        .background(Color.White)
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
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
        ) {
            val shortSourceTitle: String? = remember(source) {
                (source as? ViewTransferCounterparty.Category)?.categoryTitle
            }
            val shortDestinationTitle: String? = remember(destination) {
                (destination as? ViewTransferCounterparty.Category)?.categoryTitle
            }
            val sourcePrimaryColor = remember(source) {
                Color(source.colorScheme.primary)
            }
            val sourceOnPrimaryColor = remember(source) {
                Color(source.colorScheme.onPrimary)
            }
            val destinationPrimaryColor = remember(destination) {
                Color(destination.colorScheme.primary)
            }
            val destinationOnPrimaryColor = remember(destination) {
                Color(destination.colorScheme.onPrimary)
            }

            BasicText(
                text = shortSourceTitle ?: source.title,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = sourceOnPrimaryColor,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(sourcePrimaryColor)
                    .clickable(
                        onClick = onSourceClicked,
                    )
                    .padding(
                        vertical = 24.dp,
                        horizontal = 8.dp,
                    )
            )

            BasicText(
                text = shortDestinationTitle ?: destination.title,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = destinationOnPrimaryColor,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(destinationPrimaryColor)
                    .clickable(
                        onClick = onDestinationClicked,
                    )
                    .padding(
                        vertical = 24.dp,
                        horizontal = 8.dp,
                    )
            )
        }

        if (subcategoryItemList.value.isNotEmpty()
            && subcategoriesColorScheme.value != null
        ) {
            SelectableSubcategoryRow(
                itemList = subcategoryItemList,
                colorScheme = subcategoriesColorScheme.value!!,
                onItemClicked = onSubcategoryItemClicked,
                modifier = Modifier
                    .padding(
                        vertical = 12.dp,
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        val sourceAmountInputState = rememberAmountInputState(
            currency = source.currency,
            initialValue = sourceAmountValue.value,
        )
        LaunchedEffect(sourceAmountInputState) {
            sourceAmountInputState
                .valueFlow
                .collect(onNewSourceAmountValueParsed)
        }
        val destinationAmountInputState = rememberAmountInputState(
            currency = destination.currency,
            initialValue = destinationAmountValue.value,
        )
        LaunchedEffect(destinationAmountInputState) {
            destinationAmountInputState
                .valueFlow
                .collect(onNewDestinationAmountValueParsed)
        }
        var isEnteringSourceAmount by remember(isSourceInputShown) {
            mutableStateOf(isSourceInputShown)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .padding(
                    horizontal = 16.dp,
                )
        ) {
            val sourceAmountFocusRequester = remember(::FocusRequester)
            val destinationAmountFocusRequester = remember(::FocusRequester)

            if (isSourceInputShown) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isEnteringSourceAmount)
                                Modifier.border(
                                    width = 1.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(source.colorScheme.primary),
                                )
                            else
                                Modifier
                        )
                        .focusRequester(sourceAmountFocusRequester)
                        .focusable()
                        .clickable(
                            onClick = {
                                isEnteringSourceAmount = true
                                sourceAmountFocusRequester.requestFocus()
                            }
                        )
                ) {
                    BasicText(
                        text = "Source amount",
                        style = TextStyle(
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedAmountInputText(
                        amountInputState = sourceAmountInputState,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!isEnteringSourceAmount)
                            Modifier.border(
                                width = 1.dp,
                                shape = RoundedCornerShape(12.dp),
                                color = Color(destination.colorScheme.primary),
                            )
                        else
                            Modifier
                    )
                    .focusRequester(destinationAmountFocusRequester)
                    .focusable()
                    .clickable(
                        onClick = {
                            isEnteringSourceAmount = false
                            destinationAmountFocusRequester.requestFocus()
                        }
                    )
            ) {
                BasicText(
                    text = "Destination amount",
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedAmountInputText(
                    amountInputState = destinationAmountInputState,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                )
            }
        }

        BasicText(
            text = date.value.getText(),
            style = TextStyle(
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onDateClicked,
                )
                .padding(
                    top = 16.dp,
                    bottom = 8.dp,
                )
        )

        Box(
            contentAlignment = Alignment.Center,
        ) {
            var emptyMemoFieldOffset by remember {
                mutableStateOf(IntOffset.Zero)
            }

            if (memo.value.isEmpty()) {
                BasicText(
                    text = "Add a note",
                    style = TextStyle(
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                    ),
                    modifier = Modifier
                        .onSizeChanged { (width, _) ->
                            emptyMemoFieldOffset = IntOffset(
                                x = -width / 2,
                                y = 0,
                            )
                        }
                )
            }

            BasicTextField(
                value = memo.value,
                onValueChange = onMemoUpdated,
                textStyle = TextStyle(
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .offset {
                        if (memo.value.isEmpty())
                            emptyMemoFieldOffset
                        else
                            IntOffset.Zero
                    }
            )
        }

        AmountKeyboard(
            inputState =
                if (isEnteringSourceAmount)
                    sourceAmountInputState
                else
                    destinationAmountInputState,
            colorScheme =
                if (isEnteringSourceAmount)
                    source.colorScheme
                else
                    destination.colorScheme,
            mainAction =
                if (isEnteringSourceAmount)
                    AmountKeyboardMainAction.Next
                else
                    AmountKeyboardMainAction.Done,
            onMainActionClicked = { action ->
                when (action) {

                    AmountKeyboardMainAction.Done ->
                        onSaveClicked()

                    AmountKeyboardMainAction.Next ->
                        isEnteringSourceAmount = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
                .height(maxSheetHeightDp / 2.5f)
        )

        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
@Preview(
    heightDp = 2000,
    apiLevel = 34,
)
private fun TransferSheetPreview(
) = Column {
    val isSourceInputShownOptions = listOf(true, false)
    val isSaveEnabledOptions = listOf(true, false)
    val colorSchemesByName = HardcodedItemColorSchemeRepository()
        .getItemColorSchemesByName()
    val icons = DrawableResItemIconRepository()
        .getItemIcons()
    val categoryColorScheme = colorSchemesByName.getValue("Green2")
    val categoryIcon = icons[45]
    val accountColorScheme = colorSchemesByName.getValue("Yellow4")
    val accountIcon = icons[66]

    isSourceInputShownOptions.forEach { isSourceInputShown ->
        isSaveEnabledOptions.forEach { isSaveEnabled ->
            BasicText(
                text = "Source input shown: $isSourceInputShown," +
                        "\nSave enabled: $isSaveEnabled",
                modifier = Modifier.padding(vertical = 16.dp)
            )
            TransferSheet(
                isSourceInputShown = isSourceInputShown,
                source = ViewTransferCounterparty.Account(
                    accountTitle = "Source",
                    currency = ViewCurrency(
                        symbol = "A",
                        precision = 2,
                    ),
                    colorScheme = accountColorScheme,
                    icon = accountIcon,
                ),
                sourceAmountValue = BigInteger("133").let(::mutableStateOf),
                onNewSourceAmountValueParsed = {},
                destination = ViewTransferCounterparty.Category(
                    categoryTitle = "Destination",
                    subcategoryTitle = null,
                    currency = ViewCurrency(
                        symbol = "B",
                        precision = 2,
                    ),
                    colorScheme = categoryColorScheme,
                    icon = categoryIcon,
                ),
                destinationAmountValue = BigInteger("331").let(::mutableStateOf),
                onNewDestinationAmountValueParsed = {},
                memo = "".let(::mutableStateOf),
                date = ViewDate.today().let(::mutableStateOf),
                onMemoUpdated = {},
                subcategoryItemList =
                    ViewSelectableSubcategoryListItemPreviewParameterProvider()
                        .values
                        .toList()
                        .let(::mutableStateOf),
                subcategoriesColorScheme = categoryColorScheme.let(::mutableStateOf),
                onSubcategoryItemClicked = {},
                onSaveClicked = {},
                onDateClicked = {},
                onSourceClicked = { },
                onDestinationClicked = { },
            )
        }
    }
}
