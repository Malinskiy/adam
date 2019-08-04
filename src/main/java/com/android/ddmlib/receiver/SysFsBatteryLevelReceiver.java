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
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Output receiver for "cat /sys/class/power_supply/.../capacity" command line.
 */
public final class SysFsBatteryLevelReceiver extends MultiLineReceiver {

    private static final Pattern BATTERY_LEVEL = Pattern.compile("^(\\d+)[.\\s]*");
    private Integer mBatteryLevel = null;

    /**
     * Get the parsed battery level.
     * @return battery level or <code>null</code> if it cannot be determined
     */
    @Nullable
    public Integer getBatteryLevel() {
        return mBatteryLevel;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            Matcher batteryMatch = BATTERY_LEVEL.matcher(line);
            if (batteryMatch.matches()) {
                if (mBatteryLevel == null) {
                    mBatteryLevel = Integer.parseInt(batteryMatch.group(1));
                } else {
                    // multiple matches, check if they are different
                    Integer tmpLevel = Integer.parseInt(batteryMatch.group(1));
                    if (!mBatteryLevel.equals(tmpLevel)) {
                        Log.w(BatteryFetcher.LOG_TAG, String.format(
                                "Multiple lines matched with different value; " +
                                "Original: %s, Current: %s (keeping original)",
                                mBatteryLevel.toString(), tmpLevel.toString()));
                    }
                }
            }
        }
    }
}
