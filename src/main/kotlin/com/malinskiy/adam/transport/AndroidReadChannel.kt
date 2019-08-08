/*
 * Copyright (C) 2019 Anton Malinskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.malinskiy.adam.transport

import com.android.ddmlib.logging.Log
import com.malinskiy.adam.Const
import com.malinskiy.adam.model.cmd.Response
import kotlinx.coroutines.io.ByteReadChannel

class AndroidReadChannel(private val delegate: ByteReadChannel) : ByteReadChannel by delegate {
    suspend fun read(): Response {
        val bytes = ByteArray(4)
        delegate.readFully(bytes, 0, 4)

        val ok = bytes.isOkay()
        val message = if (!ok) {
            delegate.readFully(bytes, 0, 4)
            val responseLength = String(bytes, Const.DEFAULT_TRANSPORT_ENCODING)
            val errorMessageLength = responseLength.toIntOrNull(16)
            if (errorMessageLength == null) {
                Log.w(TAG, "Unexpected error message length $responseLength")
                null
            } else {
                val errorBytes = ByteArray(errorMessageLength)
                delegate.readFully(errorBytes, 0, errorMessageLength)
                String(errorBytes, Const.DEFAULT_TRANSPORT_ENCODING)
            }
        } else {
            null
        }

        return Response(ok, message)
    }

    private fun ByteArray.isOkay(): Boolean {
        return this[0] == 'O'.toByte() &&
                this[1] == 'K'.toByte() &&
                this[2] == 'A'.toByte() &&
                this[3] == 'Y'.toByte()
    }

    companion object {
        val TAG: String = AndroidReadChannel::class.java.simpleName
    }
}