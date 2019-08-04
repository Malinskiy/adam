/*
 * Copyright (C) 2010 The Android Open Source Project
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


/**
 * Exception thrown when adb refuses a command.
 */
public class AdbCommandRejectedException extends Exception {
    private static final long serialVersionUID = 1L;
    private final boolean mIsDeviceOffline;
    private final boolean mErrorDuringDeviceSelection;

    AdbCommandRejectedException(String message) {
        super(message);
        mIsDeviceOffline = "device offline".equals(message);
        mErrorDuringDeviceSelection = false;
    }

    AdbCommandRejectedException(String message, boolean errorDuringDeviceSelection) {
        super(message);
        mErrorDuringDeviceSelection = errorDuringDeviceSelection;
        mIsDeviceOffline = "device offline".equals(message);
    }

    /**
     * Returns true if the error is due to the device being offline.
     */
    public boolean isDeviceOffline() {
        return mIsDeviceOffline;
    }

    /**
     * Returns whether adb refused to target a given device for the command.
     * <p/>If false, adb refused the command itself, if true, it refused to target the given
     * device.
     */
    public boolean wasErrorDuringDeviceSelection() {
        return mErrorDuringDeviceSelection;
    }
}
