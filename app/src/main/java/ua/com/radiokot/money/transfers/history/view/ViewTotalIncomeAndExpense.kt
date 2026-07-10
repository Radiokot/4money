package ua.com.radiokot.money.transfers.history.view

import androidx.compose.runtime.Immutable
import ua.com.radiokot.money.currency.view.ViewCurrency
import java.math.BigInteger

@Immutable
class ViewTotalIncomeAndExpense(
    val income: BigInteger,
    val expense: BigInteger,
    val currency: ViewCurrency,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewTotalIncomeAndExpense) return false

        if (income != other.income) return false
        if (expense != other.expense) return false
        if (currency != other.currency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = income.hashCode()
        result = 31 * result + expense.hashCode()
        result = 31 * result + currency.hashCode()
        return result
    }
}
