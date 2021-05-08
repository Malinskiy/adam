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

package com.malinskiy.adam.transport.vertx

import io.netty.buffer.Unpooled
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.Arguments
import io.vertx.core.streams.ReadStream

class VariableSizeRecordParser(
    private val stream: ReadStream<Buffer>? = null
) : Handler<Buffer>, ReadStream<Buffer> {
    // Empty and unmodifiable
    private val EMPTY_BUFFER = Buffer.buffer(Unpooled.EMPTY_BUFFER)
    private var buff = EMPTY_BUFFER
    private val bufferLock = Object()
    private var pos = 0 // Current position in buffer
    private var start = 0 // Position of beginning of current record
    private var requested = 0
    private var maxRecordSize = 0
    private var demand = Long.MAX_VALUE
    private var eventHandler: Handler<Buffer>? = null
    private var endHandler: Handler<Void?>? = null
    private var exceptionHandler: Handler<Throwable>? = null
    private var streamEnded = false
    private val drained
        get() = streamEnded && (buff.byteBuf.writerIndex() == 0)

    /**
     * This method is called to provide the parser with data.toChannel
     *
     * @param buffer  a chunk of data
     */
    override fun handle(buffer: Buffer) {
        synchronized(bufferLock) {
            if (buff.length() == 0) {
                buff = buffer
            } else {
                buff.appendBuffer(buffer)
            }
        }
        handleParsing()
        if (maxRecordSize > 0 && buff.length() > maxRecordSize) {
            val ex = IllegalStateException("The current record is too long")
            exceptionHandler?.handle(ex) ?: throw ex
        }
    }

    fun request(size: Int) {
        Arguments.require(size > 0, "Size must be > 0")
        requested = size
        handleParsing()
    }

    private fun handleParsing() {
        synchronized(bufferLock) {
            do {
                if (demand > 0L) {
                    var next: Int = parseFixed()
                    if (next == -1) {
                        next = if (streamEnded) {
                            break
                        } else {
                            stream?.resume()
                            if (streamEnded) {
                                continue
                            }
                            break
                        }
                    }
                    requested = 0
                    if (demand != Long.MAX_VALUE) {
                        demand--
                    }
                    val event = buff.getBuffer(start, next)
                    start = pos
                    val handler = eventHandler
                    handler?.handle(event)
                    if (streamEnded) {
                        break
                    }
                } else {
                    // Should use a threshold ?
                    stream?.pause()
                    break
                }
            } while (true)
            val length = buff.length()
            if (start == length) {
                buff = EMPTY_BUFFER
            } else if (start > 0) {
                buff = buff.getBuffer(start, length)
            }
            pos -= start
            start = 0
            if (drained) {
                end()
            }
        }
    }

    private fun parseFixed(): Int {
        val length = buff.length()
        if (length - start >= requested && requested > 0) {
            val end = start + requested
            pos = end
            return end
        } else if (length - start >= 0 && streamEnded && length - start < requested) {
            val end = start + length
            pos = end
            return end
        }
        return -1
    }

    private fun end() {
        endHandler?.handle(null)
    }

    override fun exceptionHandler(handler: Handler<Throwable>?) = apply { exceptionHandler = handler }

    override fun handler(handler: Handler<Buffer>?): ReadStream<Buffer> {
        eventHandler = handler
        if (stream != null) {
            if (handler != null) {
                stream.endHandler(Handler { v: Void? ->
                    streamEnded = true
                    handleParsing()
                })
                stream.exceptionHandler(Handler { err: Throwable ->
                    exceptionHandler?.handle(err)
                })
                stream.handler(this)
            } else {
                stream.handler(null)
                stream.endHandler(null)
                stream.exceptionHandler(null)
            }
        }
        return this
    }

    override fun pause() = apply { demand = 0L }

    override fun fetch(amount: Long): ReadStream<Buffer> {
        Arguments.require(amount > 0, "Fetch amount must be > 0")
        demand += amount
        if (demand < 0L) {
            demand = Long.MAX_VALUE
        }
        handleParsing()
        return this
    }

    override fun resume() = apply { fetch(Long.MAX_VALUE) }

    override fun endHandler(handler: Handler<Void?>?) = apply { endHandler = handler }
}
