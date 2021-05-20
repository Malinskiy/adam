/*
 * Copyright (C) 2020 Anton Malinskiy
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

package com.malinskiy.adam.request.framebuffer

import java.awt.Transparency
import java.awt.color.ICC_ColorSpace
import java.awt.color.ICC_Profile
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.DirectColorModel
import java.io.IOException

class ColorModelFactory {
    private val cache = mutableMapOf<String, ColorModel>()

    fun get(profileName: String, type: Int): ColorModel {
        cache[profileName]?.let { return it }

        synchronized(cache) {
            cache[profileName]?.let { return it }

            val profile = try {
                ICC_Profile.getInstance(javaClass.classLoader.getResourceAsStream("colorProfiles/$profileName"))
            } catch (e: IOException) { // Ignore
                ICC_Profile.getInstance(ICC_ColorSpace.CS_sRGB)
            }
            val colorSpace = ICC_ColorSpace(profile)
            val colorModel: ColorModel = when (type) {
                BufferedImage.TYPE_4BYTE_ABGR -> {
                    ComponentColorModel(
                        colorSpace,
                        true,
                        false,
                        Transparency.TRANSLUCENT,
                        DataBuffer.TYPE_BYTE
                    )
                }
                BufferedImage.TYPE_3BYTE_BGR -> {
                    ComponentColorModel(
                        colorSpace,
                        false,
                        false,
                        Transparency.TRANSLUCENT,
                        DataBuffer.TYPE_BYTE
                    )
                }
                BufferedImage.TYPE_INT_ARGB -> {
                    DirectColorModel(
                        colorSpace,
                        32,
                        0x00ff0000,
                        0x0000ff00,
                        0x000000ff,
                        -0x1000000,
                        false,
                        DataBuffer.TYPE_INT
                    )
                }
                else -> throw RuntimeException("Unsupported buffered image type $type")
            }
            return colorModel.also { cache[profileName] = it }
        }
    }


}
