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

package com.malinskiy.adam.request.reverse

import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.SynchronousRequest

/**
 * Doesn't work with SerialTarget, have to use the serial as a parameter for the execute method
 */
class RemoveAllReversePortForwardsRequest : SynchronousRequest<Unit>(target = NonSpecifiedTarget) {
    override fun serialize() = createBaseRequest("reverse:killforward-all")

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) = Unit

    override fun transform() = Unit
}