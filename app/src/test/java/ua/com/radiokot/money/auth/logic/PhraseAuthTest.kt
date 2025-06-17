package ua.com.radiokot.money.auth.logic

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.money.auth.data.PhraseAuthData

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
class PhraseAuthTest {

    @Test
    fun authenticateAndVerifySuccessfully() {

        val seed =
            Mnemonics.MnemonicCode("tonight fat have keen intact happy social powder tired shaft length cram")
                .toSeed()
        val challenge = "You, sir, are a fish!"

        val output = runBlocking {
            PhraseAuth.authenticate(
                phraseSeed = seed,
                authChallenge = challenge,
            )
        }

        Assert.assertEquals(
            "221bf436021133aa79da5c1bea7b04b78a0a7b9b9d2760bd19ae68ae3dd435ee".lowercase(),
            output.publicKeyHex.lowercase(),
        )
        Assert.assertEquals(
            64,
            output.publicKeyHex.length,
        )
        Assert.assertEquals(
            "1860a5f3273bc30725991ff835f08185de617e2a6c6cf1f87d3436d6446b6209d8cb84f6e09651e213b51a54dbd4c5508d1432b3290b4d162ef865bd0af76e02".lowercase(),
            output.signatureHex.lowercase(),
        )
        Assert.assertEquals(
            128,
            output.signatureHex.length,
        )
        Assert.assertEquals(
            challenge,
            output.challenge,
        )
        Assert.assertTrue(
            runBlocking { PhraseAuth.verify(output) }
        )
    }

    @Test(
        expected = IllegalArgumentException::class,
    )
    fun failAuthentication_IfSeedSizeIsWrong() {
        runBlocking {
            PhraseAuth.authenticate(
                phraseSeed = byteArrayOf(1, 2, 3),
                authChallenge = "challenge",
            )
        }
    }

    @Test
    fun failVerification_IfSignatureMismatch() {
        Assert.assertFalse(runBlocking {
            PhraseAuth.verify(
                data = PhraseAuthData(
                    publicKeyHex = "221bf436021133aa79da5c1bea7b04b78a0a7b9b9d2760bd19ae68ae3dd435ee",
                    challenge = "oiiaioiiiai",
                    signatureHex = "1860a5f3273bc30725991ff835f08185de617e2a6c6cf1f87d3436d6446b6209d8cb84f6e09651e213b51a54dbd4c5508d1432b3290b4d162ef865bd0af76e02"
                )
            )
        })
    }
}
