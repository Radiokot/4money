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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.stableClickable
import ua.com.radiokot.money.uikit.AmountInputField
import ua.com.radiokot.money.uikit.TextButton
import java.math.BigInteger

@Composable
fun TransferSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: TransferSheetViewModel,
) {
    val source = viewModel.source.collectAsState().value
    val destination = viewModel.destination.collectAsState().value

    TransferSheet(
        isSourceInputShown = viewModel.isSourceInputShown.collectAsState().value,
        source = source
            ?: return,
        sourceAmountValue = viewModel.sourceAmountValue.collectAsState(),
        onNewSourceAmountValueParsed = viewModel::onNewSourceAmountValueParsed,
        destination = destination
            ?: return,
        destinationAmountValue = viewModel.destinationAmountValue.collectAsState(),
        onNewDestinationAmountValueParsed = viewModel::onNewDestinationAmountValueParsed,
        memo = viewModel.memo.collectAsStateWithLifecycle(),
        date = viewModel.date.collectAsStateWithLifecycle(),
        onMemoUpdated = viewModel::onMemoUpdated,
        subcategoryItemList = viewModel.subcategoryItemList.collectAsState(),
        onSubcategoryItemClicked = viewModel::onSubcategoryItemClicked,
        isSaveEnabled = viewModel.isSaveEnabled.collectAsState(),
        onSaveClicked = viewModel::onSaveClicked,
        modifier = modifier,
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
    onSubcategoryItemClicked: (ViewSelectableSubcategoryListItem) -> Unit,
    isSaveEnabled: State<Boolean>,
    onSaveClicked: () -> Unit,
) = BoxWithConstraints(
    modifier = modifier
        .background(Color(0xfff0f4f8))
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
) {

    val maxSheetHeightDp =
        if (maxHeight < 400.dp)
            maxHeight
        else
            maxHeight * 0.8f

    val locale = LocalConfiguration.current.locales.get(0)
    val amountFormat = remember(locale) {
        ViewAmountFormat(locale)
    }

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

            BasicText(
                text = shortSourceTitle ?: source.title,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        vertical = 24.dp,
                        horizontal = 8.dp,
                    )
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.Gray)
            )

            BasicText(
                text = shortDestinationTitle ?: destination.title,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        vertical = 24.dp,
                        horizontal = 8.dp,
                    )
            )
        }

        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color.Gray)
        )


        if (subcategoryItemList.value.isNotEmpty()) {
            SelectableSubcategoryRow(
                itemList = subcategoryItemList,
                onItemClicked = onSubcategoryItemClicked,
                modifier = Modifier
                    .padding(
                        vertical = 12.dp,
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .padding(
                    horizontal = 16.dp,
                )
        ) {
            val softKeyboard = LocalSoftwareKeyboardController.current
            val sourceAmountFocusRequester = remember {
                FocusRequester()
            }
            val destinationAmountFocusRequester = remember {
                FocusRequester()
            }

            if (isSourceInputShown) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    BasicText(
                        text = "Source amount",
                        style = TextStyle(
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AmountInputField(
                        value = sourceAmountValue,
                        currency = source.currency,
                        amountFormat = amountFormat,
                        onNewValueParsed = onNewSourceAmountValueParsed,
                        onKeyboardSubmit = {
                            destinationAmountFocusRequester.requestFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(sourceAmountFocusRequester)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                BasicText(
                    text = "Destination amount",
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                AmountInputField(
                    value = destinationAmountValue,
                    currency = destination.currency,
                    amountFormat = amountFormat,
                    onNewValueParsed = onNewDestinationAmountValueParsed,
                    onKeyboardSubmit = {
                        softKeyboard?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(destinationAmountFocusRequester)
                )
            }

            LaunchedEffect(isSourceInputShown) {
                if (isSourceInputShown) {
                    sourceAmountFocusRequester.requestFocus()
                } else {
                    destinationAmountFocusRequester.requestFocus()
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
        ) {
            var emptyMemoFieldOffset by remember {
                mutableStateOf(IntOffset.Zero)
            }

            if (memo.value.isEmpty()) {
                BasicText(
                    text = "Notes…",
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
                    .padding(16.dp)
                    .offset {
                        if (memo.value.isEmpty())
                            emptyMemoFieldOffset
                        else
                            IntOffset.Zero
                    }
            )
        }

        TextButton(
            text = "Save",
            isEnabled = isSaveEnabled.value,
            modifier = Modifier
                .stableClickable(
                    onClick = onSaveClicked,
                )
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                )
        )

        BasicText(
            text = when (date.value.specificType) {
                ViewDate.SpecificType.Today ->
                    "Today"

                ViewDate.SpecificType.Yesterday ->
                    "Yesterday"

                null ->
                    date.value.localDate.toString()
            },
            style = TextStyle(
                textAlign = TextAlign.Center,
                color = Color.DarkGray,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
@Preview(
    heightDp = 2000,
)
private fun TransferSheetPreview(
) = Column {
    val isSourceInputShownOptions = listOf(true, false)
    val isSaveEnabledOptions = listOf(true, false)

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
                ),
                sourceAmountValue = BigInteger("133").let(::mutableStateOf),
                onNewSourceAmountValueParsed = {},
                destination = ViewTransferCounterparty.Account(
                    accountTitle = "Destination",
                    currency = ViewCurrency(
                        symbol = "B",
                        precision = 2,
                    ),
                ),
                destinationAmountValue = BigInteger("331").let(::mutableStateOf),
                onNewDestinationAmountValueParsed = {},
                subcategoryItemList =
                ViewSelectableSubcategoryListItemPreviewParameterProvider()
                    .values
                    .toList()
                    .let(::mutableStateOf),
                onSubcategoryItemClicked = {},
                isSaveEnabled = isSaveEnabled.let(::mutableStateOf),
                onSaveClicked = {},
                memo = "".let(::mutableStateOf),
                date = ViewDate.today().let(::mutableStateOf),
                onMemoUpdated = {},
            )
        }
    }
}
