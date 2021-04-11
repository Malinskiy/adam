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

package com.malinskiy.adam.transport.vertx

import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.SocketFactory
import io.vertx.core.Vertx
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.coroutines.await
import java.net.InetSocketAddress

class VertxSocketFactory(
    private val connectTimeout: Long = 10_000,
    private val idleTimeout: Long = 30_000
) : SocketFactory {
    private val vertx by lazy { Vertx.vertx() }

    override suspend fun tcp(socketAddress: InetSocketAddress, connectTimeout: Long?, idleTimeout: Long?): Socket {
        val vertxSocket = VertxSocket(SocketAddress.inetSocketAddress(socketAddress), NetClientOptions().apply {
            setConnectTimeout((connectTimeout ?: this@VertxSocketFactory.connectTimeout).toTimeoutInt())
            setIdleTimeout((idleTimeout ?: this@VertxSocketFactory.idleTimeout).toTimeoutInt())
        })
        val id = vertx.deployVerticle(vertxSocket).await()
        vertxSocket.id = id
        return vertxSocket
    }
    
    private fun Long.toTimeoutInt(): Int {
        val toInt = toInt()
        return if (toInt < 0) Int.MAX_VALUE
        else toInt
    }
}
