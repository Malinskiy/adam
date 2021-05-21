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

package com.malinskiy.adam.server.junit5

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import com.malinskiy.adam.server.stub.dsl.Session
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties


class AdbServerExtension : BeforeEachCallback, AfterEachCallback {
    lateinit var server: AndroidDebugBridgeServer
    val client: AndroidDebugBridgeClient
        get() = server.client

    fun session(block: suspend Session.() -> Unit) {
        server.listen { input, output ->
            val session = Session(input, output)
            block(session)
        }
    }

    override fun beforeEach(context: ExtensionContext) {
        server = AndroidDebugBridgeServer()
        server.start()

        setupServerField(context)
        setupClientField(context)
    }

    private fun setupServerField(context: ExtensionContext) {
        val instance = context.testInstance.get()
        instance::class.memberProperties
            .filter { it.hasAnnotation<AdbServer>() && it.returnType.isSubtypeOf(AndroidDebugBridgeServer::class.createType()) }
            .filterIsInstance<KMutableProperty<*>>()
            .forEach { prop ->
                prop.setter.call(instance, server)
            }
    }

    private fun setupClientField(context: ExtensionContext) {
        val instance = context.testInstance.get()
        instance::class.memberProperties
            .filter { it.hasAnnotation<AdbClient>() && it.returnType.isSubtypeOf(AndroidDebugBridgeClient::class.createType()) }
            .filterIsInstance<KMutableProperty<*>>()
            .forEach { prop ->
                prop.setter.call(instance, client)
            }
    }

    override fun afterEach(context: ExtensionContext?) {
        runBlocking {
            withContext(NonCancellable) {
                server.dispose()
            }
        }
    }
}

