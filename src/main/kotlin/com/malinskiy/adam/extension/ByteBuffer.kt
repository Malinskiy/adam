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

package com.malinskiy.adam.extension

import java.nio.Buffer
import java.nio.ByteBuffer

/**
 * Mitigation of running JDK 9 code on JRE 8
 *
 * java.lang.NoSuchMethodError: java.nio.ByteBuffer.xxx()Ljava/nio/ByteBuffer;
 */
fun ByteBuffer.compatRewind() = ((this as Buffer).rewind() as ByteBuffer)
fun ByteBuffer.compatLimit(newLimit: Int) = ((this as Buffer).limit(newLimit) as ByteBuffer)
fun ByteBuffer.compatPosition(newLimit: Int) = ((this as Buffer).position(newLimit) as ByteBuffer)
