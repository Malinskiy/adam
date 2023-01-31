/*
 * Copyright (C) 2020 Anton Malinskiy
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

import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withMaxFilePacketBuffer
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

open class ChanneledShellCommandRequest(
    private val cmd: String,
    channel: ReceiveChannel<ShellCommandInputChunk>,
    target: Target = NonSpecifiedTarget,
    socketIdleTimeout: Long? = null,
) : AsyncChannelRequest<ShellCommandResultChunk, ShellCommandInputChunk>(
    target = target,
    channel = channel,
    socketIdleTimeout = socketIdleTimeout
) {

    override suspend fun readElement(socket: Socket, sendChannel: SendChannel<ShellCommandResultChunk>): Boolean {
        withMaxFilePacketBuffer {
            val data = array()
            when (MessageType.of(socket.readByte().toInt())) {
                MessageType.STDOUT -> {
                    val length = socket.readIntLittleEndian()
                    socket.readFully(data, 0, length)
                    sendChannel.send(ShellCommandResultChunk(stdout = data.copyOfRange(0, length)))
                }

                MessageType.STDERR -> {
                    val length = socket.readIntLittleEndian()
                    socket.readFully(data, 0, length)
                    sendChannel.send(ShellCommandResultChunk(stderr = data.copyOfRange(0, length)))
                }

                MessageType.EXIT -> {
                    //ignoredLength
                    socket.readIntLittleEndian()
                    val exitCode = socket.readByte().toInt()
                    sendChannel.send(ShellCommandResultChunk(exitCode = exitCode))
                    return true
                }

                else -> Unit
            }

            return false
        }
    }

    /**
     * Handles stdin
     */
    override suspend fun writeElement(element: ShellCommandInputChunk, socket: Socket) {
        element.stdin?.let { bytes ->
            socket.writeByte(MessageType.STDIN.toValue())
            socket.writeIntLittleEndian(bytes.size)
            socket.writeFully(bytes)
        }

        if (element.close) {
            socket.writeByte(MessageType.CLOSE_STDIN.toValue())
            socket.writeIntLittleEndian(0)
        }
    }

    override fun serialize() = createBaseRequest("shell,v2,raw:$cmd")
}

data class ShellCommandResultChunk(
    val stdout: ByteArray? = null,
    val stderr: ByteArray? = null,
    val exitCode: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellCommandResultChunk

        if (stdout != null) {
            if (other.stdout == null) return false
            if (!stdout.contentEquals(other.stdout)) return false
        } else if (other.stdout != null) return false
        if (stderr != null) {
            if (other.stderr == null) return false
            if (!stderr.contentEquals(other.stderr)) return false
        } else if (other.stderr != null) return false
        if (exitCode != other.exitCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stdout?.contentHashCode() ?: 0
        result = 31 * result + (stderr?.contentHashCode() ?: 0)
        result = 31 * result + (exitCode ?: 0)
        return result
    }
}

data class ShellCommandInputChunk(
    val stdin: ByteArray? = null,
    val close: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellCommandInputChunk

        if (stdin != null) {
            if (other.stdin == null) return false
            if (!stdin.contentEquals(other.stdin)) return false
        } else if (other.stdin != null) return false
        if (close != other.close) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stdin?.contentHashCode() ?: 0
        result = 31 * result + close.hashCode()
        return result
    }
}
