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

package ua.com.radiokot.money.accounts.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.accounts.data.Account

@Composable
fun AccountTypeSelectionSheet(
    modifier: Modifier = Modifier,
    selectedType: Account.Type,
    onTypeClicked: (Account.Type) -> Unit,
) = BoxWithConstraints(
    modifier = modifier
        .background(Color.White)
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
) {

    val maxSheetHeightDp =
        if (maxHeight < 400.dp)
            maxHeight
        else
            maxHeight * 0.8f

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                max = maxSheetHeightDp,
            )
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Account type",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 16.dp
                    )
            )
        }
        itemsIndexed(
            items = Account.Type.entries,
        ) { i, item ->

            val itemModifier = Modifier
                .padding(
                    bottom = 4.dp,
                )
                .background(
                    shape = when (i) {
                        0 -> RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 6.dp,
                            bottomEnd = 6.dp
                        )

                        Account.Type.entries.size - 1 -> RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 6.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )

                        else -> RoundedCornerShape(6.dp)
                    },

                    color = if (item == selectedType)
                        Color(0xfff8efb3)
                    else
                        Color(0xfff8fafd)
                )
                .clickable(
                    onClick = {
                        onTypeClicked(item)
                    }
                )

            when (item) {
                Account.Type.Regular ->
                    AccountTypeItem(
                        icon = "👛",
                        title = "Regular",
                        description = "Cash, bank cards, etc.",
                        modifier = itemModifier
                    )

                Account.Type.Savings ->
                    AccountTypeItem(
                        icon = "🐹",
                        title = "Savings",
                        description = "Stash, cold wallet, etc.",
                        modifier = itemModifier
                    )
            }
        }
    }
}

@Composable
private fun AccountTypeItem(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    description: String,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .padding(12.dp)
) {
    Text(
        text = icon,
        fontSize = 36.sp,
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column(
        modifier = Modifier
            .weight(1f)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
        )

        Text(
            text = description,
            color = Color.Gray,
            modifier = Modifier
                .padding(
                    top = 4.dp,
                )
        )
    }
}

@Preview(
    apiLevel = 34,
)
@Composable
private fun Preview(

) {
    AccountTypeSelectionSheet(
        selectedType = Account.Type.Savings,
        onTypeClicked = {},
    )
}
