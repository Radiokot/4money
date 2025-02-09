package ua.com.radiokot.money.uikit

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ua.com.radiokot.money.currency.view.ViewAmount
import ua.com.radiokot.money.currency.view.ViewCurrency
import java.math.BigInteger

class ViewAmountPreviewParameterProvider : PreviewParameterProvider<ViewAmount> {
    override val values: Sequence<ViewAmount>
        get() = sequenceOf(
            ViewAmount(
                value = BigInteger("1234"),
                currency = ViewCurrency(
                    symbol = "$",
                    precision = 2,
                ),
            ),
            ViewAmount(
                value = BigInteger("567890"),
                currency = ViewCurrency(
                    symbol = "€",
                    precision = 2,
                ),
            ),
            ViewAmount(
                value = BigInteger("62788"),
                currency = ViewCurrency(
                    symbol = "₿",
                    precision = 8,
                ),
            ),
            ViewAmount(
                value = BigInteger("210"),
                currency = ViewCurrency(
                    symbol = "sat",
                    precision = 0,
                ),
            ),
            ViewAmount(
                value = BigInteger("0"),
                currency = ViewCurrency(
                    symbol = "г",
                    precision = 2,
                ),
            ),
            ViewAmount(
                value = BigInteger("-5000"),
                currency = ViewCurrency(
                    symbol = "$",
                    precision = 2,
                ),
            ),
        )
}
