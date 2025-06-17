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

package ua.com.radiokot.money.auth.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.decodeHex
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import ua.com.radiokot.money.auth.data.PhraseAuthData
import java.security.Security

@OptIn(ExperimentalStdlibApi::class)
object PhraseAuth {

    suspend fun authenticate(
        phraseSeed: ByteArray,
        authChallenge: String,
    ) = withContext(Dispatchers.Default) {

        require(phraseSeed.size == 64) {
            "The seed is expected to be 64 bytes (result of HMAC-SHA512)"
        }

        ensureBouncyCastleProvider()

        val privateKeyParameters = Ed25519PrivateKeyParameters(phraseSeed.copyOf(32))
        val publicKeyParameters = privateKeyParameters.generatePublicKey()

        val keyPair = AsymmetricCipherKeyPair(publicKeyParameters, privateKeyParameters)

        val signature = Ed25519Signer().run {
            init(true, keyPair.private)

            val message = authChallenge.toByteArray(Charsets.UTF_8)
            update(message, 0, message.size)

            generateSignature()
        }


        PhraseAuthData(
            publicKeyHex = publicKeyParameters
                .encoded
                .toHexString(),
            challenge = authChallenge,
            signatureHex = signature
                .toHexString(),
        )
    }

    suspend fun verify(
        data: PhraseAuthData,
    ): Boolean = withContext(Dispatchers.Default) {

        val publicKey = Ed25519PublicKeyParameters(data.publicKeyHex.decodeHex().toByteArray())

        Ed25519Signer().run {
            init(false, publicKey)

            val message = data.challenge.toByteArray(Charsets.UTF_8)
            update(message, 0, message.size)

            verifySignature(data.signatureHex.decodeHex().toByteArray())
        }
    }

    private fun ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
}
