/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmlib;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Handle thread status updates.
 */
final class HandleNativeHeap extends ChunkHandler {

    public static final int CHUNK_NHGT = type("NHGT"); //$NON-NLS-1$
    public static final int CHUNK_NHSG = type("NHSG"); //$NON-NLS-1$
    public static final int CHUNK_NHST = type("NHST"); //$NON-NLS-1$
    public static final int CHUNK_NHEN = type("NHEN"); //$NON-NLS-1$

    private static final HandleNativeHeap mInst = new HandleNativeHeap();

    /**
     * Handle getting different sized size_t and pointer reads.
     */
    abstract class NativeBuffer {
        public NativeBuffer(ByteBuffer buffer) {
            mBuffer = buffer;
        }

        public abstract int getSizeT();
        public abstract long getPtr();

        protected ByteBuffer mBuffer;
    }

    /**
     * This class treats size_t and pointer values as 32 bit.
     */
    final class NativeBuffer32 extends NativeBuffer {
        public NativeBuffer32(ByteBuffer buffer) {
          super(buffer);
        }

        @Override
        public int getSizeT() {
            return mBuffer.getInt();
        }
        @Override
        public long getPtr() {
            return (long)mBuffer.getInt() & 0x00000000ffffffffL;
        }
    }

    /**
     * This class treats size_t and pointer values as 64 bit.
     */
    final class NativeBuffer64 extends NativeBuffer {
        public NativeBuffer64(ByteBuffer buffer) {
          super(buffer);
        }

        @Override
        public int getSizeT() {
            return (int)mBuffer.getLong();
        }
        @Override
        public long getPtr() {
            return mBuffer.getLong();
        }
    }

