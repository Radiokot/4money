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

package ua.com.radiokot.money.categories.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.uikit.TextButton

@Composable
private fun EditSubcategoryScreen(
    isNewSubcategory: Boolean,
    isSaveEnabled: State<Boolean>,
    onSaveClicked: () -> Unit,
    title: State<String>,
    onTitleChanged: (String) -> Unit,
    onCloseClicked: () -> Unit,
) = Column(
    modifier = Modifier
        .windowInsetsPadding(
            WindowInsets.navigationBars
                .only(WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
        )
        .padding(
            horizontal = 16.dp,
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
            text =
            if (isNewSubcategory)
                "Add subcategory"
            else
                "Edit subcategory",
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 16.dp,
                )
        )

        TextButton(
            text = "✔️",
            isEnabled = isSaveEnabled.value,
            padding = buttonPadding,
            modifier = Modifier
                .clickable(
                    enabled = isSaveEnabled.value,
                    onClick = onSaveClicked,
                )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Title",
    )

    Spacer(modifier = Modifier.height(6.dp))

    val focusRequester = remember {
        FocusRequester()
    }

    BasicTextField(
        value = title.value,
        onValueChange = onTitleChanged,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.DarkGray,
            )
            .padding(12.dp)
            .focusRequester(focusRequester)
    )

    Spacer(modifier = Modifier.height(24.dp))

    TextButton(
        text = "Continue",
        isEnabled = isSaveEnabled.value,
        modifier = Modifier
            .fillMaxWidth(1f)
            .clickable(
                enabled = isSaveEnabled.value,
                onClick = onSaveClicked,
            )
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun EditSubcategoryScreenRoot(
    viewModel: EditSubcategoryScreenViewModel,
) {
    EditSubcategoryScreen(
        isNewSubcategory = viewModel.isNewSubcategory,
        isSaveEnabled = viewModel.isSaveEnabled.collectAsState(),
        onSaveClicked = remember { viewModel::onSaveClicked },
        title = viewModel.title.collectAsState(),
        onTitleChanged = remember { viewModel::onTitleChanged },
        onCloseClicked = remember { viewModel::onCloseClicked },
    )
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun EditAccountScreenPreview(

) {
    EditSubcategoryScreen(
        isNewSubcategory = true,
        isSaveEnabled = false.let(::mutableStateOf),
        onSaveClicked = {},
        title = "Radio".let(::mutableStateOf),
        onTitleChanged = {},
        onCloseClicked = {},
    )
}
