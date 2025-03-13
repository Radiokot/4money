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

package ua.com.radiokot.money

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun rememberMoneyAppNavController(): NavHostController {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
        // The default spec causes significant delay 🤦🏻.
        animationSpec = tween(durationMillis = AnimationConstants.DefaultDurationMillis / 2),
    )

    val bottomSheetNavigator = remember(bottomSheetState) {
        BottomSheetNavigator(bottomSheetState)
    }

    return rememberNavController(
        bottomSheetNavigator,
    )
}

@Composable
fun MoneyAppModalBottomSheetLayout(
    moneyAppNavController: NavController,
) {
    val bottomSheetNavigator =
        moneyAppNavController.navigatorProvider.getNavigator(BottomSheetNavigator::class.java)

    // Pop back stack when hiding (click outside, drag) the current content.
    // Only do it if not navigating to another sheet.
    LaunchedEffect(bottomSheetNavigator) {
        var lastShownBackStackEntry: NavBackStackEntry? = null
        snapshotFlow { bottomSheetNavigator.navigatorSheetState.targetValue }
            .collect { targetSheetState ->
                val currentBackStackEntry = moneyAppNavController.currentBackStackEntry
                if (targetSheetState != ModalBottomSheetValue.Hidden) {
                    lastShownBackStackEntry = currentBackStackEntry
                } else if (currentBackStackEntry == lastShownBackStackEntry) {
                    moneyAppNavController.popBackStack()
                }
            }
    }

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
        ),
        sheetGesturesEnabled = false,
        content = {},
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    )
}
