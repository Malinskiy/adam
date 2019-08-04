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

import com.android.annotations.Nullable;
import com.android.ddmlib.log.LogReceiver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to handle requests and connections to adb.
 * <p/>{@link AndroidDebugBridge} is the public API to connection to adb, while {@link AdbHelper}
 * does the low level stuff.
 * <p/>This currently uses spin-wait non-blocking I/O. A Selector would be more efficient,
 * but seems like overkill for what we're doing here.
 */
final class AdbHelper {

    // public static final long kOkay = 0x59414b4fL;
    // public static final long kFail = 0x4c494146L;

    static final int WAIT_TIME = 5; // spin-wait sleep, in ms

    static final String DEFAULT_ENCODING = "ISO-8859-1"; //$NON-NLS-1$

    /** do not instantiate */
    private AdbHelper() {
    }

    /**
     * Response from ADB.
     */
    static class AdbResponse {
        public AdbResponse() {
            message = "";
        }

        public boolean okay; // first 4 bytes in response were "OKAY"?

        public String message; // diagnostic string if #okay is false
    }

    /**
     * Create and connect a new pass-through socket, from the host to a port on
     * the device.
     *
     * @param adbSockAddr
     * @param device the device to connect to. Can be null in which case the connection will be
     * to the first available device.
     * @param devicePort the port we're opening
     * @throws TimeoutException in case of timeout on the connection.
     * @throws IOException in case of I/O error on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     */
    public static SocketChannel open(InetSocketAddress adbSockAddr,
            Device device, int devicePort)
            throws IOException, TimeoutException, AdbCommandRejectedException {

        SocketChannel adbChan = SocketChannel.open(adbSockAddr);
        try {
            adbChan.socket().setTcpNoDelay(true);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to
            // talk to a specific device
            setDevice(adbChan, device);

            byte[] req = createAdbForwardRequest(null, devicePort);
            // Log.hexDump(req);

            write(adbChan, req);

            AdbResponse resp = readAdbResponse(adbChan, false);
            if (!resp.okay) {
                throw new AdbCommandRejectedException(resp.message);
            }

            adbChan.configureBlocking(true);
        } catch (TimeoutException e) {
            adbChan.close();
            throw e;
        } catch (IOException e) {
            adbChan.close();
            throw e;
        } catch (AdbCommandRejectedException e) {
            adbChan.close();
            throw e;
        }

        return adbChan;
    }

    /**
     * Creates and connects a new pass-through socket, from the host to a port on
     * the device.
     *
     * @param adbSockAddr
     * @param device the device to connect to. Can be null in which case the connection will be
     * to the first available device.
     * @param pid the process pid to connect to.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public static SocketChannel createPassThroughConnection(InetSocketAddress adbSockAddr,
            Device device, int pid)
            throws TimeoutException, AdbCommandRejectedException, IOException {

        SocketChannel adbChan = SocketChannel.open(adbSockAddr);
        try {
            adbChan.socket().setTcpNoDelay(true);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to
            // talk to a specific device
            setDevice(adbChan, device);

            byte[] req = createJdwpForwardRequest(pid);
            // Log.hexDump(req);

            write(adbChan, req);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                throw new AdbCommandRejectedException(resp.message);
            }

            adbChan.configureBlocking(true);
        } catch (TimeoutException e) {
            adbChan.close();
            throw e;
        } catch (IOException e) {
            adbChan.close();
            throw e;
        } catch (AdbCommandRejectedException e) {
            adbChan.close();
            throw e;
        }

        return adbChan;
    }

    /**
     * Creates a port forwarding request for adb. This returns an array
     * containing "####tcp:{port}:{addStr}".
     * @param addrStr the host. Can be null.
     * @param port the port on the device. This does not need to be numeric.
     */
    private static byte[] createAdbForwardRequest(String addrStr, int port) {
        String reqStr;

        if (addrStr == null)
            reqStr = "tcp:" + port;
        else
            reqStr = "tcp:" + port + ":" + addrStr;
        return formAdbRequest(reqStr);
    }

