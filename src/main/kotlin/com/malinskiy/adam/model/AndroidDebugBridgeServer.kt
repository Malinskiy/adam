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

package com.malinskiy.adam.model

import com.android.ddmlib.logging.Log
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.interactor.DiscoverAdbSocketInteractor
import com.malinskiy.adam.model.cmd.Request
import com.malinskiy.adam.model.cmd.ResponseTransformer
import com.malinskiy.adam.model.cmd.SetDeviceRequest
import com.malinskiy.adam.model.cmd.SynchronousRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.io.ByteWriteChannel
import java.net.InetAddress
import java.net.InetSocketAddress

class AndroidDebugBridgeServer(
    val port: Int,
    val host: InetAddress
) {
    private val socketAddress: InetSocketAddress = InetSocketAddress(host, port)

    suspend fun <T: Any?> execute(serial: String, request: SynchronousRequest<T>): T = execute(serial, request, request)

    suspend fun execute(serial: String, request: Request, response: ByteWriteChannel) {
        aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .connect(socketAddress).use { socket ->
                val readChannel = socket.openReadChannel().toAndroidChannel()
                val writeChannel = socket.openWriteChannel(autoFlush = true).toAndroidChannel()

                processRequest(writeChannel, SetDeviceRequest(serial), readChannel)
                processRequest(writeChannel, request, readChannel)
                processResponse(response, readChannel)
            }
    }

    suspend fun <T : Any?> execute(serial: String, request: Request, responseTransformer: ResponseTransformer<T>): T {
        aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .connect(socketAddress).use { socket ->
                val readChannel = socket.openReadChannel().toAndroidChannel()
                val writeChannel = socket.openWriteChannel(autoFlush = true).toAndroidChannel()

                processRequest(writeChannel, SetDeviceRequest(serial), readChannel)
                processRequest(writeChannel, request, readChannel)
                val response = processResponse(readChannel)

                return responseTransformer.transform(response)
            }
    }

    private suspend fun processResponse(
        response: ByteWriteChannel,
        readChannel: AndroidReadChannel
    ) {
        val data = ByteArray(16384)
        loop@ do {
            if (response.isClosedForWrite || readChannel.isClosedForRead) break@loop
            val count = readChannel.readAvailable(data, 0, 16384)
            when {
                count == 0 -> {
                    continue@loop
                }
                count > 0 -> {
                    response.writeFully(data, 0, count)
                }
            }
        } while (count >= 0)
    }

    /**
     * Since this method will allocate as much memory as needed
     * one should not use it for streaming requests such as logcat streaming
     * as to not run out of memory
     */
    private suspend fun processResponse(readChannel: AndroidReadChannel): String {
        val builder = StringBuilder()
        val data = ByteArray(16384)
        loop@ do {
            val count = readChannel.readAvailable(data, 0, 16384)
            when {
                count == 0 -> {
                    continue@loop
                }
                count > 0 -> {
                    builder.append(String(data, 0, count, Const.DEFAULT_TRANSPORT_ENCODING))
                }
            }
        } while (count >= 0)

        return builder.toString()
    }

    private suspend fun processRequest(
        writeChannel: AndroidWriteChannel,
        request: Request,
        readChannel: AndroidReadChannel
    ) {
        writeChannel.write(request)
        val response = readChannel.read()
        if (!response.okay) {
            Log.w(TAG, "adb server rejected command ${String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING)}")
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