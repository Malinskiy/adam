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

package com.android.ddmlib.receiver;

import com.android.ddmlib.logging.Log;
import com.android.ddmlib.interactor.BatteryFetcher;
import com.android.ddmlib.receiver.base.MultiLineReceiver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Output receiver for "dumpsys battery" command line.
 */
public final class BatteryReceiver extends MultiLineReceiver {
    private static final Pattern BATTERY_LEVEL = Pattern.compile("\\s*level: (\\d+)");
    private static final Pattern SCALE = Pattern.compile("\\s*scale: (\\d+)");

    private Integer mBatteryLevel = null;
    private Integer mBatteryScale = null;

    /**
     * Get the parsed percent battery level.
     * @return
     */
    public Integer getBatteryLevel() {
        if (mBatteryLevel != null && mBatteryScale != null) {
            return (mBatteryLevel * 100) / mBatteryScale;
        }
        return null;
    }

    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            Matcher batteryMatch = BATTERY_LEVEL.matcher(line);
            if (batteryMatch.matches()) {
                try {
                    mBatteryLevel = Integer.parseInt(batteryMatch.group(1));
                } catch (NumberFormatException e) {
                    Log.w(BatteryFetcher.LOG_TAG, String.format("Failed to parse %s as an integer",
                                                                batteryMatch.group(1)));
                }
            }
            Matcher scaleMatch = SCALE.matcher(line);
            if (scaleMatch.matches()) {
                try {
                    mBatteryScale = Integer.parseInt(scaleMatch.group(1));
                } catch (NumberFormatException e) {
                    Log.w(BatteryFetcher.LOG_TAG, String.format("Failed to parse %s as an integer",
                                                                batteryMatch.group(1)));
                }
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