    /**
     * Creates a port forwarding request to a jdwp process. This returns an array
     * containing "####jwdp:{pid}".
     * @param pid the jdwp process pid on the device.
     */
    private static byte[] createJdwpForwardRequest(int pid) {
        String reqStr = String.format("jdwp:%1$d", pid); //$NON-NLS-1$
        return formAdbRequest(reqStr);
    }

    /**
     * Create an ASCII string preceded by four hex digits. The opening "####"
     * is the length of the rest of the string, encoded as ASCII hex (case
     * doesn't matter).
     */
    public static byte[] formAdbRequest(String req) {
        String resultStr = String.format("%04X%s", req.length(), req); //$NON-NLS-1$
        byte[] result;
        try {
            result = resultStr.getBytes(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace(); // not expected
            return null;
        }
        assert result.length == req.length() + 4;
        return result;
    }

    /**
     * Reads the response from ADB after a command.
     * @param chan The socket channel that is connected to adb.
     * @param readDiagString If true, we're expecting an OKAY response to be
     *      followed by a diagnostic string. Otherwise, we only expect the
     *      diagnostic string to follow a FAIL.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws IOException in case of I/O error on the connection.
     */
    static AdbResponse readAdbResponse(SocketChannel chan, boolean readDiagString)
            throws TimeoutException, IOException {

        AdbResponse resp = new AdbResponse();

        byte[] reply = new byte[4];
        read(chan, reply);

        if (isOkay(reply)) {
            resp.okay = true;
        } else {
            readDiagString = true; // look for a reason after the FAIL
            resp.okay = false;
        }

        // not a loop -- use "while" so we can use "break"
        try {
            while (readDiagString) {
                // length string is in next 4 bytes
                byte[] lenBuf = new byte[4];
                read(chan, lenBuf);

                String lenStr = replyToString(lenBuf);

                int len;
                try {
                    len = Integer.parseInt(lenStr, 16);
                } catch (NumberFormatException nfe) {
                    Log.w("ddms", "Expected digits, got '" + lenStr + "': "
                            + lenBuf[0] + " " + lenBuf[1] + " " + lenBuf[2] + " "
                            + lenBuf[3]);
                    Log.w("ddms", "reply was " + replyToString(reply));
                    break;
                }

                byte[] msg = new byte[len];
                read(chan, msg);

                resp.message = replyToString(msg);
                Log.v("ddms", "Got reply '" + replyToString(reply) + "', diag='"
                        + resp.message + "'");

                break;
            }
        } catch (Exception e) {
            // ignore those, since it's just reading the diagnose string, the response will
            // contain okay==false anyway.
        }

        return resp;
    }

    /**
     * Retrieve the frame buffer from the device with the given timeout. A timeout of 0 indicates
     * that it will wait forever.
     *
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    static RawImage getFrameBuffer(InetSocketAddress adbSockAddr, Device device, long timeout,
      TimeUnit unit)
            throws TimeoutException, AdbCommandRejectedException, IOException {

        RawImage imageParams = new RawImage();
        byte[] request = formAdbRequest("framebuffer:"); //$NON-NLS-1$
        byte[] nudge = {
            0
        };
        byte[] reply;

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to talk
            // to a specific device
            setDevice(adbChan, device);

            write(adbChan, request);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                throw new AdbCommandRejectedException(resp.message);
            }

            // first the protocol version.
            reply = new byte[4];
            read(adbChan, reply);

            ByteBuffer buf = ByteBuffer.wrap(reply);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            int version = buf.getInt();

            // get the header size (this is a count of int)
            int headerSize = RawImage.getHeaderSize(version);

            // read the header
            reply = new byte[headerSize * 4];
            read(adbChan, reply);

            buf = ByteBuffer.wrap(reply);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // fill the RawImage with the header
            if (!imageParams.readHeader(version, buf)) {
                Log.e("Screenshot", "Unsupported protocol: " + version);
                return null;
            }

            Log.d("ddms", "image params: bpp=" + imageParams.bpp + ", size="
                    + imageParams.size + ", width=" + imageParams.width
                    + ", height=" + imageParams.height);

            write(adbChan, nudge);

            reply = new byte[imageParams.size];
            read(adbChan, reply, imageParams.size, unit.toMillis(timeout));

            imageParams.data = reply;
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }

        return imageParams;
    }

    /**
     * @deprecated Use {@link #executeRemoteCommand(java.net.InetSocketAddress, String, IDevice, IShellOutputReceiver, long, java.util.concurrent.TimeUnit)}.
     */
    @Deprecated
    static void executeRemoteCommand(InetSocketAddress adbSockAddr,
        String command, IDevice device, IShellOutputReceiver rcvr, int maxTimeToOutputResponse)
        throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException,
        IOException {
        executeRemoteCommand(adbSockAddr, command, device, rcvr, maxTimeToOutputResponse,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Executes a shell command on the device and retrieve the output. The output is
     * handed to <var>rcvr</var> as it arrives.
     *
     * @param adbSockAddr the {@link InetSocketAddress} to adb.
     * @param command the shell command to execute
     * @param device the {@link IDevice} on which to execute the command.
     * @param rcvr the {@link IShellOutputReceiver} that will receives the output of the shell
     *            command
     * @param maxTimeToOutputResponse max time between command output. If more time passes
     *            between command output, the method will throw
     *            {@link ShellCommandUnresponsiveException}. A value of 0 means the method will
     *            wait forever for command output and never throw.
     * @param maxTimeUnits Units for non-zero {@code maxTimeToOutputResponse} values.
     * @throws TimeoutException in case of timeout on the connection when sending the command.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws ShellCommandUnresponsiveException in case the shell command doesn't send any output
     *            for a period longer than <var>maxTimeToOutputResponse</var>.
     * @throws IOException in case of I/O error on the connection.
     *
     * @see DdmPreferences#getTimeOut()
     */
    static void executeRemoteCommand(InetSocketAddress adbSockAddr,
        String command, IDevice device, IShellOutputReceiver rcvr, long maxTimeToOutputResponse,
        TimeUnit maxTimeUnits) throws TimeoutException, AdbCommandRejectedException,
        ShellCommandUnresponsiveException, IOException {

        executeRemoteCommand(adbSockAddr, AdbService.SHELL, command, device, rcvr, maxTimeToOutputResponse,
                maxTimeUnits, null /* inputStream */);
    }

    /**
     * Identify which adb service the command should target.
     */
    public enum AdbService {
        /**
         * the shell service
         */
        SHELL,

        /**
         * The exec service.
         */
        EXEC
    }

    /**
     * Executes a remote command on the device and retrieve the output. The output is
     * handed to <var>rcvr</var> as it arrives. The command is execute by the remote service
     * identified by the adbService parameter.
     *
     * @param adbSockAddr the {@link InetSocketAddress} to adb.
     * @param adbService the {@link com.android.ddmlib.AdbHelper.AdbService} to use to run the
     *                   command.
     * @param command the shell command to execute
     * @param device the {@link IDevice} on which to execute the command.
     * @param rcvr the {@link IShellOutputReceiver} that will receives the output of the shell
     *            command
     * @param maxTimeToOutputResponse max time between command output. If more time passes
     *            between command output, the method will throw
     *            {@link ShellCommandUnresponsiveException}. A value of 0 means the method will
     *            wait forever for command output and never throw.
     * @param maxTimeUnits Units for non-zero {@code maxTimeToOutputResponse} values.
     * @param is a optional {@link InputStream} to be streamed up after invoking the command
     *           and before retrieving the response.
     * @throws TimeoutException in case of timeout on the connection when sending the command.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws ShellCommandUnresponsiveException in case the shell command doesn't send any output
     *            for a period longer than <var>maxTimeToOutputResponse</var>.
     * @throws IOException in case of I/O error on the connection.
     *
     * @see DdmPreferences#getTimeOut()
     */
    static void executeRemoteCommand(InetSocketAddress adbSockAddr, AdbService adbService,
            String command, IDevice device, IShellOutputReceiver rcvr, long maxTimeToOutputResponse,
            TimeUnit maxTimeUnits,
            @Nullable InputStream is) throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {

        long maxTimeToOutputMs = 0;
        if (maxTimeToOutputResponse > 0) {
            if (maxTimeUnits == null) {
                throw new NullPointerException("Time unit must not be null for non-zero max.");
            }
            maxTimeToOutputMs = maxTimeUnits.toMillis(maxTimeToOutputResponse);
        }

        Log.v("ddms", "execute: running " + command);

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to
            // talk
            // to a specific device
            setDevice(adbChan, device);

            byte[] request = formAdbRequest(adbService.name().toLowerCase() + ":" + command); //$NON-NLS-1$
            write(adbChan, request);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                Log.e("ddms", "ADB rejected shell command (" + command + "): " + resp.message);
                throw new AdbCommandRejectedException(resp.message);
            }

            byte[] data = new byte[16384];

            // stream the input file if present.
            if (is != null) {
                int read;
                while ((read = is.read(data)) != -1) {
                    ByteBuffer buf = ByteBuffer.wrap(data, 0, read);
                    int written = 0;
                    while (buf.hasRemaining()) {
                        written += adbChan.write(buf);
                    }
                    if (written != read) {
                        Log.e("ddms",
                                "ADB write inconsistency, wrote " + written + "expected " + read);
                        throw new AdbCommandRejectedException("write failed");
                    }
                }
            }

            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.clear();
            long timeToResponseCount = 0;
            while (true) {
                int count;

                if (rcvr != null && rcvr.isCancelled()) {
                    Log.v("ddms", "execute: cancelled");
                    break;
                }

                count = adbChan.read(buf);
                if (count < 0) {
                    // we're at the end, we flush the output
                    rcvr.flush();
                    Log.v("ddms", "execute '" + command + "' on '" + device + "' : EOF hit. Read: "
                            + count);
                    break;
                } else if (count == 0) {
                    try {
                        int wait = WAIT_TIME * 5;
                        timeToResponseCount += wait;
                        if (maxTimeToOutputMs > 0 && timeToResponseCount > maxTimeToOutputMs) {
                            throw new ShellCommandUnresponsiveException();
                        }
                        Thread.sleep(wait);
                    } catch (InterruptedException ie) {
                    }
                } else {
                    // reset timeout
                    timeToResponseCount = 0;

                    // send data to receiver if present
                    if (rcvr != null) {
                        rcvr.addOutput(buf.array(), buf.arrayOffset(), buf.position());
                    }
                    buf.rewind();
                }
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
            Log.v("ddms", "execute: returning");
        }
    }

    /**
     * Runs the Event log service on the {@link Device}, and provides its output to the
     * {@link LogReceiver}.
     * <p/>This call is blocking until {@link LogReceiver#isCancelled()} returns true.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the Device on which to run the service
     * @param rcvr the {@link LogReceiver} to receive the log output
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public static void runEventLogService(InetSocketAddress adbSockAddr, Device device,
            LogReceiver rcvr) throws TimeoutException, AdbCommandRejectedException, IOException {
        runLogService(adbSockAddr, device, "events", rcvr); //$NON-NLS-1$
    }

    /**
     * Runs a log service on the {@link Device}, and provides its output to the {@link LogReceiver}.
     * <p/>This call is blocking until {@link LogReceiver#isCancelled()} returns true.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the Device on which to run the service
     * @param logName the name of the log file to output
     * @param rcvr the {@link LogReceiver} to receive the log output
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public static void runLogService(InetSocketAddress adbSockAddr, Device device, String logName,
            LogReceiver rcvr) throws TimeoutException, AdbCommandRejectedException, IOException {
        SocketChannel adbChan = null;

        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to talk
            // to a specific device
            setDevice(adbChan, device);

            byte[] request = formAdbRequest("log:" + logName);
            write(adbChan, request);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                throw new AdbCommandRejectedException(resp.message);
            }

            byte[] data = new byte[16384];
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (true) {
                int count;

                if (rcvr != null && rcvr.isCancelled()) {
                    break;
                }

                count = adbChan.read(buf);
                if (count < 0) {
                    break;
                } else if (count == 0) {
                    try {
                        Thread.sleep(WAIT_TIME * 5);
                    } catch (InterruptedException ie) {
                    }
                } else {
                    if (rcvr != null) {
                        rcvr.parseNewData(buf.array(), buf.arrayOffset(), buf.position());
                    }
                    buf.rewind();
                }
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }
    }

    /**
     * Creates a port forwarding between a local and a remote port.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the device on which to do the port forwarding
     * @param localPortSpec specification of the local port to forward, should be of format
     *                             tcp:<port number>
     * @param remotePortSpec specification of the remote port to forward to, one of:
     *                             tcp:<port>
     *                             localabstract:<unix domain socket name>
     *                             localreserved:<unix domain socket name>
     *                             localfilesystem:<unix domain socket name>
     *                             dev:<character device name>
     *                             jdwp:<process pid> (remote only)
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public static void createForward(InetSocketAddress adbSockAddr, Device device,
            String localPortSpec, String remotePortSpec)
                    throws TimeoutException, AdbCommandRejectedException, IOException {

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            byte[] request = formAdbRequest(String.format(
                    "host-serial:%1$s:forward:%2$s;%3$s", //$NON-NLS-1$
                    device.getSerialNumber(), localPortSpec, remotePortSpec));

            write(adbChan, request);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                Log.w("create-forward", "Error creating forward: " + resp.message);
                throw new AdbCommandRejectedException(resp.message);
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }
    }

    /**
     * Remove a port forwarding between a local and a remote port.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the device on which to remove the port forwarding
     * @param localPortSpec specification of the local port that was forwarded, should be of format
     *                             tcp:<port number>
     * @param remotePortSpec specification of the remote port forwarded to, one of:
     *                             tcp:<port>
     *                             localabstract:<unix domain socket name>
     *                             localreserved:<unix domain socket name>
     *                             localfilesystem:<unix domain socket name>
     *                             dev:<character device name>
     *                             jdwp:<process pid> (remote only)
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public static void removeForward(InetSocketAddress adbSockAddr, Device device,
            String localPortSpec, String remotePortSpec)
                    throws TimeoutException, AdbCommandRejectedException, IOException {

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            byte[] request = formAdbRequest(String.format(
                    "host-serial:%1$s:killforward:%2$s", //$NON-NLS-1$
                    device.getSerialNumber(), localPortSpec));

            write(adbChan, request);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                Log.w("remove-forward", "Error creating forward: " + resp.message);
                throw new AdbCommandRejectedException(resp.message);
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }
    }

    /**
     * Checks to see if the first four bytes in "reply" are OKAY.
     */
    static boolean isOkay(byte[] reply) {
        return reply[0] == (byte)'O' && reply[1] == (byte)'K'
                && reply[2] == (byte)'A' && reply[3] == (byte)'Y';
    }

    /**
     * Converts an ADB reply to a string.
     */
    static String replyToString(byte[] reply) {
        String result;
        try {
            result = new String(reply, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace(); // not expected
            result = "";
        }
        return result;
    }

    /**
     * Reads from the socket until the array is filled, or no more data is coming (because
     * the socket closed or the timeout expired).
     * <p/>This uses the default time out value.
     *
     * @param chan the opened socket to read from. It must be in non-blocking
     *      mode for timeouts to work
     * @param data the buffer to store the read data into.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws IOException in case of I/O error on the connection.
     */
    static void read(SocketChannel chan, byte[] data) throws TimeoutException, IOException {
        read(chan, data, -1, DdmPreferences.getTimeOut());
    }

    /**
     * Reads from the socket until the array is filled, the optional length
     * is reached, or no more data is coming (because the socket closed or the
     * timeout expired). After "timeout" milliseconds since the
     * previous successful read, this will return whether or not new data has
     * been found.
     *
     * @param chan the opened socket to read from. It must be in non-blocking
     *      mode for timeouts to work
     * @param data the buffer to store the read data into.
     * @param length the length to read or -1 to fill the data buffer completely
     * @param timeout The timeout value in ms. A timeout of zero means "wait forever".
     */
    static void read(SocketChannel chan, byte[] data, int length, long timeout)
            throws TimeoutException, IOException {
        ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
        int numWaits = 0;

        while (buf.position() != buf.limit()) {
            int count;

            count = chan.read(buf);
            if (count < 0) {
                Log.d("ddms", "read: channel EOF");
                throw new IOException("EOF");
            } else if (count == 0) {
                // TODO: need more accurate timeout?
                if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
                    Log.d("ddms", "read: timeout");
                    throw new TimeoutException();
                }
                // non-blocking spin
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException ie) {
                }
                numWaits++;
            } else {
                numWaits = 0;
            }
        }
    }

    /**
     * Write until all data in "data" is written or the connection fails or times out.
     * <p/>This uses the default time out value.
     * @param chan the opened socket to write to.
     * @param data the buffer to send.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws IOException in case of I/O error on the connection.
     */
    static void write(SocketChannel chan, byte[] data) throws TimeoutException, IOException {
        write(chan, data, -1, DdmPreferences.getTimeOut());
    }

    /**
     * Write until all data in "data" is written, the optional length is reached,
     * the timeout expires, or the connection fails. Returns "true" if all
     * data was written.
     * @param chan the opened socket to write to.
     * @param data the buffer to send.
     * @param length the length to write or -1 to send the whole buffer.
     * @param timeout The timeout value. A timeout of zero means "wait forever".
     * @throws TimeoutException in case of timeout on the connection.
     * @throws IOException in case of I/O error on the connection.
     */
    static void write(SocketChannel chan, byte[] data, int length, int timeout)
            throws TimeoutException, IOException {
        ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
        int numWaits = 0;

        while (buf.position() != buf.limit()) {
            int count;

            count = chan.write(buf);
            if (count < 0) {
                Log.d("ddms", "write: channel EOF");
                throw new IOException("channel EOF");
            } else if (count == 0) {
                // TODO: need more accurate timeout?
                if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
                    Log.d("ddms", "write: timeout");
                    throw new TimeoutException();
                }
                // non-blocking spin
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException ie) {
                }
                numWaits++;
            } else {
                numWaits = 0;
            }
        }
    }

    /**
     * tells adb to talk to a specific device
     *
     * @param adbChan the socket connection to adb
     * @param device The device to talk to.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    static void setDevice(SocketChannel adbChan, IDevice device)
            throws TimeoutException, AdbCommandRejectedException, IOException {
        // if the device is not -1, then we first tell adb we're looking to talk
        // to a specific device
        if (device != null) {
            String msg = "host:transport:" + device.getSerialNumber(); //$NON-NLS-1$
            byte[] device_query = formAdbRequest(msg);

            write(adbChan, device_query);

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay) {
                throw new AdbCommandRejectedException(resp.message,
                        true/*errorDuringDeviceSelection*/);
            }
        }
    }

    /**
     * Reboot the device.
     *
     * @param into what to reboot into (recovery, bootloader).  Or null to just reboot.
     * @throws TimeoutException in case of timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws IOException in case of I/O error on the connection.
     */
    public static void reboot(String into, InetSocketAddress adbSockAddr,
            Device device) throws TimeoutException, AdbCommandRejectedException, IOException {
        byte[] request;
        if (into == null) {
            request = formAdbRequest("reboot:"); //$NON-NLS-1$
        } else {
            request = formAdbRequest("reboot:" + into); //$NON-NLS-1$
        }

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to talk
            // to a specific device
            setDevice(adbChan, device);

            write(adbChan, request);
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }
    }
}
