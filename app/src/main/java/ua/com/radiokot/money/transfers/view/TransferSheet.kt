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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ua.com.radiokot.money.currency.view.ViewAmountFormat
import ua.com.radiokot.money.currency.view.ViewCurrency
import ua.com.radiokot.money.uikit.AmountInputField
import ua.com.radiokot.money.uikit.TextButton
import java.math.BigInteger

@Composable
fun TransferSheetRoot(
    modifier: Modifier = Modifier,
    viewModel: TransferSheetViewModel,
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
        val source = viewModel.source.collectAsState().value
        val destination = viewModel.destination.collectAsState().value

        TransferSheet(
            onBackPressed = viewModel::onBackPressed,
            isSourceInputShown = viewModel.isSourceInputShown.collectAsState().value,
            source = source
                ?: return@AnimatedVisibility,
            sourceAmountValueFlow = viewModel.sourceAmountValue,
            onNewSourceAmountValueParsed = viewModel::onNewSourceAmountValueParsed,
            destination = destination
                ?: return@AnimatedVisibility,
            destinationAmountValueFlow = viewModel.destinationAmountValue,
            onNewDestinationAmountValueParsed = viewModel::onNewDestinationAmountValueParsed,
            isSaveEnabled = viewModel.isSaveEnabled.collectAsState().value,
            onSaveClicked = viewModel::onSaveClicked,
        )
    }
}

@Composable
private fun TransferSheet(
    onBackPressed: () -> Unit,
    isSourceInputShown: Boolean,
    source: ViewTransferCounterparty,
    sourceAmountValueFlow: StateFlow<BigInteger>,
    onNewSourceAmountValueParsed: (BigInteger) -> Unit,
    destination: ViewTransferCounterparty,
    destinationAmountValueFlow: StateFlow<BigInteger>,
    onNewDestinationAmountValueParsed: (BigInteger) -> Unit,
    isSaveEnabled: Boolean,
    onSaveClicked: () -> Unit,
) = BoxWithConstraints {

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
            .shadow(8.dp)
            .safeDrawingPadding()
            .fillMaxWidth()
            .heightIn(
                max = maxSheetHeightDp,
            )
            .verticalScroll(rememberScrollState())
            .background(Color(0xfff0f4f8))
    ) {
        BackHandler(onBack = onBackPressed)

        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
        ) {
            BasicText(
                text = source.title,
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
                text = destination.title,
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

        Spacer(modifier = Modifier.height(24.dp))

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
                        valueFlow = sourceAmountValueFlow,
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
                    valueFlow = destinationAmountValueFlow,
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

        Spacer(modifier = Modifier.height(16.dp))

        val clickableSaveModifier = remember {
            Modifier.clickable { onSaveClicked() }
        }

        TextButton(
            text = "Save",
            isEnabled = isSaveEnabled,
            modifier = Modifier
                .then(clickableSaveModifier)
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                )
        )

        Spacer(modifier = Modifier.height(24.dp))
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
                onBackPressed = {},
                isSourceInputShown = isSourceInputShown,
                source = ViewTransferCounterparty(
                    title = "Source",
                    currency = ViewCurrency(
                        symbol = "A",
                        precision = 2,
                    )
                ),
                sourceAmountValueFlow = MutableStateFlow(BigInteger("133")),
                onNewSourceAmountValueParsed = {},
                destination = ViewTransferCounterparty(
                    title = "Destination",
                    currency = ViewCurrency(
                        symbol = "B",
                        precision = 2,
                    )
                ),
                destinationAmountValueFlow = MutableStateFlow(BigInteger("331")),
                onNewDestinationAmountValueParsed = {},
                isSaveEnabled = isSaveEnabled,
                onSaveClicked = {},
            )
        }
    }
}
