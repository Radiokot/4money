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

package ua.com.radiokot.money.colors.view

import android.icu.text.BreakIterator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Icon
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemIcon

@Composable
fun ItemLogo(
    modifier: Modifier = Modifier,
    title: String,
    colorScheme: ItemColorScheme,
    icon: ItemIcon?,
    shape: Shape = RoundedCornerShape(12.dp),
) = BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .background(
            color = Color(colorScheme.primary),
            shape = shape,
        )
) {
    if (icon != null) {
        Icon(
            painter = painterResource(icon.resId),
            contentDescription = "icon",
            tint = Color(colorScheme.onPrimary),
            modifier = Modifier
                .size((maxWidth * 0.5f))
        )
        return@BoxWithConstraints
    }

    // Fallback to letter (grapheme).
    val fontSizeSp = (maxWidth * 0.5f).value.sp / LocalDensity.current.fontScale

    val firstGrapheme = remember(title) {

        val firstGraphemeEndIndex =
            BreakIterator.getCharacterInstance().run {
                setText(title)
                next()
            }

        if (firstGraphemeEndIndex > 0)
            title.substring(0, firstGraphemeEndIndex)
        else
            "…"
    }

    BasicText(
        text = firstGrapheme,
        style = TextStyle(
            color = Color(colorScheme.onPrimary),
            fontSize = fontSizeSp,
        )
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {
    LazyVerticalGrid(
        columns = GridCells.FixedSize(40.dp),
        modifier = Modifier
            .widthIn(
                max = 120.dp,
            )
    ) {
        items(
            items = listOf(
                "Ole",
                "😸",
                "\uD83D\uDC77\uD83C\uDFFB\u200D♀\uFE0F", // Woman construction worker
                "\uD83D\uDC69\u200D\uD83D\uDD2C", // Woman chemist,
                "",
            )
        ) { title ->
            ItemLogo(
                title = title,
                colorScheme = HardcodedItemColorSchemeRepository()
                    .getItemColorSchemesByName()["Turquoise4"]!!,
                icon = null,
                modifier = Modifier
                    .size(42.dp)
                    .padding(1.dp)
            )
        }

        item {
            ItemLogo(
                title = "Oleg",
                colorScheme = HardcodedItemColorSchemeRepository()
                    .getItemColorSchemesByName()["Yellow4"]!!,
                icon = DrawableResItemIconRepository()
                    .getItemIconsByName()["finances_34"]!!,
                modifier = Modifier
                    .size(42.dp)
                    .padding(1.dp)
            )
        }
    }
}
