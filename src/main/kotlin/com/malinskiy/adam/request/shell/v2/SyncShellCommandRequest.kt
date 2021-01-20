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

package com.malinskiy.adam.request.shell.v2

import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestValidationException
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.transport.Socket
import io.ktor.utils.io.*

/**
 * shell v2 service required for this request
 */
abstract class SyncShellCommandRequest<T : Any?>(val cmd: String, target: Target = NonSpecifiedTarget) :
    ComplexRequest<T>(target) {

    private val data = ByteArray(Const.MAX_PACKET_LENGTH)
    private val stdoutBuilder = StringBuilder()
    private val stderrBuilder = StringBuilder()
    private var exitCode: Int = -1

    /**
     * Descendants should override this method to map the response to appropriate output
     */
    abstract fun convertResult(response: ShellCommandResult): T

    override fun serialize() = createBaseRequest("shell,v2,raw:$cmd")
    override suspend fun readElement(socket: Socket): T {
        loop@ while (true) {
            when (val messageType = MessageType.of(socket.readByte().toInt())) {
                MessageType.STDOUT -> {
                    val length = socket.readIntLittleEndian()
                    socket.readFully(data, 0, length)
                    stdoutBuilder.append(String(data, 0, length, Const.DEFAULT_TRANSPORT_ENCODING))
                }
                MessageType.STDERR -> {
                    val length = socket.readIntLittleEndian()
                    socket.readFully(data, 0, length)
                    stderrBuilder.append(String(data, 0, length, Const.DEFAULT_TRANSPORT_ENCODING))
                }
                MessageType.EXIT -> {
                    val length = socket.readIntLittleEndian()
                    exitCode = socket.readByte().toInt()
                    break@loop
                }
                MessageType.STDIN, MessageType.CLOSE_STDIN, MessageType.WINDOW_SIZE_CHANGE, MessageType.INVALID -> {
                    throw RequestValidationException("Unsupported message $messageType")
                }
            }
        }

        val shellCommandResult = ShellCommandResult(
            stdout = stdoutBuilder.toString(),
            stderr = stderrBuilder.toString(),
            exitCode = exitCode
        )

        return convertResult(shellCommandResult)
    }
}
