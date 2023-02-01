/*
 * Copyright (C) 2023 Anton Malinskiy
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

package com.malinskiy.adam.request.shell

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.request.shell.v1.ChanneledShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandInputChunk
import com.malinskiy.adam.request.shell.v2.ShellCommandResultChunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import com.malinskiy.adam.request.shell.v2.ChanneledShellCommandRequest as V2ChanneledShellCommandRequest

abstract class AsyncCompatShellCommandRequest<T : Any>(
    val cmd: String,
    private val supportedFeatures: List<Feature>,
    private val target: Target = NonSpecifiedTarget,
    private val coroutineScope: CoroutineScope,
    private val socketIdleTimeout: Long? = null,
) : MultiRequest<ReceiveChannel<T>>() {

    abstract suspend fun convertChunk(response: ShellCommandResultChunk): T?

    override suspend fun execute(
        androidDebugBridgeClient: AndroidDebugBridgeClient,
        serial: String?
    ): ReceiveChannel<T> {
        return when {
            supportedFeatures.contains(Feature.SHELL_V2) -> {
                val channel = Channel<ShellCommandInputChunk>()
                val receiveChannel = androidDebugBridgeClient.execute(
                    V2ChanneledShellCommandRequest(cmd, channel, target, socketIdleTimeout), coroutineScope, serial,
                )
                coroutineScope.produce {
                    for (chunk in receiveChannel) {
                        convertChunk(chunk)?.let { send(it) }
                    }
                    this@AsyncCompatShellCommandRequest.close(this.channel)
                }
            }

            else -> {
                val receiveChannel: ReceiveChannel<String> = androidDebugBridgeClient.execute(
                    ChanneledShellCommandRequest(cmd, target, socketIdleTimeout), coroutineScope, serial,
                )
                coroutineScope.produce {
                    for (line in receiveChannel) {
                        val chunk = ShellCommandResultChunk(stdout = line.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
                        convertChunk(chunk)?.let { send(it) }
                    }
                    this@AsyncCompatShellCommandRequest.close(this.channel)
                }
            }
        }
    }

    abstract suspend fun close(channel: SendChannel<T>)
}
