/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.ddmlib.ClientData.IMethodProfilingHandler;
import com.android.ddmlib.ClientData.MethodProfilingStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Handle heap status updates.
 */
final class HandleProfiling extends ChunkHandler {

    public static final int CHUNK_MPRS = type("MPRS");
    public static final int CHUNK_MPRE = type("MPRE");
    public static final int CHUNK_MPSS = type("MPSS");
    public static final int CHUNK_MPSE = type("MPSE");
    public static final int CHUNK_SPSS = type("SPSS");
    public static final int CHUNK_SPSE = type("SPSE");
    public static final int CHUNK_MPRQ = type("MPRQ");
    public static final int CHUNK_FAIL = type("FAIL");

    private static final HandleProfiling mInst = new HandleProfiling();

    private HandleProfiling() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_MPRE, mInst);
        mt.registerChunkHandler(CHUNK_MPSE, mInst);
        mt.registerChunkHandler(CHUNK_MPRQ, mInst);
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
    public void handleChunk(Client client, int type, ByteBuffer data,
        boolean isReply, int msgId) {

        Log.d("ddm-prof", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_MPRE) {
            handleMPRE(client, data);
        } else if (type == CHUNK_MPSE) {
            handleMPSE(client, data);
        } else if (type == CHUNK_MPRQ) {
            handleMPRQ(client, data);
        } else if (type == CHUNK_FAIL) {
            handleFAIL(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /**
     * Send a MPRS (Method PRofiling Start) request to the client.
     *
     * The arguments to this method will eventually be passed to
     * android.os.Debug.startMethodTracing() on the device.
     *
     * @param fileName is the name of the file to which profiling data
     *          will be written (on the device); it will have {@link DdmConstants#DOT_TRACE}
     *          appended if necessary
     * @param bufferSize is the desired buffer size in bytes (8MB is good)
     * @param flags see startMethodTracing() docs; use 0 for default behavior
     */
    public static void sendMPRS(Client client, String fileName, int bufferSize,
        int flags) throws IOException {

        ByteBuffer rawBuf = allocBuffer(3*4 + fileName.length() * 2);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(bufferSize);
        buf.putInt(flags);
        buf.putInt(fileName.length());
        ByteBufferUtil.putString(buf, fileName);

        finishChunkPacket(packet, CHUNK_MPRS, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPRS) + " '" + fileName
            + "', size=" + bufferSize + ", flags=" + flags);
        client.sendAndConsume(packet, mInst);

        // record the filename we asked for.
        client.getClientData().setPendingMethodProfiling(fileName);

        // send a status query. this ensure that the status is properly updated if for some
        // reason starting the tracing failed.
        sendMPRQ(client);
    }

    /**
     * Send a MPRE (Method PRofiling End) request to the client.
     */
    public static void sendMPRE(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_MPRE, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPRE));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Handle notification that method profiling has finished writing
     * data to disk.
     */
    private void handleMPRE(Client client, ByteBuffer data) {
        byte result;

        // get the filename and make the client not have pending HPROF dump anymore.
        String filename = client.getClientData().getPendingMethodProfiling();
        client.getClientData().setPendingMethodProfiling(null);

        result = data.get();

        // get the app-level handler for method tracing dump
        IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
        if (handler != null) {
            if (result == 0) {
                handler.onSuccess(filename, client);

                Log.d("ddm-prof", "Method profiling has finished");
            } else {
                handler.onEndFailure(client, null /*message*/);

                Log.w("ddm-prof", "Method profiling has failed (check device log)");
            }
        }

        client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.OFF);
        client.update(Client.CHANGE_METHOD_PROFILING_STATUS);
    }

    /**
     * Send a MPSS (Method Profiling Streaming Start) request to the client.
     *
     * The arguments to this method will eventually be passed to
     * android.os.Debug.startMethodTracing() on the device.
     *
     * @param bufferSize is the desired buffer size in bytes (8MB is good)
     * @param flags see startMethodTracing() docs; use 0 for default behavior
     */
    public static void sendMPSS(Client client, int bufferSize,
        int flags) throws IOException {

        ByteBuffer rawBuf = allocBuffer(2*4);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(bufferSize);
        buf.putInt(flags);

        finishChunkPacket(packet, CHUNK_MPSS, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPSS)
            + "', size=" + bufferSize + ", flags=" + flags);
        client.sendAndConsume(packet, mInst);

        // send a status query. this ensure that the status is properly updated if for some
        // reason starting the tracing failed.
        sendMPRQ(client);
    }

    /**
     * Send a SPSS (Sampling Profiling Streaming Start) request to the client.
     *
     * @param bufferSize is the desired buffer size in bytes (8MB is good)
     * @param samplingInterval sampling interval
     * @param samplingIntervalTimeUnits units for sampling interval
     */
    public static void sendSPSS(Client client, int bufferSize, int samplingInterval,
            TimeUnit samplingIntervalTimeUnits) throws IOException {
        int interval = (int) samplingIntervalTimeUnits.toMicros(samplingInterval);

        ByteBuffer rawBuf = allocBuffer(3*4);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(bufferSize);
        buf.putInt(0); // flags
        buf.putInt(interval);

        finishChunkPacket(packet, CHUNK_SPSS, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_SPSS)
                + "', size=" + bufferSize + ", flags=0, samplingInterval=" + interval);
        client.sendAndConsume(packet, mInst);

        // send a status query. this ensure that the status is properly updated if for some
        // reason starting the tracing failed.
        sendMPRQ(client);
    }

    /**
     * Send a MPSE (Method Profiling Streaming End) request to the client.
     */
    public static void sendMPSE(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_MPSE, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPSE));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Send a SPSE (Sampling Profiling Streaming End) request to the client.
     */
    public static void sendSPSE(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_SPSE, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_SPSE));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Handle incoming profiling data.  The MPSE packet includes the
     * complete .trace file.
     */
    private void handleMPSE(Client client, ByteBuffer data) {
        IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
        if (handler != null) {
            byte[] stuff = new byte[data.capacity()];
            data.get(stuff, 0, stuff.length);

            Log.d("ddm-prof", "got trace file, size: " + stuff.length + " bytes");

            handler.onSuccess(stuff, client);
        }

        client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.OFF);
        client.update(Client.CHANGE_METHOD_PROFILING_STATUS);
    }

    /**
     * Send a MPRQ (Method PRofiling Query) request to the client.
     */
    public static void sendMPRQ(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_MPRQ, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPRQ));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Receive response to query.
     */
    private void handleMPRQ(Client client, ByteBuffer data) {
        byte result;

        result = data.get();

        if (result == 0) {
            client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.OFF);
            Log.d("ddm-prof", "Method profiling is not running");
        } else if (result == 1) {
            client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.TRACER_ON);
            Log.d("ddm-prof", "Method tracing is active");
        } else if (result == 2) {
            client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.SAMPLER_ON);
            Log.d("ddm-prof", "Sampler based profiling is active");
        }
        client.update(Client.CHANGE_METHOD_PROFILING_STATUS);
    }

    private void handleFAIL(Client client, ByteBuffer data) {
        /*int errorCode =*/ data.getInt();
        int length = data.getInt() * 2;
        String message = null;
        if (length > 0) {
            byte[] messageBuffer = new byte[length];
            data.get(messageBuffer, 0, length);
            message = new String(messageBuffer);
        }

        // this can be sent if
        // - MPRS failed (like wrong permission)
        // - MPSE failed for whatever reason

        String filename = client.getClientData().getPendingMethodProfiling();
        if (filename != null) {
            // reset the pending file.
            client.getClientData().setPendingMethodProfiling(null);

            // and notify of failure
            IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
            if (handler != null) {
                handler.onStartFailure(client, message);
            }
        } else {
            // this is MPRE
            // notify of failure
            IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
            if (handler != null) {
                handler.onEndFailure(client, message);
            }
        }

        // send a query to know the current status
        try {
            sendMPRQ(client);
        } catch (IOException e) {
            Log.e("HandleProfiling", e);
        }
    }
}

