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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.stub.ServerReadChannel
import com.malinskiy.adam.server.stub.ServerWriteChannel
import io.ktor.utils.io.writeIntLittleEndian
import java.io.File


class Session(val input: ServerReadChannel, val output: ServerWriteChannel) {
    suspend fun expectCmd(expected: () -> String): OkayFailSubSession {
        val transportCmd = input.receiveCommand()
        assertThat(transportCmd).isEqualTo(expected())

        return OkayFailSubSession(session = this)
    }

    suspend fun expectShell(expected: () -> String): ShellV1SubSession {
        val transportCmd = input.receiveCommand()
        assertThat(transportCmd).isEqualTo("shell:${expected()}")
        return ShellV1SubSession(session = this)
    }

    suspend fun respondTransport(success: Boolean, message: String? = null) {
        output.respond(
            when (success) {
                true -> Const.Message.OKAY
                false -> Const.Message.FAIL
            }
        )

        if (!success && message != null) {
            output.respondStringV1(message)
        }
    }

    suspend fun respondShellV1(stdout: String) {
        output.respondShellV1(stdout)
    }

    suspend fun expectFramebuffer(): FramebufferSubSession {
        expectCmd { "framebuffer:" }
        return FramebufferSubSession(this)
    }

    suspend fun respondScreencaptureV2(replay: File) {
        //Extended version
        output.writeIntLittleEndian(1)

        val sample = replay.readBytes()
        output.writeFully(sample, 0, 48)
        assertThat(input.readByte()).isEqualTo(0.toByte())
        output.writeFully(sample, 48, sample.size - 48)
    }

    suspend fun respondScreencaptureV3(replay: File) {
        //Extended version
        output.writeIntLittleEndian(2)

        val sample = replay.readBytes()
        output.writeFully(sample, 0, 52)
        assertThat(input.readByte()).isEqualTo(0.toByte())
        output.writeFully(sample, 52, sample.size - 52)
    }

    suspend fun respondAdbServerVersion(x: Int) {
        output.respondStringV1(x.toString(16))
    }

    suspend fun respondPairDevice(message: String) {
        output.respondStringV1(message)
    }

    suspend fun respondReconnectOffline(message: String) {
        output.respondStringV1(message)
    }

    suspend fun respondReconnectSingleDevice(message: String) {
        output.respondStringRaw(message)
    }

    suspend fun respondRemountPartitions(message: () -> String) {
        output.respondStringRaw(message())
    }

    suspend fun respondSideloadChunkRequested(part: String) {
        output.respondStringRaw(part)
    }

    suspend fun respondPortForward(success: Boolean, port: Int? = null) {
        output.respond(
            when (success) {
                true -> Const.Message.OKAY
                false -> Const.Message.FAIL
            }
        )

        if (success && port != null) {
            output.respondStringV1(port.toString())
        }
    }

    suspend fun respondSetDmVerityChecking(message: String) {
        output.respondStringRaw(message)
    }

    suspend fun expectShellV2Stdin(expected: String) {
        val stdin = input.receiveShellV2Stdin()
        assertThat(stdin).isEqualTo(expected)
    }

    suspend fun expectShellV2StdinClose() {
        input.receiveShellV2StdinClose()
    }

    suspend fun respondShellV2Stdout(message: String) {
        output.respondShellV2Stdout(message)
    }

    suspend fun respondShellV2Exit(exitCode: Int) {
        output.respondShellV2Exit(exitCode)
    }

    suspend fun respondShellV2Stderr(stderr: String) {
        output.respondShellV2Stderr(stderr)
    }

    suspend fun respondShellV2WindowSizeChange() {
        output.respondShellV2WindowSizeChange()
    }

    suspend fun respondShellV2Invalid() {
        output.respondShellV2Invalid()
    }

    suspend fun expectStat(path: () -> String) {
        val receiveStat = input.receiveStat()
        assertThat(receiveStat).isEqualTo(path())
    }

    suspend fun respondStat(size: Int, mode: Int = 0, lastModified: Int = 0) {
        output.respondStat(size, mode, lastModified)
    }

