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

package com.malinskiy.adam

import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.exception.RequestValidationException
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.interactor.DiscoverAdbSocketInteractor
import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.SetDeviceRequest
import com.malinskiy.adam.request.async.AsyncChannelRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import com.malinskiy.adam.transport.KtorSocketFactory
import com.malinskiy.adam.transport.SocketFactory
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class AndroidDebugBridgeClient(
    val port: Int,
    val host: InetAddress,
    val socketFactory: SocketFactory
) {
    private val socketAddress: InetSocketAddress = InetSocketAddress(host, port)

    suspend fun <T : Any?> execute(request: ComplexRequest<T>, serial: String? = null): T {
        if (!request.validate()) {
            throw RequestValidationException("Request ${request.javaClass.simpleName} did not pass validation")
        }
        socketFactory.tcp(socketAddress).use { socket ->
            val readChannel = socket.openReadChannel().toAndroidChannel()
            var writeChannel: AndroidWriteChannel? = null
            try {
                writeChannel = socket.openWriteChannel(autoFlush = true).toAndroidChannel()
                serial?.let {
                    processRequest(writeChannel, SetDeviceRequest(it).serialize(), readChannel)
                }
                return request.process(readChannel, writeChannel)
            } finally {
                try {
                    writeChannel?.close()
                    readChannel.cancel()
                } catch (e: Exception) {
                    log.debug(e) { "Exception during cleanup. Ignoring" }
                }
            }
        }
    }

    fun <T : Any?> execute(request: AsyncChannelRequest<T>, scope: CoroutineScope, serial: String? = null): ReceiveChannel<T> {
        if (!request.validate()) {
            throw RequestValidationException("Request ${request.javaClass.simpleName} did not pass validation")
        }
        return scope.produce {
            socketFactory.tcp(socketAddress).use { socket ->
                val readChannel = socket.openReadChannel().toAndroidChannel()
                var writeChannel: AndroidWriteChannel? = null

                try {
                    writeChannel = socket.openWriteChannel(autoFlush = true).toAndroidChannel()
                    serial?.let {
                        processRequest(writeChannel, SetDeviceRequest(it).serialize(), readChannel)
                    }

                    request.handshake(readChannel, writeChannel)

                    while (true) {
                        if (isClosedForSend || readChannel.isClosedForRead || writeChannel.isClosedForWrite) {
                            break
                        }
                        val element = request.readElement(readChannel, writeChannel)
                        send(element)
                    }
                } finally {
                    try {
                        request.close(channel)
                        writeChannel?.close()
                        readChannel.cancel()
                    } catch (e: Exception) {
                        log.debug(e) { "Exception during cleanup. Ignoring" }
                    }
                }
            }
        }
    }

    private suspend fun processRequest(
        writeChannel: AndroidWriteChannel,
        request: ByteArray,
        readChannel: AndroidReadChannel
    ) {
        writeChannel.write(request)
        val response = readChannel.read()
        if (!response.okay) {
            log.warn { "adb server rejected command ${String(request, Const.DEFAULT_TRANSPORT_ENCODING)}" }
            throw RequestRejectedException(response.message ?: "no message received")
        }
    }

    companion object {
        private val log = AdamLogging.logger {}
    }
}

class AndroidDebugBridgeClientFactory {
    var port: Int? = null
    var host: InetAddress? = null
    var coroutineContext: CoroutineContext? = null
    var socketFactory: SocketFactory? = null
    var socketTimeout: Duration? = null

    fun build(): AndroidDebugBridgeClient {
        return AndroidDebugBridgeClient(
            port = port ?: DiscoverAdbSocketInteractor().execute(),
            host = host ?: InetAddress.getByName(Const.DEFAULT_ADB_HOST),
            socketFactory = socketFactory ?: KtorSocketFactory(
                coroutineContext = coroutineContext ?: Dispatchers.IO,
                socketTimeout = socketTimeout?.toMillis() ?: 30_000
            )
        )
    }
}
