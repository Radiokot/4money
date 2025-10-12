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

package ua.com.radiokot.money.lock.logic

import android.os.SystemClock
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import ua.com.radiokot.money.MoneyAppActivity
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.lock.data.AppLockPreferences
import ua.com.radiokot.money.map

/**
 * A lock for app screens.
 * Locks if the passcode is set up and the app has been in background long enough.
 * It is purely visual, no encryption/decryption is happening under the hood.
 *
 * @see MoneyAppActivity.requiresUnlocking
 */
class AppLock(
    private val backgroundLockThresholdMs: Long,
    private val preferences: AppLockPreferences,
) {
    private val log by lazyLogger("AppLock")
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wentToBackgroundAtMs: Long = 0

    /**
     * If [isEnabled], length of the current passcode,
     * 0 otherwise.
     */
    val currentPasscodeLength: Int
        get() =
            preferences
                .appLockPasscode
                .value
                ?.length
                ?: 0

    val isEnabled: StateFlow<Boolean> =
        preferences
            .appLockPasscode
            .map(coroutineScope) { passcode ->
                passcode != null
            }

    var isLocked: Boolean = isEnabled.value
        private set

    fun unlock(passcode: String): Boolean =
        if (preferences.appLockPasscode.value == passcode) {
            isLocked = false

            log.debug {
                "unlock(): unlocked with passcode"
            }

            true
        } else {
            false
        }

    fun unlock(biometricPromptResult: BiometricPrompt.AuthenticationResult) {
        // Whatever.
        isLocked = false

        log.debug {
            "unlock(): unlocked with biometrics:" +
                    "\ntype=${biometricPromptResult.authenticationType}"
        }
    }

    fun onAppWentToBackground() {
        wentToBackgroundAtMs = SystemClock.uptimeMillis()

        log.debug {
            "onAppWentToBackground(): timestamp saved:" +
                    "\nwentToBackgroundAtMs=$wentToBackgroundAtMs"
        }
    }

    fun onAppReturnedToForeground() {

        if (!isEnabled.value || isLocked) {
            return
        }

        if (SystemClock.uptimeMillis() - wentToBackgroundAtMs > backgroundLockThresholdMs) {

            log.debug {
                "onAppReturnedToForeground(): locking as been in background long enough:" +
                        "\nwentToBackgroundAtMs=$wentToBackgroundAtMs"
            }

            isLocked = true
        } else {

            log.debug {
                "onAppReturnedToForeground(): not locking as not been in background long enough:" +
                        "\nwentToBackgroundAtMs=$wentToBackgroundAtMs"
            }
        }
    }
}
