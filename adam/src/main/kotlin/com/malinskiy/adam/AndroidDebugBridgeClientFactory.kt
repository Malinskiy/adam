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

package com.malinskiy.adam

import com.malinskiy.adam.interactor.DiscoverAdbSocketInteractor
import com.malinskiy.adam.transport.KtorSocketFactory
import com.malinskiy.adam.transport.SocketFactory
import kotlinx.coroutines.Dispatchers
import java.net.InetAddress
import java.time.Duration
import kotlin.coroutines.CoroutineContext

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