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

package com.malinskiy.adam.request.misc

import com.malinskiy.adam.request.SynchronousRequest

class RebootRequest(private val mode: RebootMode = RebootMode.DEFAULT) : SynchronousRequest<Unit>() {
    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) = Unit

    override fun serialize() = createBaseRequest("reboot:${mode.value}")

    override fun transform() = Unit
}

enum class RebootMode(val value: String) {
    DEFAULT(""),
    RECOVERY("recovery"),
    BOOTLOADER("bootloader"),
    SIDELOAD("sideload"),
    SIDELOAD_AUTO_REBOOT("sideload-auto-reboot")
}
