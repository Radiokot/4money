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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
fun TransferSheet(
    onBackPressed: () -> Unit,
    isSourceInputShown: Boolean,
    sourceAmountValueFlow: StateFlow<BigInteger>,
    sourceAmountCurrency: ViewCurrency,
    onNewSourceAmountValueParsed: (BigInteger) -> Unit,
    destAmountValueFlow: StateFlow<BigInteger>,
    destAmountCurrency: ViewCurrency,
    onNewDestAmountValueParsed: (BigInteger) -> Unit,
    isSaveEnabled: Boolean,
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
                text = "Source",
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
                text = "Destination",
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
                        currency = sourceAmountCurrency,
                        amountFormat = amountFormat,
                        onNewValueParsed = onNewSourceAmountValueParsed,
                        onKeyboardSubmit = {
                            softKeyboard?.hide()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
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
                    valueFlow = destAmountValueFlow,
                    currency = destAmountCurrency,
                    amountFormat = amountFormat,
                    onNewValueParsed = onNewDestAmountValueParsed,
                    onKeyboardSubmit = {
                        softKeyboard?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            text = "Save",
            isEnabled = isSaveEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                )
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
@Preview
private fun TransferSheetPreview(
) = Column {

    BasicText(
        text = "With source input:",
        modifier = Modifier.padding(vertical = 16.dp)
    )
    TransferSheet(
        onBackPressed = {},
        isSourceInputShown = true,
        sourceAmountValueFlow = MutableStateFlow(BigInteger("133")),
        sourceAmountCurrency = ViewCurrency(
            symbol = "A",
            precision = 2,
        ),
        onNewSourceAmountValueParsed = {},
        destAmountValueFlow = MutableStateFlow(BigInteger("331")),
        destAmountCurrency = ViewCurrency(
            symbol = "B",
            precision = 2,
        ),
        onNewDestAmountValueParsed = {},
        isSaveEnabled = true,
    )

    BasicText(
        text = "Without source input:",
        modifier = Modifier.padding(vertical = 16.dp)
    )
    TransferSheet(
        onBackPressed = {},
        isSourceInputShown = false,
        sourceAmountValueFlow = MutableStateFlow(BigInteger("133")),
        sourceAmountCurrency = ViewCurrency(
            symbol = "A",
            precision = 2,
        ),
        onNewSourceAmountValueParsed = {},
        destAmountValueFlow = MutableStateFlow(BigInteger("331")),
        destAmountCurrency = ViewCurrency(
            symbol = "B",
            precision = 2,
        ),
        onNewDestAmountValueParsed = {},
        isSaveEnabled = true,
    )
}
