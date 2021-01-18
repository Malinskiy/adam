/*
 * Copyright (C) 2021 Anton Malinskiy
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

package com.malinskiy.adam.request.emu

import com.malinskiy.adam.Const
import com.malinskiy.adam.transport.Socket
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File
import java.net.InetSocketAddress

/**
 * This request is completely different to other requests: it connects directly to emulator instead of adb server
 * For simplicity it can be used in the same way as adb server requests and shares the socket creation logic (and hence the coroutine
 * context) with other requests
 *
 * @param cmd use `help` to list all available commands, may be emulator-dependant
 * @param hostname usually localhost since emulator port is binded to the loopback interface only
 * @param port console port of the emulator. if your device is emulator-5554, port is 5554, emulator-5556 - 5556 and so on
 * @param authToken authentication token is required for connecting to emulator. If null, will be read from $HOME/.emulator_console_auth_token.
 * If you want to remove the auth completely - $HOME/.emulator_console_auth_token file should be empty
 * @param cleanResponse by default all responses contain the emulator auth output that is unnecessary. If you want  the output to include
 * everything the emulator returns - set this to false
 */
class EmulatorCommandRequest(
    private val cmd: String,
    val address: InetSocketAddress,
    private val authToken: String? = null,
    private val cleanResponse: Boolean = true
) {
    private suspend fun readAuthToken(): String? {
        val authTokenFile = File(System.getProperty("user.home"), ".emulator_console_auth_token")
        return if (authTokenFile.exists() && authTokenFile.isFile) {
            authTokenFile.readChannel().readUTF8Line()
        } else {
            null
        }
    }

    suspend fun process(socket: Socket): String {
        val sessionBuilder = StringBuilder()
        val token = authToken ?: readAuthToken() ?: ""
        if (token.isNotEmpty()) {
            sessionBuilder.append("auth $token\n")
        }
        sessionBuilder.append("$cmd\n")
        sessionBuilder.append("quit\n")

        socket.writeFully(sessionBuilder.toString().toByteArray())

        val buffer = ByteArray(1024)
        val output = StringBuilder()
        loop@ do {
            if (socket.isClosedForWrite || socket.isClosedForRead) break@loop

            val count = socket.readAvailable(buffer, 0, Const.MAX_PACKET_LENGTH)
            when {
                count == 0 -> {
                    continue@loop
                }
                count > 0 -> {
                    output.append(String(buffer, 0, count, Charsets.UTF_8))
                }
            }
        } while (count >= 0)

        val firstOkPosition = output.indexOf(OUTPUT_DELIMITER)
        val secondOkPosition = output.indexOf(OUTPUT_DELIMITER, firstOkPosition + 1)
        return output.substring(secondOkPosition + OUTPUT_DELIMITER.length)
    }

    companion object {
        /**
         * Note: the following messages are expected to be quite stable from emulator.
         * Emulator console will send the following message upon connection:
         *
         * Android Console: Authentication required
         * Android Console: type 'auth <auth_token>' to authenticate
         * Android Console: you can find your <auth_token> in
         * '/<path-to-home>/.emulator_console_auth_token'
         * OK\r\n
         *
         * and the following after authentication:
         * Android Console: type 'help' for a list of commands
         * OK\r\n
         *
         * So we try to search and skip first two "OK\r\n", return the rest.
         *
         */
        const val OUTPUT_DELIMITER: String = "OK\r\n"
    }
}
