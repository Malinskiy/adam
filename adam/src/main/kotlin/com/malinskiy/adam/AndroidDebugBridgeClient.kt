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

import com.malinskiy.adam.exception.RequestValidationException
import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.emu.EmulatorCommandRequest
import com.malinskiy.adam.request.misc.SetDeviceRequest
import com.malinskiy.adam.transport.SocketFactory
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.InetAddress
import java.net.InetSocketAddress

class AndroidDebugBridgeClient(
    val port: Int,
    val host: InetAddress,
    val socketFactory: SocketFactory
) : Closeable {
    private val socketAddress: InetSocketAddress = InetSocketAddress(host, port)

    suspend fun <T : Any?> execute(request: ComplexRequest<T>, serial: String? = null): T {
        val validationResponse = request.validate()
        if (!validationResponse.success) {
            val requestSimpleClassName = request.javaClass.simpleName
            throw RequestValidationException("Request $requestSimpleClassName did not pass validation: ${validationResponse.message}")
        }

        return socketFactory.tcp(
            socketAddress = socketAddress,
            idleTimeout = request.socketIdleTimeout
        ).use { socket ->
            serial?.let {
                SetDeviceRequest(it).handshake(socket)
            }
            request.process(socket)
        }
    }

    fun <T : Any?, I : Any?> execute(request: AsyncChannelRequest<T, I>, scope: CoroutineScope, serial: String? = null): ReceiveChannel<T> {
        val validationResponse = request.validate()
        if (!validationResponse.success) {
            val requestSimpleClassName = request.javaClass.simpleName
            throw RequestValidationException("Request $requestSimpleClassName did not pass validation: ${validationResponse.message}")
        }
        return scope.produce {
            socketFactory.tcp(
                socketAddress = socketAddress,
                idleTimeout = request.socketIdleTimeout
            ).use { socket ->
                var backChannel = request.channel
                var backChannelJob: Job? = null

                try {
                    serial?.let {
                        SetDeviceRequest(it).handshake(socket)
                    }

                    request.handshake(socket)

                    backChannelJob = launch {
                        if (backChannel == null) return@launch
                        for (it in backChannel) {
                            if (!socket.isClosedForWrite) {
                                request.writeElement(it, socket)
                            }
                        }
                    }

                    while (true) {
                        if (isClosedForSend ||
                            socket.isClosedForRead ||
                            request.channel?.isClosedForReceive == true
                        ) {
                            break
                        }
                        val finished = request.readElement(socket, this)
                        if (finished) break
                    }
                } finally {
                    try {
                        withContext(NonCancellable) {
                            request.close(channel)
                            backChannelJob?.cancel()
                        }
                    } catch (e: Exception) {
                        log.debug(e) { "Exception during cleanup. Ignoring" }
                    }
                }
            }
        }
    }

    suspend fun execute(request: EmulatorCommandRequest): String {
        return socketFactory.tcp(
            socketAddress = request.address,
            idleTimeout = request.idleTimeoutOverride
        ).use { socket ->
            request.process(socket)
        }
    }

    suspend fun <T> execute(request: MultiRequest<T>, serial: String? = null): T {
        val validationResponse = request.validate()
        if (!validationResponse.success) {
            val requestSimpleClassName = request.javaClass.simpleName
            throw RequestValidationException("Request $requestSimpleClassName did not pass validation: ${validationResponse.message}")
        }

        return request.execute(this, serial)
    }

    companion object {
        private val log = AdamLogging.logger {}
    }

    /**
     * If you're reusing the socket factory across multiple clients then this will affect another client
     */
    override fun close() = socketFactory.close()
}
