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

package com.android.ddmlib.model.chunk;


import com.android.ddmlib.Client;
import com.android.ddmlib.model.JdwpPacket;
import com.android.ddmlib.logging.Log;
import com.android.ddmlib.MonitorThread;
import com.android.ddmlib.extension.ByteBufferKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class HandleViewDebug extends ChunkHandler {
    /** Enable/Disable tracing of OpenGL calls. */
    public static final int CHUNK_VUGL = type("VUGL");

    /** List {@link ViewRootImpl}'s of this process. */
    public static final int CHUNK_VULW = type("VULW");

    /** Operation on view root, first parameter in packet should be one of VURT_* constants */
    public static final int CHUNK_VURT = type("VURT");

    /** Dump view hierarchy. */
    private static final int VURT_DUMP_HIERARCHY = 1;

    /** Capture View Layers. */
    private static final int VURT_CAPTURE_LAYERS = 2;

    /** Dump View Theme. */
    private static final int VURT_DUMP_THEME = 3;

    /**
     * Generic View Operation, first parameter in the packet should be one of the
     * VUOP_* constants below.
     */
    public static final int CHUNK_VUOP = type("VUOP");

    /** Capture View. */
    private static final int VUOP_CAPTURE_VIEW = 1;

    /** Obtain the Display List corresponding to the view. */
    private static final int VUOP_DUMP_DISPLAYLIST = 2;

    /** Profile a view. */
    private static final int VUOP_PROFILE_VIEW = 3;

    /** Invoke a method on the view. */
    private static final int VUOP_INVOKE_VIEW_METHOD = 4;

    /** Set layout parameter. */
    private static final int VUOP_SET_LAYOUT_PARAMETER = 5;

    private static final String TAG = "ddmlib"; //$NON-NLS-1$

    private static final HandleViewDebug sInstance = new HandleViewDebug();

    private static final ViewDumpHandler sViewOpNullChunkHandler =
            new NullChunkHandler(CHUNK_VUOP);

    private HandleViewDebug() {}

    public static void register(MonitorThread mt) {
        // TODO: add chunk type for auto window updates
        // and register here
        mt.registerChunkHandler(CHUNK_VUGL, sInstance);
        mt.registerChunkHandler(CHUNK_VULW, sInstance);
        mt.registerChunkHandler(CHUNK_VUOP, sInstance);
        mt.registerChunkHandler(CHUNK_VURT, sInstance);
    }

    @Override
    public void clientReady(Client client) throws IOException {}

    @Override
    public void clientDisconnected(Client client) {}

    public abstract static class ViewDumpHandler extends ChunkHandler {
        private final CountDownLatch mLatch = new CountDownLatch(1);
        private final int mChunkType;

        public ViewDumpHandler(int chunkType) {
            mChunkType = chunkType;
        }

        @Override
        public void clientReady(Client client) throws IOException {
        }

        @Override
        public void clientDisconnected(Client client) {
        }

        @Override
        public void handleChunk(Client client, int type, ByteBuffer data,
                boolean isReply, int msgId) {
            if (type != mChunkType) {
                handleUnknownChunk(client, type, data, isReply, msgId);
                return;
            }

            handleViewDebugResult(data);
            mLatch.countDown();
        }

        protected abstract void handleViewDebugResult(ByteBuffer data);

        protected void waitForResult(long timeout, TimeUnit unit) {
            try {
                mLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                // pass
            }
        }
    }

    public static void listViewRoots(Client client, ViewDumpHandler replyHandler)
            throws IOException {
        ByteBuffer buf = allocBuffer(8);
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);
        chunkBuf.putInt(1);
        finishChunkPacket(packet, CHUNK_VULW, chunkBuf.position());
        client.sendAndConsume(packet, replyHandler);
    }

    public static void dumpViewHierarchy(@NotNull Client client, @NotNull String viewRoot,
            boolean skipChildren, boolean includeProperties, @NotNull ViewDumpHandler handler)
                    throws IOException {
        ByteBuffer buf = allocBuffer(4      // opcode
                + 4                         // view root length
                + viewRoot.length() * 2     // view root
                + 4                         // skip children
                + 4);                       // include view properties
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);

        chunkBuf.putInt(VURT_DUMP_HIERARCHY);
        chunkBuf.putInt(viewRoot.length());
        ByteBufferKt.putString(chunkBuf, viewRoot);
        chunkBuf.putInt(skipChildren ? 1 : 0);
        chunkBuf.putInt(includeProperties ? 1 : 0);

        finishChunkPacket(packet, CHUNK_VURT, chunkBuf.position());
        client.sendAndConsume(packet, handler);
    }

    public static void captureLayers(@NotNull Client client, @NotNull String viewRoot,
            @NotNull ViewDumpHandler handler) throws IOException {
        int bufLen = 8 + viewRoot.length() * 2;

        ByteBuffer buf = allocBuffer(bufLen);
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);

        chunkBuf.putInt(VURT_CAPTURE_LAYERS);
        chunkBuf.putInt(viewRoot.length());
        ByteBufferKt.putString(chunkBuf, viewRoot);

        finishChunkPacket(packet, CHUNK_VURT, chunkBuf.position());
        client.sendAndConsume(packet, handler);
    }

    private static void sendViewOpPacket(@NotNull Client client, int op, @NotNull String viewRoot,
            @NotNull String view, @Nullable byte[] extra, @Nullable ViewDumpHandler handler)
                    throws IOException {
        int bufLen = 4 +                        // opcode
                4 + viewRoot.length() * 2 +     // view root strlen + view root
                4 + view.length() * 2;          // view strlen + view

        if (extra != null) {
            bufLen += extra.length;
        }

        ByteBuffer buf = allocBuffer(bufLen);
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);

        chunkBuf.putInt(op);
        chunkBuf.putInt(viewRoot.length());
        ByteBufferKt.putString(chunkBuf, viewRoot);

        chunkBuf.putInt(view.length());
        ByteBufferKt.putString(chunkBuf, view);

        if (extra != null) {
            chunkBuf.put(extra);
        }

        finishChunkPacket(packet, CHUNK_VUOP, chunkBuf.position());
        if (handler != null) {
            client.sendAndConsume(packet, handler);
        } else {
            client.sendAndConsume(packet);
        }
    }

    public static void profileView(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view, @NotNull ViewDumpHandler handler) throws IOException {
        sendViewOpPacket(client, VUOP_PROFILE_VIEW, viewRoot, view, null, handler);
    }

    public static void captureView(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view, @NotNull ViewDumpHandler handler) throws IOException {
        sendViewOpPacket(client, VUOP_CAPTURE_VIEW, viewRoot, view, null, handler);
    }

    public static void invalidateView(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view) throws IOException {
        invokeMethod(client, viewRoot, view, "invalidate");
    }

    public static void requestLayout(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view) throws IOException {
        invokeMethod(client, viewRoot, view, "requestLayout");
    }

    public static void dumpDisplayList(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view) throws IOException {
        sendViewOpPacket(client, VUOP_DUMP_DISPLAYLIST, viewRoot, view, null,
                sViewOpNullChunkHandler);
    }

    public static void dumpTheme(@NotNull Client client, @NotNull String viewRoot,
            @NotNull ViewDumpHandler handler)
            throws IOException {
        ByteBuffer buf = allocBuffer(4      // opcode
                + 4                         // view root length
                + viewRoot.length() * 2);     // view root
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);

        chunkBuf.putInt(VURT_DUMP_THEME);
        chunkBuf.putInt(viewRoot.length());
        ByteBufferKt.putString(chunkBuf, viewRoot);

        finishChunkPacket(packet, CHUNK_VURT, chunkBuf.position());
        client.sendAndConsume(packet, handler);
    }

    /** A {@link ViewDumpHandler} to use when no response is expected. */
    private static class NullChunkHandler extends ViewDumpHandler {
        public NullChunkHandler(int chunkType) {
            super(chunkType);
        }

        @Override
        protected void handleViewDebugResult(ByteBuffer data) {
        }
    }

    public static void invokeMethod(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view, @NotNull String method, Object... args) throws IOException {
        int len = 4 + method.length() * 2;
        if (args != null) {
            // # of args
            len += 4;

            // for each argument, we send a char type specifier (2 bytes) and
            // the arg value (max primitive size = sizeof(double) = 8
            len += 10 * args.length;
        }

        byte[] extra = new byte[len];
        ByteBuffer b = ByteBuffer.wrap(extra);

        b.putInt(method.length());
        ByteBufferKt.putString(b, method);

        if (args != null) {
            b.putInt(args.length);

            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Boolean) {
                    b.putChar('Z');
                    b.put((byte) ((Boolean) arg ? 1 : 0));
                } else if (arg instanceof Byte) {
                    b.putChar('B');
                    b.put((Byte) arg);
                } else if (arg instanceof Character) {
                    b.putChar('C');
                    b.putChar((Character) arg);
                } else if (arg instanceof Short) {
                    b.putChar('S');
                    b.putShort((Short) arg);
                } else if (arg instanceof Integer) {
                    b.putChar('I');
                    b.putInt((Integer) arg);
                } else if (arg instanceof Long) {
                    b.putChar('J');
                    b.putLong((Long) arg);
                } else if (arg instanceof Float) {
                    b.putChar('F');
                    b.putFloat((Float) arg);
                } else if (arg instanceof Double) {
                    b.putChar('D');
                    b.putDouble((Double) arg);
                } else {
                    Log.e(TAG, "View method invocation only supports primitive arguments, supplied: " + arg);
                    return;
                }
            }
        }

        sendViewOpPacket(client, VUOP_INVOKE_VIEW_METHOD, viewRoot, view, extra,
                sViewOpNullChunkHandler );
    }

    public static void setLayoutParameter(@NotNull Client client, @NotNull String viewRoot,
            @NotNull String view, @NotNull String parameter, int value) throws IOException {
        int len = 4 + parameter.length() * 2 + 4;
        byte[] extra = new byte[len];
        ByteBuffer b = ByteBuffer.wrap(extra);

        b.putInt(parameter.length());
        ByteBufferKt.putString(b, parameter);
        b.putInt(value);
        sendViewOpPacket(client, VUOP_SET_LAYOUT_PARAMETER, viewRoot, view, extra,
                sViewOpNullChunkHandler);
    }

    @Override
    public void handleChunk(Client client, int type, ByteBuffer data,
            boolean isReply, int msgId) {
    }

    public static void sendStartGlTracing(Client client) throws IOException {
        ByteBuffer buf = allocBuffer(4);
        JdwpPacket packet = new JdwpPacket(buf);

        ByteBuffer chunkBuf = getChunkDataBuf(buf);
        chunkBuf.putInt(1);
        finishChunkPacket(packet, CHUNK_VUGL, chunkBuf.position());

        client.sendAndConsume(packet);
    }

    public static void sendStopGlTracing(Client client) throws IOException {
        ByteBuffer buf = allocBuffer(4);
        JdwpPacket packet = new JdwpPacket(buf);

        ByteBuffer chunkBuf = getChunkDataBuf(buf);
        chunkBuf.putInt(0);
        finishChunkPacket(packet, CHUNK_VUGL, chunkBuf.position());

        client.sendAndConsume(packet);
    }
}

