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

package com.malinskiy.adam.interactor

import com.android.ddmlib.logging.Log
import com.malinskiy.adam.Const

class DiscoverAdbSocketInteractor {
    private val TAG = DiscoverAdbSocketInteractor::class.java.simpleName

    fun execute() = discover("system property") { System.getProperty(Const.SERVER_PORT_ENV_VAR) }
        ?: discover("env var") { System.getenv(Const.SERVER_PORT_ENV_VAR) }
        ?: Const.DEFAULT_ADB_PORT

    private fun discover(discoveryType: String, discoveryBlock: () -> String?): Int? {
        try {
            val env: String? = discoveryBlock.invoke()
            env?.let {
                val port = Integer.decode(it)
                if (port.valid()) return port
            }
        } catch (ex: SecurityException) {
            Log.w(
                TAG,
                "No access to $discoveryType allowed by security manager. If you've set ANDROID_ADB_SERVER_PORT value, it's being ignored."
            )
        } catch (e: IllegalArgumentException) {
            Log.w(
                TAG,
                "Invalid value for ANDROID_ADB_SERVER_PORT ${e.message}."
            )
        }

        return null
    }

    private fun Int.valid() = when {
        this <= 0 || this >= 65535 -> false
        else -> true
    }
}