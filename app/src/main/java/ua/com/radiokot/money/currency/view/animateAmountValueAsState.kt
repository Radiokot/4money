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
import androidx.compose.animation.core.VectorConverter
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
 * Animates transition of the [ViewAmount.value] to the [targetAmount]
 * as if it is a counter.
 */
@Composable
fun animateAmountValueAsState(
    targetAmount: ViewAmount,
    animationSpec: AnimationSpec<Int> = tween(),
): State<BigInteger> {

    val animatableInterpolationMultiplier = remember {
        Animatable(0, Int.VectorConverter)
    }
    var startValue by remember {
        mutableStateOf(targetAmount.value)
    }
    var endValue by remember {
        mutableStateOf(targetAmount.value)
    }
    val precisionMultiplier = remember {
        BigInteger.TEN.pow(targetAmount.currency.precision)
    }
    val animateDecimals by remember {
        derivedStateOf {
            endValue % precisionMultiplier != BigInteger.ZERO
                    || startValue % precisionMultiplier != BigInteger.ZERO
        }
    }

    val animatedValueState: State<BigInteger> = remember {
        derivedStateOf {
            val interpolationMultiplier = animatableInterpolationMultiplier.value
            if (interpolationMultiplier >= InterpolationAccuracy) {
                return@derivedStateOf endValue
            }

            var interpolated = (endValue - startValue)
                .multiply(interpolationMultiplier.toBigInteger())
                .divide(InterpolationAccuracyBI)

            if (!animateDecimals) {
                interpolated -= interpolated.remainder(precisionMultiplier)
            }

            startValue.add(interpolated)
        }
    }

    LaunchedEffect(targetAmount) {
        startValue = animatedValueState.value
        endValue = targetAmount.value

        animatableInterpolationMultiplier.snapTo(0)
        animatableInterpolationMultiplier.animateTo(
            targetValue = InterpolationAccuracy,
            animationSpec = animationSpec
        )
    }

    return animatedValueState
}

private const val InterpolationAccuracy = 1000
private val InterpolationAccuracyBI = InterpolationAccuracy.toBigInteger()
