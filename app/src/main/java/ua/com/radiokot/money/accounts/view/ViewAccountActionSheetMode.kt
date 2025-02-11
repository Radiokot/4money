package ua.com.radiokot.money.accounts.view

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ViewAccountActionSheetMode {
    object Actions : ViewAccountActionSheetMode
    object Balance : ViewAccountActionSheetMode
}
