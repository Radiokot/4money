package ua.com.radiokot.money.accounts.view

import androidx.compose.runtime.Immutable

@Immutable
enum class ViewAccountActionSheetMode {
    Actions,
    Balance,
    TransferDestination,
    IncomeSource,
    ExpenseDestination,
    ;
}
