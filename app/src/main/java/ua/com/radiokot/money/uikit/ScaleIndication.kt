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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * Animates scale decrease of a pressed element.
 */
class ScaleIndication(
    private val pressedScale: Float = 0.95f,
    private val animationSpec: AnimationSpec<Float> = tween(100),
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        ScaleIndicationNode(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this

    inner class ScaleIndicationNode(
        private val interactionSource: InteractionSource,
    ) : Modifier.Node(),
        DrawModifierNode {

        private val animatableScale = Animatable(1f)
        private var isPressed = false
            set(value) {
                field = value
                coroutineScope.launch {
                    animatableScale.animateTo(
                        targetValue =
                            if (value)
                                pressedScale
                            else
                                1f,
                        animationSpec = animationSpec,
                    )
                }
            }
        private var isHovered = false
        private var isFocused = false

        override fun onAttach() {
            coroutineScope.launch {
                val collectedPresses = mutableSetOf<PressInteraction>()
                val collectedHovers = mutableSetOf<HoverInteraction>()
                val collectedFocuses = mutableSetOf<FocusInteraction>()
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press ->
                            collectedPresses.add(interaction)

                        is PressInteraction.Release ->
                            collectedPresses.remove(interaction.press)

                        is PressInteraction.Cancel ->
                            collectedPresses.remove(interaction.press)

                        is HoverInteraction.Enter ->
                            collectedHovers.add(interaction)

                        is HoverInteraction.Exit ->
                            collectedHovers.remove(interaction.enter)

                        is FocusInteraction.Focus ->
                            collectedFocuses.add(interaction)

                        is FocusInteraction.Unfocus ->
                            collectedFocuses.remove(interaction.focus)
                    }
                    val pressed = collectedPresses.isNotEmpty()
                    val hovered = collectedHovers.isNotEmpty()
                    val focused = collectedFocuses.isNotEmpty()
                    var invalidateNeeded = false
                    if (isPressed != pressed) {
                        isPressed = pressed
                        invalidateNeeded = true
                    }
                    if (isHovered != hovered) {
                        isHovered = hovered
                        invalidateNeeded = true
                    }
                    if (isFocused != focused) {
                        isFocused = focused
                        invalidateNeeded = true
                    }
                    if (invalidateNeeded) invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            scale(
                scaleX = animatableScale.value,
                scaleY = animatableScale.value,
                pivot = center,
            ) {
                this@draw.drawContent()
                if (isHovered || isFocused) {
                    drawRect(color = Color.Black.copy(alpha = 0.1f))
                }
            }
        }
    }
}
