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

package com.malinskiy.adam.interactor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import org.junit.Test

class DiscoverAdbSocketInteractorTest {

    @Test
    fun testDefault() {
        val execute = DiscoverAdbSocketInteractor().execute()
        assertThat(execute).isEqualTo(Const.DEFAULT_ADB_PORT)
    }

    @Test
    fun testProperty() {
        System.setProperty(Const.SERVER_PORT_ENV_VAR, "1234")
        val execute = DiscoverAdbSocketInteractor().execute()
        System.setProperty(Const.SERVER_PORT_ENV_VAR, "")

        assertThat(execute).isEqualTo(1234)
    }
}