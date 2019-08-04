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

package com.android.ddmlib;

import java.util.concurrent.TimeUnit;

public class ScreenRecorderOptions {
    // video size is given by width x height, defaults to device's main display resolution
    // or 1280x720.
    public final int width;
    public final int height;

    // bit rate in Mbps. Defaults to 4Mbps
    public final int bitrateMbps;

    // time limit, maximum of 3 seconds
    public final long timeLimit;
    public final TimeUnit timeLimitUnits;

    private ScreenRecorderOptions(Builder builder) {
        width = builder.mWidth;
        height = builder.mHeight;

        bitrateMbps = builder.mBitRate;

        timeLimit = builder.mTime;
        timeLimitUnits = builder.mTimeUnits;
    }

    public static class Builder {
        private int mWidth;
        private int mHeight;
        private int mBitRate;
        private long mTime;
        private TimeUnit mTimeUnits;

        public Builder setSize(int w, int h) {
            mWidth = w;
            mHeight = h;
            return this;
        }

        public Builder setBitRate(int bitRateMbps) {
            mBitRate = bitRateMbps;
            return this;
        }

        public Builder setTimeLimit(long time, TimeUnit units) {
            mTime = time;
            mTimeUnits = units;
            return this;
        }

        public ScreenRecorderOptions build() {
            return new ScreenRecorderOptions(this);
        }
    }
}
