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

package com.malinskiy.adam.server.junit4

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import com.malinskiy.adam.server.stub.dsl.Session
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class AdbServerRule : TestRule {
    lateinit var server: AndroidDebugBridgeServer
    val client: AndroidDebugBridgeClient
        get() = server.client

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                server = AndroidDebugBridgeServer()
                server.start()

                try {
                    base.evaluate()
                } finally {
                    runBlocking {
                        withContext(NonCancellable) {
                            server.dispose()
                        }
                    }
                }
            }
        }
    }

    fun session(block: suspend Session.() -> Unit) {
        server.session(block)
    }
}

