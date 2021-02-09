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

package com.malinskiy.adam.transport

import java.net.InetSocketAddress

class NioSocketFactory(
    private val connectTimeout: Long = 10_000,
    private val idleTimeout: Long = 30_000
) : SocketFactory {
    override suspend fun tcp(socketAddress: InetSocketAddress, connectTimeout: Long?, idleTimeout: Long?): Socket {
        val nioSocket = NioSocket(
            socketAddress = socketAddress,
            connectTimeout = connectTimeout ?: this.connectTimeout,
            idleTimeout = idleTimeout ?: this.idleTimeout,
        )
        nioSocket.connect()
        return nioSocket
    }

    override fun close() {}
}
