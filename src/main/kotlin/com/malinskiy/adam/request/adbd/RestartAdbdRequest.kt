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

package com.malinskiy.adam.request.adbd

import com.malinskiy.adam.request.SynchronousRequest
import com.malinskiy.adam.request.transform.StringResponseTransformer

class RestartAdbdRequest(private val mode: AdbdMode) : SynchronousRequest<String>() {
    private val transformer = StringResponseTransformer()

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) = transformer.process(bytes, offset, limit)

    override fun serialize() = createBaseRequest(mode.requestString)

    override fun transform() = transformer.transform()
}
