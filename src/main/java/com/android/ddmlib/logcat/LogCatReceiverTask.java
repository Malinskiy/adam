/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.ddmlib.logcat;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.GuardedBy;
import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogCatReceiverTask implements Runnable {
    private static final String LOGCAT_COMMAND = "logcat -v long"; //$NON-NLS-1$
    private static final int DEVICE_POLL_INTERVAL_MSEC = 1000;

    private static final LogCatMessage sDeviceDisconnectedMsg =
            errorMessage("Device disconnected: 1");
    private static final LogCatMessage sConnectionTimeoutMsg =
            errorMessage("LogCat Connection timed out");
    private static final LogCatMessage sConnectionErrorMsg =
            errorMessage("LogCat Connection error");

    private final IDevice mDevice;
    private final LogCatOutputReceiver mReceiver;
    private final LogCatMessageParser mParser;
    private final AtomicBoolean mCancelled;

    @GuardedBy("this")
    private final Set<LogCatListener> mListeners = new HashSet<LogCatListener>();

    public LogCatReceiverTask(@NonNull IDevice device) {
        mDevice = device;

        mReceiver = new LogCatOutputReceiver();
        mParser = new LogCatMessageParser();
        mCancelled = new AtomicBoolean();
    }

    @Override
    public void run() {
        // wait while device comes online
        while (!mDevice.isOnline()) {
            try {
                Thread.sleep(DEVICE_POLL_INTERVAL_MSEC);
            } catch (InterruptedException e) {
                return;
            }
        }

        try {
            mDevice.executeShellCommand(LOGCAT_COMMAND, mReceiver, 0);
        } catch (TimeoutException e) {
            notifyListeners(Collections.singletonList(sConnectionTimeoutMsg));
        } catch (AdbCommandRejectedException ignored) {
            // will not be thrown as long as the shell supports logcat
        } catch (ShellCommandUnresponsiveException ignored) {
            // this will not be thrown since the last argument is 0
        } catch (IOException e) {
            notifyListeners(Collections.singletonList(sConnectionErrorMsg));
        }

        notifyListeners(Collections.singletonList(sDeviceDisconnectedMsg));
    }

    public void stop() {
        mCancelled.set(true);
    }

    private class LogCatOutputReceiver extends MultiLineReceiver {
        public LogCatOutputReceiver() {
            setTrimLine(false);
        }

        /** Implements {@link IShellOutputReceiver#isCancelled() }. */
        @Override
        public boolean isCancelled() {
            return mCancelled.get();
        }

        @Override
        public void processNewLines(String[] lines) {
            if (!mCancelled.get()) {
                processLogLines(lines);
            }
        }

        private void processLogLines(String[] lines) {
            List<LogCatMessage> newMessages = mParser.processLogLines(lines, mDevice);
            if (!newMessages.isEmpty()) {
                notifyListeners(newMessages);
            }
        }
    }

    public synchronized void addLogCatListener(LogCatListener l) {
        mListeners.add(l);
    }

    public synchronized void removeLogCatListener(LogCatListener l) {
        mListeners.remove(l);
    }

    private synchronized void notifyListeners(List<LogCatMessage> messages) {
        for (LogCatListener l: mListeners) {
            l.log(messages);
        }
    }

    private static LogCatMessage errorMessage(String msg) {
        return new LogCatMessage(LogLevel.ERROR, "", "", "", "", "", msg);
    }
}
