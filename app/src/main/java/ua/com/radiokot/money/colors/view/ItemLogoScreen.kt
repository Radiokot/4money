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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import kotlinx.coroutines.launch
import ua.com.radiokot.money.colors.data.DrawableResItemIconRepository
import ua.com.radiokot.money.colors.data.HardcodedItemColorSchemeRepository
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.colors.data.ItemIcon
import ua.com.radiokot.money.colors.data.ItemLogoType
import ua.com.radiokot.money.plus
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun ItemLogoScreen(
    modifier: Modifier = Modifier,
    logoType: ItemLogoType,
    itemTitle: String,
    colorSchemeList: State<List<ItemColorScheme>>,
    selectedColorScheme: State<ItemColorScheme>,
    onColorSchemeClicked: (ItemColorScheme) -> Unit,
    iconList: State<List<ItemIcon>>,
    selectedIcon: State<ItemIcon?>,
    onIconClicked: (ItemIcon?) -> Unit,
    onCloseClicked: () -> Unit,
    onSaveClicked: () -> Unit,
) = Column(
    modifier = modifier
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .only(WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
        )
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 56.dp,
            )
            .padding(
                horizontal = 16.dp,
            )
    ) {
        val buttonPadding = remember {
            PaddingValues(6.dp)
        }

        TextButton(
            text = "❌",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onCloseClicked,
                )
        )

        Text(
            text = when (logoType) {
                ItemLogoType.Account ->
                    "Account logo"

                ItemLogoType.Category ->
                    "Category logo"
            },
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )

        TextButton(
            text = "✔️",
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    onClick = onSaveClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    val logoTransitionSpec = remember {

        fun AnimatedContentTransitionScope<Pair<ItemColorScheme, ItemIcon?>>.() =
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
                sizeTransform = null,
            )
    }

    AnimatedContent(
        targetState = selectedColorScheme.value to selectedIcon.value,
        transitionSpec = logoTransitionSpec,
        label = "logo",
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
    ) { (colorScheme, icon) ->

        ItemLogo(
            title = itemTitle,
            colorScheme = colorScheme,
            icon = icon,
            modifier = Modifier
                .size(72.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    val pages: List<Page> = remember {
        listOf(
            Page.Icon,
            Page.Color,
        )
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = pages::size,
    )
    val coroutineScope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        BasicText(
            text = "Icon",
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                textDecoration =
                    if (pagerState.currentPage == pages.indexOf(Page.Icon))
                        TextDecoration.Underline
                    else
                        null,
            ),
            modifier = Modifier
                .clickable {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = pages.indexOf(Page.Icon),
                        )
                    }
                }
        )

        BasicText(
            text = "Color",
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                textDecoration =
                    if (pagerState.currentPage == pages.indexOf(Page.Color))
                        TextDecoration.Underline
                    else
                        null,
            ),
            modifier = Modifier
                .clickable {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = pages.indexOf(Page.Color),
                        )
                    }
                }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    val pickerContentPadding =
        PaddingValues(
            vertical = 16.dp,
        ) + WindowInsets
            .navigationBars
            .asPaddingValues()

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = pages.size - 1,
        verticalAlignment = Alignment.Top,
        key = Int::unaryPlus,
        modifier = Modifier
            .fillMaxSize()
    ) { pageIndex ->
        when (pages[pageIndex]) {

            Page.Icon -> {
                IconPicker(
                    iconList = iconList,
                    selectedIcon = selectedIcon,
                    onIconClicked = onIconClicked,
                    contentPadding = pickerContentPadding,
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                        )
                )
            }

            Page.Color -> {
                ColorSchemePicker(
                    colorSchemeList = colorSchemeList,
                    selectedColorScheme = selectedColorScheme,
                    onColorSchemeClicked = onColorSchemeClicked,
                    contentPadding = pickerContentPadding,
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                        )
                )
            }
        }
    }
}

@Composable
fun ItemLogoScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ItemLogoScreenViewModel,
) {
    ItemLogoScreen(
        logoType = viewModel.logoType,
        itemTitle = viewModel.itemTitle,
        colorSchemeList = viewModel.colorSchemeList.collectAsState(),
        selectedColorScheme = viewModel.selectedColorScheme.collectAsState(),
        onColorSchemeClicked = remember { viewModel::onColorSchemeClicked },
        iconList = viewModel.iconList.collectAsState(),
        selectedIcon = viewModel.selectedIcon.collectAsState(),
        onIconClicked = remember { viewModel::onIconClicked },
        onCloseClicked = remember { viewModel::onCloseClicked },
        onSaveClicked = remember { viewModel::onSaveClicked },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun Preview(

) {
    val colorSchemes = HardcodedItemColorSchemeRepository()
        .getItemColorSchemes()
    val selectedColorScheme = remember {
        mutableStateOf(colorSchemes[12])
    }
    val icons = DrawableResItemIconRepository()
        .getItemIcons()
    val selectedIcon = remember {
        mutableStateOf<ItemIcon?>(null)
    }

    ItemLogoScreen(
        logoType = ItemLogoType.Account,
        itemTitle = "🤗",
        colorSchemeList =
            colorSchemes
                .let(::mutableStateOf),
        selectedColorScheme = selectedColorScheme,
        onColorSchemeClicked = { selectedColorScheme.value = it },
        iconList =
            icons
                .let(::mutableStateOf),
        selectedIcon = selectedIcon,
        onIconClicked = { selectedIcon.value = it },
        onCloseClicked = { },
        onSaveClicked = { },
    )
}

private enum class Page {
    Icon,
    Color,
}
