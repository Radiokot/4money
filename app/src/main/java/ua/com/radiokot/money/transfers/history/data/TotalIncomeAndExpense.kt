package ua.com.radiokot.money.transfers.history.data

import java.math.BigInteger

/**
 * Total of income and expense for an account for a certain period.
 * All non-negative.
 */
@JvmInline
value class TotalIncomeAndExpense(
    private val p: Pair<BigInteger, BigInteger>,
) {
    /**
     * Non-negative.
     */
    val income: BigInteger
        get() = p.first

    /**
     * Non-negative.
     */
    val expense: BigInteger
        get() = p.second
}
