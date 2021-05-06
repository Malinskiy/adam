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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.compatFlip
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import com.malinskiy.adam.transport.roket.Roket
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer


class TCPSocketTest {

    lateinit var selectorManager: SelectorManager

    @Before
    fun setup() {
        selectorManager = SelectorManager()
        selectorManager.start()
    }

    @After
    fun teardown() {
        selectorManager.close()
    }

    @Test
    fun testHappyPath() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val port = server.startAndListen { input, output ->
                val forwardCmd = input.receiveCommand()
                assertThat(forwardCmd).isEqualTo("host:devices")
                output.respond(Const.Message.OKAY)
                output.close()
            }.port

            val tcpSocket = TCPSocketBuilder(
                InetSocketAddress("localhost", port),
                selectorManager
            ).build()

            tcpSocket.connect()
            tcpSocket.write { channel ->
                val written = channel.write(ByteBuffer.wrap("000Chost:devices".toByteArray()))
                println("Written $written")
            }

            val result = ByteBuffer.allocate(1024)
            val response = tcpSocket.read { readableChannel ->
                result.compatLimit(4)
                readableChannel.read(result)
                result.compatFlip()
            }

            val bytes = ByteArray(4)
            response.get(bytes, 0, 4)
            assertThat(bytes).isEqualTo(Const.Message.OKAY)
        }
    }

    @Test(expected = SocketTimeoutException::class)
    fun testReadTimeoutException() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val port = server.startAndListen { input, output ->
                delay(2000)
            }.port

            val socket = TCPSocketBuilder(
                InetSocketAddress("localhost", port),
                selectorManager,
                idleTimeout = 1000
            ).build()
            socket.connect()

            val roket = Roket(socket).readByte()
        }
    }

    @Test(expected = SocketTimeoutException::class)
    fun testReadAvailableTimeoutException() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val port = server.startAndListen { input, output ->
                delay(2000)
            }.port

            val socket = TCPSocketBuilder(
                InetSocketAddress("localhost", port),
                selectorManager,
                idleTimeout = 1000
            ).build()
            socket.connect()

            val bytes = ByteArray(1)
            val read = Roket(socket).readAvailable(bytes, 0, 1)
            println(read)
        }
    }

    @Test(expected = SocketTimeoutException::class)
    fun testWriteTimeoutException() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val port = server.startAndListen { input, output ->
                delay(2000)
            }.port

            val socket = TCPSocketBuilder(
                InetSocketAddress("localhost", port),
                selectorManager,
                idleTimeout = 1000
            ).build()
            socket.connect()

            //16MiB is enough to fill all of send buffer
            val bytes = ByteArray(1024 * 1024 * 16)
            val write = Roket(socket).writeFully(bytes)
        }
    }
}
