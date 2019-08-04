/*
 * Copyright (C) 2011 The Android Open Source Project
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
import com.android.ddmlib.Log.LogLevel;

/**
 * Model a single log message output from {@code logcat -v long}.
 * A logcat message has a {@link LogLevel}, the pid (process id) of the process
 * generating the message, the time at which the message was generated, and
 * the tag and message itself.
 */
public final class LogCatMessage {
    private final LogLevel mLogLevel;
    private final String mPid;
    private final String mTid;
    private final String mAppName;
    private final String mTag;
    private final String mTime;
    private final String mMessage;

    /**
     * Construct an immutable log message object.
     */
    public LogCatMessage(@NonNull LogLevel logLevel, @NonNull String pid, @NonNull String tid,
            @NonNull String appName, @NonNull String tag,
            @NonNull String time, @NonNull String msg) {
        mLogLevel = logLevel;
        mPid = pid;
        mAppName = appName;
        mTag = tag;
        mTime = time;
        mMessage = msg;

        long tidValue;
        try {
            // Thread id's may be in hex on some platforms.
            // Decode and store them in radix 10.
            tidValue = Long.decode(tid.trim());
        } catch (NumberFormatException e) {
            tidValue = -1;
        }

        mTid = Long.toString(tidValue);
    }

    @NonNull
    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    @NonNull
    public String getPid() {
        return mPid;
    }

    @NonNull
    public String getTid() {
        return mTid;
    }

    @NonNull
    public String getAppName() {
        return mAppName;
    }

    @NonNull
    public String getTag() {
        return mTag;
    }

    @NonNull
    public String getTime() {
        return mTime;
    }

    @NonNull
    public String getMessage() {
        return mMessage;
    }

    @Override
    public String toString() {
        return mTime + ": "
                + mLogLevel.getPriorityLetter() + "/"
                + mTag + "("
                + mPid + "): "
                + mMessage;
    }
}
