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

package com.malinskiy.adam.server.stub.dsl

import com.malinskiy.adam.Const
import java.io.File

class SendFileExpectationV2(private val session: Session) {
    suspend fun receiveFile(fixture: File): SendFileExpectationV2 {
        session.receiveFile(fixture)
        return this
    }

    suspend fun done() {
        session.respondOkay()
    }

    suspend fun fail(message: String) {
        session.output.respond(Const.Message.FAIL)
        session.output.respondStringV2(message)
    }
}
