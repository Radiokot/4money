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

package ua.com.radiokot.money.auth.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.money.accounts.view.AccountsActivity
import ua.com.radiokot.money.uikit.TextButton

class AuthActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuthScreenRoot(
                viewModel = viewModel,
            )
        }

        lifecycleScope.launch {
            subscribeToEvents()
        }
    }

    private suspend fun subscribeToEvents(
    ): Nothing = viewModel.events.collect { event ->
        when (event) {
            AuthViewModel.Event.GoToMainScreen -> {
                startActivity(Intent(this, AccountsActivity::class.java))
                finish()
            }

            is AuthViewModel.Event.ShowFloatingError ->
                Toast.makeText(this, "Error: ${event.error::class.simpleName}", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(
            "AuthActivity",
            "onNewIntent(): received_new_intent:" +
                    "\nintent=$intent"
        )
        viewModel.onNewIntent(intent)
    }
}

@Composable
private fun AuthScreenRoot(
    viewModel: AuthViewModel,
) = AuthScreen(
    onAuthenticateClicked = viewModel::onAuthenticateClicked,
)

@Composable
private fun AuthScreen(
    onAuthenticateClicked: () -> Unit,
) = Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .safeDrawingPadding()
        .padding(16.dp)
        .fillMaxSize()
) {
    TextButton(
        text = "Authenticate",
        modifier = Modifier
            .clickable { onAuthenticateClicked() }
    )
}

@Composable
@Preview
private fun AuthScreenPreview() = AuthScreen(
    onAuthenticateClicked = {},
)
