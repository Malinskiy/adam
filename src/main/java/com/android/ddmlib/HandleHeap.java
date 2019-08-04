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

import com.android.ddmlib.ClientData.AllocationTrackingStatus;
import com.android.ddmlib.ClientData.IHprofDumpHandler;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Handle heap status updates.
 */
final class HandleHeap extends ChunkHandler {

    public static final int CHUNK_HPIF = type("HPIF");
    public static final int CHUNK_HPST = type("HPST");
    public static final int CHUNK_HPEN = type("HPEN");
    public static final int CHUNK_HPSG = type("HPSG");
    public static final int CHUNK_HPGC = type("HPGC");
    public static final int CHUNK_HPDU = type("HPDU");
    public static final int CHUNK_HPDS = type("HPDS");
    public static final int CHUNK_REAE = type("REAE");
    public static final int CHUNK_REAQ = type("REAQ");
    public static final int CHUNK_REAL = type("REAL");

    // args to sendHPSG
    public static final int WHEN_DISABLE = 0;
    public static final int WHEN_GC = 1;
    public static final int WHAT_MERGE = 0; // merge adjacent objects
    public static final int WHAT_OBJ = 1;   // keep objects distinct

    // args to sendHPIF
    public static final int HPIF_WHEN_NEVER = 0;
    public static final int HPIF_WHEN_NOW = 1;
    public static final int HPIF_WHEN_NEXT_GC = 2;
    public static final int HPIF_WHEN_EVERY_GC = 3;

    private static final HandleHeap mInst = new HandleHeap();

