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

import com.android.ddmlib.logging.Log
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.interactor.DiscoverAdbSocketInteractor
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.SetDeviceRequest
import com.malinskiy.adam.request.async.AsyncChannelRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.net.InetAddress
import java.net.InetSocketAddress

class AndroidDebugBridgeServer(
    val port: Int,
    val host: InetAddress
) {
    private val socketAddress: InetSocketAddress = InetSocketAddress(host, port)

    suspend fun <T : Any?> execute(request: ComplexRequest<T>, serial: String? = null): T {
        aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .connect(socketAddress).use { socket ->
                val readChannel = socket.openReadChannel().toAndroidChannel()
                val writeChannel = socket.openWriteChannel(autoFlush = true).toAndroidChannel()

                serial?.let {
                    processRequest(writeChannel, SetDeviceRequest(it).serialize(), readChannel)
                }

                processRequest(writeChannel, request.serialize(), readChannel)
                return request.process(readChannel, writeChannel)
            }
    }

    fun <T : Any?> execute(request: AsyncChannelRequest<T>, scope: CoroutineScope, serial: String? = null): ReceiveChannel<T> {
        return scope.produce {
            aSocket(ActorSelectorManager(Dispatchers.IO))
                .tcp()
                .connect(socketAddress).use { socket ->
                    val readChannel = socket.openReadChannel().toAndroidChannel()
                    val writeChannel = socket.openWriteChannel(autoFlush = true).toAndroidChannel()

                    serial?.let {
                        processRequest(writeChannel, SetDeviceRequest(it).serialize(), readChannel)
                    }

                    processRequest(writeChannel, request.serialize(), readChannel)

                    while (true) {
                        if (isClosedForSend || readChannel.isClosedForRead || writeChannel.isClosedForWrite) return@produce
                        val element = request.readElement(readChannel, writeChannel)
                        send(element)
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
            Log.w(TAG, "adb server rejected command ${String(request, Const.DEFAULT_TRANSPORT_ENCODING)}")
            throw RequestRejectedException(response.message ?: "no message received")
        }
    }

    companion object {
        val TAG: String = AndroidDebugBridgeServer::class.java.simpleName
    }
}

class AndroidDebugBridgeServerFactory {
    var port: Int? = null
    var host: InetAddress? = null

    fun build(): AndroidDebugBridgeServer {
        return AndroidDebugBridgeServer(
            port = port ?: DiscoverAdbSocketInteractor().execute(),
            host = host ?: InetAddress.getByName(Const.DEFAULT_ADB_HOST)
        )
    }
}