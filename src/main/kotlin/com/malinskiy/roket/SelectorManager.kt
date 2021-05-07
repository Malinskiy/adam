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

package com.malinskiy.roket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.channels.WritableByteChannel
import java.nio.channels.spi.SelectorProvider
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class SelectorManager : CoroutineScope {
    override val coroutineContext: CoroutineContext = newSingleThreadContext("RoketSelector")

    private val provider = SelectorProvider.provider()
    private var selectionJob: Job? = null
    private val queue: Queue<Message> = ConcurrentLinkedQueue()
    private lateinit var selector: Selector

    fun send(message: Message) {
        queue.add(message)
        selector.wakeup()
    }

    fun start() {
        selector = provider.openSelector()
        selectionJob = launch {
            while (isActive) {
                val selected = selector.select()
                if (selected > 0 || selector.selectedKeys().size > 0) {
                    processSelected()
                }
                processRequests()
            }
        }
    }

    private fun processRequests() {
        do {
            val request = queue.poll() ?: break
            when (request) {
                is ConnectMessage -> {
                    val socketChannel = request.socketChannel
                    registerChannel(socketChannel, SelectionKey.OP_CONNECT)
                    registerContinuation(socketChannel, connect = request.continuation)
                }
                is DisconnectMessage -> {
                    val socketChannel = request.socketChannel
                    socketChannel.keyFor(selector)?.cancel()
                    socketChannel.close()
                    request.continuation.resume(Unit)
                }
                is ReadMessage -> {
                    val socketChannel = request.socketChannel
                    registerChannel(socketChannel, SelectionKey.OP_READ)
                    registerContinuation(socketChannel, read = request.continuation)
                }
                is WriteMessage -> {
                    val socketChannel = request.socketChannel
                    registerChannel(socketChannel, SelectionKey.OP_WRITE)
                    registerContinuation(socketChannel, write = request.continuation)
                }
            }
        } while (true)
    }

    private fun registerContinuation(
        socketChannel: SocketChannel,
        connect: Continuation<Unit>? = null,
        disconnect: Continuation<Unit>? = null,
        read: Continuation<ReadableByteChannel>? = null,
        write: Continuation<WritableByteChannel>? = null,
    ): Continuations {
        val selectionKey = socketChannel.keyFor(selector)
        val continuations = when {
            selectionKey.attachment() == null -> {
                Continuations().apply {
                    selectionKey.attach(this)
                }
            }
            else -> selectionKey.attachment() as Continuations
        }

        disconnect?.let { continuations.disconnect = it }
        connect?.let { continuations.connect = it }
        read?.let { continuations.read = it }
        write?.let { continuations.write = it }

        return continuations
    }

    private fun registerChannel(socketChannel: SocketChannel, interest: Int) {
        if (socketChannel.isRegistered) {
            socketChannel.keyFor(selector).addInterest(interest)
        } else {
            socketChannel.register(selector, interest)
        }
    }

    private fun processSelected() {
        val selectedKeys = selector.selectedKeys()
        for (key in selectedKeys) {

            val continuations = key.attachment() as Continuations
            val connectContinuation = continuations.connect
            val readContinuation = continuations.read
            val writeContinuation = continuations.write
            if (key.isValid && key.isConnectable && connectContinuation != null) {
                key.removeInterest(SelectionKey.OP_CONNECT)
                continuations.connect = null
                connectContinuation.resume(Unit)
            }
            if (key.isValid && key.isReadable && readContinuation != null) {
                key.removeInterest(SelectionKey.OP_READ)
                continuations.read = null
                readContinuation.resume(key.channel() as ReadableByteChannel)
            }
            if (key.isValid && key.isWritable && writeContinuation != null) {
                key.removeInterest(SelectionKey.OP_WRITE)
                continuations.write = null
                writeContinuation.resume(key.channel() as WritableByteChannel)
            }
        }
    }

    fun close() {
        selectionJob?.cancel()
    }
}

sealed class Message
data class ConnectMessage(
    val socketChannel: SocketChannel,
    val continuation: Continuation<Unit>
) : Message()

data class ReadMessage(
    val socketChannel: SocketChannel,
    val continuation: Continuation<ReadableByteChannel>
) : Message()

data class WriteMessage(
    val socketChannel: SocketChannel,
    val continuation: Continuation<WritableByteChannel>
) : Message()

data class DisconnectMessage(
    val socketChannel: SocketChannel,
    val continuation: Continuation<Unit>
) : Message()

private fun SelectionKey.addInterest(interest: Int) = apply {
    interestOps(interestOps() or interest)
}

private fun SelectionKey.removeInterest(interest: Int) = apply {
    interestOps(interestOps() and interest.inv())
}