    private HandleNativeHeap() {
    }


    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_NHGT, mInst);
        mt.registerChunkHandler(CHUNK_NHSG, mInst);
        mt.registerChunkHandler(CHUNK_NHST, mInst);
        mt.registerChunkHandler(CHUNK_NHEN, mInst);
    }

    /**
     * Client is ready.
     */
    @Override
    public void clientReady(Client client) throws IOException {}

    /**
     * Client went away.
     */
    @Override
    public void clientDisconnected(Client client) {}

    /**
     * Chunk handler entry point.
     */
    @Override
    public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {

        Log.d("ddm-nativeheap", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_NHGT) {
            handleNHGT(client, data);
        } else if (type == CHUNK_NHST) {
            // start chunk before any NHSG chunk(s)
            client.getClientData().getNativeHeapData().clearHeapData();
        } else if (type == CHUNK_NHEN) {
            // end chunk after NHSG chunk(s)
            client.getClientData().getNativeHeapData().sealHeapData();
        } else if (type == CHUNK_NHSG) {
            handleNHSG(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }

        client.update(Client.CHANGE_NATIVE_HEAP_DATA);
    }

    /**
     * Send an NHGT (Native Thread GeT) request to the client.
     */
    public static void sendNHGT(Client client) throws IOException {

        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data in request message

        finishChunkPacket(packet, CHUNK_NHGT, buf.position());
        Log.d("ddm-nativeheap", "Sending " + name(CHUNK_NHGT));
        client.sendAndConsume(packet, mInst);

        rawBuf = allocBuffer(2);
        packet = new JdwpPacket(rawBuf);
        buf = getChunkDataBuf(rawBuf);

        buf.put((byte)HandleHeap.WHEN_DISABLE);
        buf.put((byte)HandleHeap.WHAT_OBJ);

        finishChunkPacket(packet, CHUNK_NHSG, buf.position());
        Log.d("ddm-nativeheap", "Sending " + name(CHUNK_NHSG));
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle our native heap data.
     */
    private void handleNHGT(Client client, ByteBuffer data) {
        ClientData clientData = client.getClientData();

        Log.d("ddm-nativeheap", "NHGT: " + data.limit() + " bytes");

        data.order(ByteOrder.LITTLE_ENDIAN);

        // There are two supported header formats.
        //
        // The original version of the header for 32 bit processes:
        //
        //   uint32_t mapSize;
        //   uint32_t mapSize;
        //   uint32_t allocSize;
        //   uint32_t allocInfoSize;
        //   uint32_t totalMemory;
        //   uint32_t backtrace_size;
        //
        // The new header which includes a signature and pointer size:
        //
        //   uint32_t signature;   (Which is always 0x812345dd)
        //   uint16_t version;     (Only version 2 of the new format supported)
        //   uint16_t pointerSize; (Size in bytes of size_t/pointer values)
        //   size_t mapSize;
        //   size_t allocSize;
        //   size_t allocInfoSize;
        //   size_t totalMemory;
        //   size_t backtrace_size;
        //
        // If the signature doesn't match, then the code uses the original
        // header format. If the signature matches, then use the new
        // header format with variable sizes of size_t and pointers.
        int signature = data.getInt(0);
        short pointerSize = 4;
        if (signature == 0x812345dd) {
            // Consume signature value.
            int ignore = data.getInt();
            short version = data.getShort();
            if (version != 2) {
                Log.e("ddms", "Unknown header version: " + version);
                return;
            }
            pointerSize = data.getShort();
        }
        NativeBuffer buffer;
        if (pointerSize == 4) {
            buffer = new NativeBuffer32(data);
        } else if (pointerSize == 8) {
            buffer = new NativeBuffer64(data);
        } else {
            Log.e("ddms", "Unknown pointer size: " + pointerSize);
            return;
        }

        // clear the previous run
        clientData.clearNativeAllocationInfo();

        int mapSize = buffer.getSizeT();
        int allocSize = buffer.getSizeT();
        int allocInfoSize = buffer.getSizeT();
        int totalMemory = buffer.getSizeT();
        int backtraceSize = buffer.getSizeT();

        Log.d("ddms", "mapSize: " + mapSize);
        Log.d("ddms", "allocSize: " + allocSize);
        Log.d("ddms", "allocInfoSize: " + allocInfoSize);
        Log.d("ddms", "totalMemory: " + totalMemory);

        clientData.setTotalNativeMemory(totalMemory);

        // this means that updates aren't turned on.
        if (allocInfoSize == 0) {
          return;
        }

        if (mapSize > 0) {
            byte[] maps = new byte[mapSize];
            data.get(maps, 0, mapSize);
            parseMaps(clientData, maps);
        }

        int iterations = allocSize / allocInfoSize;
        for (int i = 0 ; i < iterations ; i++) {
            NativeAllocationInfo info = new NativeAllocationInfo(
                    buffer.getSizeT() /* size */,
                    buffer.getSizeT() /* allocations */);

            for (int j = 0 ; j < backtraceSize ; j++) {
                long addr = buffer.getPtr();
                if (addr == 0x0) {
                    // skip past null addresses
                    continue;
                }

                info.addStackCallAddress(addr);
            }
            clientData.addNativeAllocation(info);
        }
    }

    private void handleNHSG(Client client, ByteBuffer data) {
        byte dataCopy[] = new byte[data.limit()];
        data.rewind();
        data.get(dataCopy);
        data = ByteBuffer.wrap(dataCopy);
        client.getClientData().getNativeHeapData().addHeapData(data);

        if (true) {
            return;
        }

        byte[] copy = new byte[data.limit()];
        data.get(copy);

        ByteBuffer buffer = ByteBuffer.wrap(copy);
        buffer.order(ByteOrder.BIG_ENDIAN);

        int id = buffer.getInt();
        int unitsize = buffer.get();
        long startAddress = buffer.getInt() & 0x00000000ffffffffL;
        int offset = buffer.getInt();
        int allocationUnitCount = buffer.getInt();

        // read the usage
        while (buffer.position() < buffer.limit()) {
            int eState = buffer.get() & 0x000000ff;
            int eLen = (buffer.get() & 0x000000ff) + 1;
        }
    }

    private void parseMaps(ClientData clientData, byte[] maps) {
        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(maps));
        BufferedReader reader = new BufferedReader(input);

        String line;

        try {
            while ((line = reader.readLine()) != null) {
                Log.d("ddms", "line: " + line);
                // Expected format:
                //   7fe51f2000-7fe5213000 rw-p 00000000 00:00 0      [stack]

                int library_start = line.lastIndexOf(' ');
                if (library_start == -1) {
                    continue;
                }

                // Assume that any string that starts with a / is a
                // shared library or executable that we will try to symbolize.
                String library = line.substring(library_start+1);
                if (!library.startsWith("/")) {
                    continue;
                }

                // Parse the start and end address range.
                int dashIndex = line.indexOf('-');
                int spaceIndex = line.indexOf(' ', dashIndex);
                if (dashIndex == -1 || spaceIndex == -1) {
                    continue;
                }

                long startAddr = 0;
                long endAddr = 0;
                try {
                    startAddr = Long.parseLong(line.substring(0, dashIndex), 16);
                    endAddr = Long.parseLong(line.substring(dashIndex+1, spaceIndex), 16);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    continue;
                }

                clientData.addNativeLibraryMapInfo(startAddr, endAddr, library);
                Log.d("ddms", library + "(" + Long.toHexString(startAddr) +
                      " - " + Long.toHexString(endAddr) + ")");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

