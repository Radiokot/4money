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

package ua.com.radiokot.money.uikit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.composeunstyled.Text

@Composable
fun RedToggle(
    modifier: Modifier = Modifier,
    isToggled: State<Boolean>,
    transitionSpec: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = rememberRedToggleTransitionSpec(),
) {
    RedToggle(
        isToggled = isToggled.value,
        transitionSpec = transitionSpec,
        modifier = modifier
    )
}

@Composable
fun RedToggle(
    modifier: Modifier = Modifier,
    isToggled: Boolean,
    transitionSpec: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = rememberRedToggleTransitionSpec(),
) {
    AnimatedContent(
        targetState = isToggled,
        label = "red-toggle",
        transitionSpec = transitionSpec,
        modifier = modifier,
    ) { isToggled ->
        Text(
            text =
                if (isToggled)
                    "🔴"
                else
                    "⭕",
        )
    }
}

@Composable
fun rememberRedToggleTransitionSpec(): AnimatedContentTransitionScope<Boolean>.() -> ContentTransform =
    remember {
        fun AnimatedContentTransitionScope<Boolean>.() =
            if (targetState) {
                ContentTransform(
                    targetContentEnter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                        )
                    ),
                    initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
                    sizeTransform = null,
                )
            } else {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = scaleOut(),
                    sizeTransform = null,
                )
            }
    }
