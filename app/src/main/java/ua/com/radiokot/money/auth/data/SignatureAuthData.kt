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

package ua.com.radiokot.money.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignatureAuthData(
    /**
     * The encoded public key corresponding to the private key used to sign the [challenge].
     */
    @SerialName("pubkeyHex")
    val publicKeyHex: String,

    /**
     * Signed [challenge] received from the server.
     */
    @SerialName("challenge")
    val challenge: String,

    /**
     * The encoded signature of the [challenge].
     */
    @SerialName("signatureHex")
    val signatureHex: String,

    @SerialName("algorithm")
    val algorithm: String,
)