    suspend fun respondStatV2(
        mode: Int,
        size: Int,
        error: Int,
        dev: Int,
        ino: Int,
        nlink: Int,
        uid: Int,
        gid: Int,
        atime: Int,
        mtime: Int,
        ctime: Int
    ) {
        output.respondStatV2(mode, size, error, dev, ino, nlink, uid, gid, atime, mtime, ctime)
    }

    suspend fun expectSend(message: () -> String): SendFileSubSession {
        val receiveCmd = input.receiveSend()
        assertThat(receiveCmd).isEqualTo(message())

        return SendFileSubSession(this)
    }

    suspend fun receiveFile(receiveFile: File) {
        input.receiveFile(receiveFile)
    }

    suspend fun expectSendV2(receiveCmd: String, mode: String, flags: Int): SendFileV2SubSession {
        val (actualReceiveCmd, actualMode, actualFlags) = input.receiveSendV2()
        assertThat(actualReceiveCmd).isEqualTo(receiveCmd)
        assertThat(actualMode.toString(8)).isEqualTo(mode)
        assertThat(actualFlags).isEqualTo(flags)

        return SendFileV2SubSession(this)
    }

    suspend fun expectRecv(path: () -> String): ReceiveFileSubSession {
        val recvPath = input.receiveRecv()
        assertThat(recvPath).isEqualTo(path())
        return ReceiveFileSubSession(this)
    }

    suspend fun respondFile(fixture: File) {
        output.respondData(fixture.readBytes())
    }

    suspend fun expectRecv2(path: () -> String): ReceiveFileSubSession {
        val recvPath = input.receiveRecv2()
        assertThat(recvPath).isEqualTo(path())

        return ReceiveFileSubSession(this)
    }

    suspend fun expectList(path: () -> String) {
        val listPath = input.receiveList()
        assertThat(listPath).isEqualTo(path())
    }

    suspend fun respondList(size: Int, mode: Int = 0, lastModified: Int = 0, name: String): DoneFailSubSession {
        output.respondList(size, mode, lastModified, name)
        return DoneFailSubSession(this)
    }

    suspend fun expectListV2(path: () -> String) {
        val listPath = input.receiveListV2()
        assertThat(listPath).isEqualTo(path())
    }

    suspend fun respondListV2(
        name: String,
        mode: Int = 0,
        size: Int,
        error: Int,
        dev: Int,
        ino: Int,
        nlink: Int,
        uid: Int,
        gid: Int,
        atime: Int,
        mtime: Int,
        ctime: Int
    ): DoneFailSubSession {
        output.respondListV2(name, mode, size, error, dev, ino, nlink, uid, gid, atime, mtime, ctime)
        return DoneFailSubSession(this)
    }

    suspend fun expectStatV2(path: () -> String) {
        val receiveStat = input.receiveStatV2()
        assertThat(receiveStat).isEqualTo(path())
    }

    suspend fun resondRestartAdbd(message: String) {
        output.respondStringRaw(message)
    }

    suspend fun respondAsyncDeviceMonitor(serial: String, state: String) {
        output.respondStringV1("$serial\t$state\n")
    }

    suspend fun respondListDevices(serialToState: Map<String, String>) {
        val response = serialToState.map { "${it.key}\t${it.value}\n" }.joinToString(separator = "")
        output.respondStringV1(response)
    }

    suspend fun respondListPortForwards(response: String) {
        output.respondStringV1(response)
    }


    suspend fun respondConnectDevice(response: String) {
        output.respondStringV1(response)
    }

    suspend fun respondDisconnectDevice(response: String) {
        output.respondStringV1(response)
    }

    suspend fun respondOkay() {
        output.respondOkay()
    }

    suspend fun expectLegacySideload(size: Int): LegacySideloadSubSession {
        expectCmd { "sideload:$size" }.accept()
        return LegacySideloadSubSession(this)
    }

    suspend fun receiveBytes(size: Int): ByteArray {
        return input.receiveBytes(size)
    }

    suspend fun expectAdbServerVersion(): GetAdbServerSubSession {
        expectCmd { "host:version" }
        return GetAdbServerSubSession(this)
    }
}
