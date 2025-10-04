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

package ua.com.radiokot.money.currency.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.math.BigInteger

/**
 * Animates counter-like transition to the [targetValue].
 */
@Composable
fun animateBigIntegerAsState(
    targetValue: BigInteger,
    animationSpec: AnimationSpec<Float> = tween(),
): State<BigInteger> {
    val animatable = remember { Animatable(0f) }
    var startValue by remember { mutableStateOf(targetValue) }
    var endValue by remember { mutableStateOf(targetValue) }

    val animatedState = remember {
        derivedStateOf {
            val progress = animatable.value
            if (progress >= 1f) {
                return@derivedStateOf endValue
            }

            val interpolated = (endValue - startValue)
                .toBigDecimal()
                .multiply(progress.toBigDecimal())
                .toBigInteger()

            startValue.add(interpolated)
        }
    }

    LaunchedEffect(targetValue) {
        startValue = animatedState.value
        endValue = targetValue

        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = animationSpec
        )
    }

    return animatedState
}
