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

package ua.com.radiokot.money.home.view

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import io.ktor.util.reflect.instanceOf
import ua.com.radiokot.money.R
import ua.com.radiokot.money.accounts.view.AccountsScreenFragment
import ua.com.radiokot.money.auth.view.UserSessionScopeActivity
import ua.com.radiokot.money.categories.view.CategoriesScreenFragment
import ua.com.radiokot.money.uikit.TextButton
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class HomeActivity : UserSessionScopeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (goToAuthIfNoSession()) {
            return
        }

        val fragmentContainer = FrameLayout(this).apply {
            id = R.id.fragmentContainer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
            ).apply {
                weight = 1f
            }
        }

        fun show(fragment: KClass<out Fragment>) {
            if (supportFragmentManager.findFragmentById(fragmentContainer.id)
                    ?.instanceOf(fragment) == true
            ) {
                return
            }

            supportFragmentManager.commitNow {
                disallowAddToBackStack()
                replace(fragmentContainer.id, fragment.createInstance())
            }
        }

        val bottomNavigation = ComposeView(this).apply {
            setContent {
                BottomNavigation(
                    onAccountsClicked = { show(AccountsScreenFragment::class) },
                    onCategoriesClicked = { show(CategoriesScreenFragment::class) },
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        setContentView(LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            orientation = LinearLayout.VERTICAL

            addView(fragmentContainer)
            addView(bottomNavigation)
        })

        if (savedInstanceState == null) {
            show(AccountsScreenFragment::class)
        }
    }
}

@Composable
private fun BottomNavigation(
    onAccountsClicked: () -> Unit,
    onCategoriesClicked: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(
        16.dp,
        Alignment.CenterHorizontally
    ),
    modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xfff0edf1))
        .padding(
            vertical = 12.dp,
        )
) {
    val clickableAccountsModifier = remember {
        Modifier.clickable { onAccountsClicked() }
    }

    TextButton(
        text = "👛 Accounts",
        modifier = Modifier
            .then(clickableAccountsModifier),
    )

    val clickableCategoriesModifier = remember {
        Modifier.clickable { onCategoriesClicked() }
    }

    TextButton(
        text = "📊 Categories",
        modifier = Modifier
            .then(clickableCategoriesModifier)
    )
}