    private HandleHeap() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_HPIF, mInst);
        mt.registerChunkHandler(CHUNK_HPST, mInst);
        mt.registerChunkHandler(CHUNK_HPEN, mInst);
        mt.registerChunkHandler(CHUNK_HPSG, mInst);
        mt.registerChunkHandler(CHUNK_HPDS, mInst);
        mt.registerChunkHandler(CHUNK_REAQ, mInst);
        mt.registerChunkHandler(CHUNK_REAL, mInst);
    }

    /**
     * Client is ready.
     */
    @Override
    public void clientReady(Client client) throws IOException {
        client.initializeHeapUpdateStatus();
    }

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
        Log.d("ddm-heap", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_HPIF) {
            handleHPIF(client, data);
        } else if (type == CHUNK_HPST) {
            handleHPST(client, data);
        } else if (type == CHUNK_HPEN) {
            handleHPEN(client, data);
        } else if (type == CHUNK_HPSG) {
            handleHPSG(client, data);
        } else if (type == CHUNK_HPDU) {
            handleHPDU(client, data);
        } else if (type == CHUNK_HPDS) {
            handleHPDS(client, data);
        } else if (type == CHUNK_REAQ) {
            handleREAQ(client, data);
        } else if (type == CHUNK_REAL) {
            handleREAL(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /*
     * Handle a heap info message.
     */
    private void handleHPIF(Client client, ByteBuffer data) {
        Log.d("ddm-heap", "HPIF!");
        try {
            int numHeaps = data.getInt();

            for (int i = 0; i < numHeaps; i++) {
                int heapId = data.getInt();
                long timeStamp = data.getLong();
                byte reason = data.get();
                long maxHeapSize = (long)data.getInt() & 0x00ffffffff;
                long heapSize = (long)data.getInt() & 0x00ffffffff;
                long bytesAllocated = (long)data.getInt() & 0x00ffffffff;
                long objectsAllocated = (long)data.getInt() & 0x00ffffffff;

                client.getClientData().setHeapInfo(heapId, maxHeapSize,
                        heapSize, bytesAllocated, objectsAllocated, timeStamp, reason);
                client.update(Client.CHANGE_HEAP_DATA);
            }
        } catch (BufferUnderflowException ex) {
            Log.w("ddm-heap", "malformed HPIF chunk from client");
        }
    }

    /**
     * Send an HPIF (HeaP InFo) request to the client.
     */
    public static void sendHPIF(Client client, int when) throws IOException {
        ByteBuffer rawBuf = allocBuffer(1);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.put((byte)when);

        finishChunkPacket(packet, CHUNK_HPIF, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPIF) + ": when=" + when);
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle a heap segment series start message.
     */
    private void handleHPST(Client client, ByteBuffer data) {
        /* Clear out any data that's sitting around to
         * get ready for the chunks that are about to come.
         */
//xxx todo: only clear data that belongs to the heap mentioned in <data>.
        client.getClientData().getVmHeapData().clearHeapData();
    }

    /*
     * Handle a heap segment series end message.
     */
    private void handleHPEN(Client client, ByteBuffer data) {
        /* Let the UI know that we've received all of the
         * data for this heap.
         */
//xxx todo: only seal data that belongs to the heap mentioned in <data>.
        client.getClientData().getVmHeapData().sealHeapData();
        client.update(Client.CHANGE_HEAP_DATA);
    }

    /*
     * Handle a heap segment message.
     */
    private void handleHPSG(Client client, ByteBuffer data) {
        byte dataCopy[] = new byte[data.limit()];
        data.rewind();
        data.get(dataCopy);
        data = ByteBuffer.wrap(dataCopy);
        client.getClientData().getVmHeapData().addHeapData(data);
//xxx todo: add to the heap mentioned in <data>
    }

    /**
     * Sends an HPSG (HeaP SeGment) request to the client.
     */
    public static void sendHPSG(Client client, int when, int what)
        throws IOException {

        ByteBuffer rawBuf = allocBuffer(2);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.put((byte)when);
        buf.put((byte)what);

        finishChunkPacket(packet, CHUNK_HPSG, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPSG) + ": when="
            + when + ", what=" + what);
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends an HPGC request to the client.
     */
    public static void sendHPGC(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_HPGC, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPGC));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends an HPDU request to the client.
     *
     * We will get an HPDU response when the heap dump has completed.  On
     * failure we get a generic failure response.
     *
     * @param fileName name of output file (on device)
     */
    public static void sendHPDU(Client client, String fileName)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(4 + fileName.length() * 2);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(fileName.length());
        ByteBufferUtil.putString(buf, fileName);

        finishChunkPacket(packet, CHUNK_HPDU, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPDU) + " '" + fileName +"'");
        client.sendAndConsume(packet, mInst);
        client.getClientData().setPendingHprofDump(fileName);
    }

    /**
     * Sends an HPDS request to the client.
     *
     * We will get an HPDS response when the heap dump has completed.  On
     * failure we get a generic failure response.
     *
     * This is more expensive for the device than HPDU, because the entire
     * heap dump is held in RAM instead of spooled out to a temp file.  On
     * the other hand, permission to write to /sdcard is not required.
     *
     * @param fileName name of output file (on device)
     */
    public static void sendHPDS(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        finishChunkPacket(packet, CHUNK_HPDS, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPDS));
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle notification of completion of a HeaP DUmp.
     */
    private void handleHPDU(Client client, ByteBuffer data) {
        byte result;

        // get the filename and make the client not have pending HPROF dump anymore.
        String filename = client.getClientData().getPendingHprofDump();
        client.getClientData().setPendingHprofDump(null);

        // get the dump result
        result = data.get();

        // get the app-level handler for HPROF dump
        IHprofDumpHandler handler = ClientData.getHprofDumpHandler();
        if (result == 0) {
            if (handler != null) {
                handler.onSuccess(filename, client);
            }
            client.getClientData().setHprofData(filename);
            Log.d("ddm-heap", "Heap dump request has finished");
        } else {
            if (handler != null) {
                handler.onEndFailure(client, null);
            }
            client.getClientData().clearHprofData();
            Log.w("ddm-heap", "Heap dump request failed (check device log)");
        }
        client.update(Client.CHANGE_HPROF);
        client.getClientData().clearHprofData();
    }

    /*
     * Handle HeaP Dump Streaming response.  "data" contains the full
     * hprof dump.
     */
    private void handleHPDS(Client client, ByteBuffer data) {
        byte[] stuff = new byte[data.capacity()];
        data.get(stuff, 0, stuff.length);

        Log.d("ddm-hprof", "got hprof file, size: " + data.capacity() + " bytes");
        client.getClientData().setHprofData(stuff);
        IHprofDumpHandler handler = ClientData.getHprofDumpHandler();
        if (handler != null) {
            handler.onSuccess(stuff, client);
        }
        client.update(Client.CHANGE_HPROF);
        client.getClientData().clearHprofData();
    }

    /**
     * Sends a REAE (REcent Allocation Enable) request to the client.
     */
    public static void sendREAE(Client client, boolean enable)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(1);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.put((byte) (enable ? 1 : 0));

        finishChunkPacket(packet, CHUNK_REAE, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_REAE) + ": " + enable);
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends a REAQ (REcent Allocation Query) request to the client.
     */
    public static void sendREAQ(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_REAQ, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_REAQ));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends a REAL (REcent ALlocation) request to the client.
     */
    public static void sendREAL(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_REAL, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_REAL));
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle the response from our REcent Allocation Query message.
     */
    private void handleREAQ(Client client, ByteBuffer data) {
        boolean enabled;

        enabled = (data.get() != 0);
        Log.d("ddm-heap", "REAQ says: enabled=" + enabled);

        client.getClientData().setAllocationStatus(enabled ? AllocationTrackingStatus.ON : AllocationTrackingStatus.OFF);
        client.update(Client.CHANGE_HEAP_ALLOCATION_STATUS);
    }

    /*
     * Handle a REcent ALlocation response.
     */
    private void handleREAL(Client client, ByteBuffer data) {
        Log.e("ddm-heap", "*** Received " + name(CHUNK_REAL));
        ClientData.IAllocationTrackingHandler handler = ClientData.getAllocationTrackingHandler();

        if (handler != null) {
          byte[] stuff = new byte[data.capacity()];
          data.get(stuff, 0, stuff.length);

          Log.d("ddm-prof", "got allocations file, size: " + stuff.length + " bytes");
          handler.onSuccess(stuff, client);
        } else {
          // Allocation tracking did not start from Android Studio's device panel
          client.getClientData().setAllocations(AllocationsParser.parse(data));
          client.update(Client.CHANGE_HEAP_ALLOCATIONS);
        }
    }
}

