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

package ua.com.radiokot.money.lock.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import ua.com.radiokot.money.MoneyAppActivity
import ua.com.radiokot.money.R

class UnlockActivity : MoneyAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {

        val passcode = rememberSaveable {
            mutableStateOf("")
        }
        val length = 4

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
        ) {

            val inputWidth = min(maxWidth * 0.8f, 400.dp)
            val inputHeight = maxHeight * 0.6f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (this@BoxWithConstraints.maxHeight > 500.dp) {
                    Image(
                        painter = painterResource(R.drawable.pear_by_francesco_cesqo_stefanini_from_noun_project_cc_by_3_0),
                        contentScale = ContentScale.FillHeight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(108.dp)
                    )
                }

                Text(
                    text = "Enter the passcode",
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(32.dp))

                PasscodeInput(
                    passcode = passcode,
                    onPasscodeChanged = { newValue: String ->

                        passcode.value = newValue

                        if (newValue.length == length) {
                            if (lock.unlock(newValue)) {
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                passcode.value = ""

                                Toast
                                    .makeText(
                                        this@UnlockActivity,
                                        "Incorrect passcode",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        }
                    },
                    length = length,
                    onBackspaceClicked = {
                        passcode.value = passcode.value.dropLast(1)
                    },
                    modifier = Modifier
                        .size(
                            width = inputWidth,
                            height = inputHeight,
                        )
                )
            }
        }
    }
}
