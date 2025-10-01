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

package ua.com.radiokot.money.lock.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.viewmodel.koinViewModel
import ua.com.radiokot.money.MoneyAppActivity
import ua.com.radiokot.money.auth.logic.UserSessionScope

class SetUpPasscodeActivity : MoneyAppActivity(
    requiresUnlocking = false,
    requiresSession = true,
) {

    override fun onCreateAllowed(savedInstanceState: Bundle?) {

        enableEdgeToEdge()

        setContent {
            UserSessionScope {
                Content(
                    finishActivity = ::finish,
                )
            }
        }
    }
}

@Composable
private fun Content(
    finishActivity: () -> Unit,
) {
    val viewModel: SetUpPasscodeScreenViewModel = koinViewModel()
    val context by rememberUpdatedState(LocalContext.current)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetUpPasscodeScreenViewModel.Event.Done,
                SetUpPasscodeScreenViewModel.Event.Close,
                    -> {
                    finishActivity()
                }

                SetUpPasscodeScreenViewModel.Event.ShowMismatchError -> {
                    Toast
                        .makeText(
                            context,
                            "Passcodes don't match, try again",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }

            }
        }
    }

    SetUpPasscodeScreen(
        viewModel = viewModel,
    )
}
