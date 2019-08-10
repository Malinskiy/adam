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

package com.malinskiy.adam.request

import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel

/**
 * This type of request starts with single serialized request
 * and then proceed to do several reads and writes that have dynamic size
 */
abstract class ComplexRequest<T : Any?>(target: Target = NonSpecifiedTarget) : Request(target) {
    abstract suspend fun process(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): T
}