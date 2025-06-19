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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import ua.com.radiokot.money.auth.logic.UserSessionScope
import ua.com.radiokot.money.home.view.HomeActivity
import ua.com.radiokot.money.rememberMoneyAppNavController

class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UserSessionScope {
                Content(
                    goHome = {
                        startActivity(
                            Intent(this, HomeActivity::class.java)
                        )
                        finishAffinity()
                    },
                )
            }
        }
    }
}

@Composable
private fun Content(
    goHome: () -> Unit,
) {
    val navController = rememberMoneyAppNavController()

    NavHost(
        navController = navController,
        startDestination = TempAuthScreenRoute,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
    ) {
        tempAuth(
            onProceedToPhraseAuth = {
                navController.navigate(
                    route = PhraseAuthScreenRoute,
                )
            },
            onDone = goHome,
        )

        phraseAuth(
            onClose = navController::navigateUp,
            onDone = goHome,
        )
    }
}
