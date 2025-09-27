package ua.com.radiokot.money.lock.data

import kotlinx.coroutines.flow.MutableStateFlow

interface AppLockPreferences {

    val appLockPasscode: MutableStateFlow<String?>
}
