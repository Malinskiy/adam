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

import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import com.malinskiy.adam.exception.RequestValidationException
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withMaxPacketBuffer

/**
 * shell v2 service required for this request
 */
abstract class SyncShellCommandRequest<T : Any?>(val cmd: String, target: Target = NonSpecifiedTarget, socketIdleTimeout: Long? = null) :
    ComplexRequest<T>(target, socketIdleTimeout) {

    private val stdoutBuilder: ByteArrayDataOutput = ByteStreams.newDataOutput()
    private val stderrBuilder: ByteArrayDataOutput = ByteStreams.newDataOutput()
    private var buffer: ByteArray = ByteArray(nextDataSize(0))
    private var exitCode: Int = -1

    /**
     * Descendants should override this method to map the response to appropriate output
     */
    abstract fun convertResult(response: ShellCommandResult): T

    override fun serialize() = createBaseRequest("shell,v2,raw:$cmd")
    override suspend fun readElement(socket: Socket): T {
        withMaxPacketBuffer {
            loop@ while (true) {
                when (val messageType = MessageType.of(socket.readByte().toInt())) {
                    MessageType.STDOUT -> stdoutBuilder.write(socket)

                    MessageType.STDERR -> stderrBuilder.write(socket)

                    MessageType.EXIT -> {
                        //ignoredLength
                        socket.readIntLittleEndian()
                        exitCode = socket.readByte().toInt()
                        break@loop
                    }
                    
                    MessageType.STDIN, MessageType.CLOSE_STDIN, MessageType.WINDOW_SIZE_CHANGE, MessageType.INVALID -> {
                        throw RequestValidationException("Unsupported message $messageType")
                    }
                }
            }
        }

        val shellCommandResult = ShellCommandResult(
            stdout = stdoutBuilder.toByteArray(),
            stderr = stderrBuilder.toByteArray(),
            exitCode = exitCode
        )

        return convertResult(shellCommandResult)
    }

    private suspend fun ByteArrayDataOutput.write(socket: Socket): Unit {
        val length = socket.readIntLittleEndian()
        if (length > buffer.size) { // Grow buffer as needed.
            buffer = ByteArray(nextDataSize(length))
        }
        socket.readFully(buffer, 0, length)
        this.write(buffer, 0, length)
    }

    companion object {

        /** Gets the next size for the data buffer in powers of two / exponential growth. */
        private fun nextDataSize(requestedLength: Int): Int {
            var size = 1 shl 14 // = 2^14 initial buffer; 8 and 16k are common in most frameworks
            while (requestedLength > size) {
                size = size shl 1
            }
            return size
        }
    }
}
