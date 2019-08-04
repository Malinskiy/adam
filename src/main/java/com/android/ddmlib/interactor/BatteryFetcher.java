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
package com.android.ddmlib.interactor;

import com.android.ddmlib.exception.AdbCommandRejectedException;
import com.android.ddmlib.exception.ShellCommandUnresponsiveException;
import com.android.ddmlib.exception.TimeoutException;
import com.android.ddmlib.logging.Log;
import com.android.ddmlib.model.IDevice;
import com.android.ddmlib.receiver.BatteryReceiver;
import com.android.ddmlib.receiver.SysFsBatteryLevelReceiver;
import com.google.common.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Fetches battery level from device.
 */
public class BatteryFetcher {

    public static final String LOG_TAG = "BatteryFetcher";

    /** the amount of time to wait between unsuccessful battery fetch attempts */
    private static final long FETCH_BACKOFF_MS = 5 * 1000; // 5 seconds
    private static final long BATTERY_TIMEOUT = 2 * 1000; // 2 seconds

    private Integer mBatteryLevel = null;
    private final IDevice mDevice;
    private long mLastSuccessTime = 0;
    private SettableFuture<Integer> mPendingRequest = null;

    public BatteryFetcher(IDevice device) {
        mDevice = device;
    }

    /**
     * Make a possibly asynchronous request for the device's battery level
     *
     * @param freshness the desired recentness of battery level
     * @param timeUnit the {@link TimeUnit} of freshness
     * @return a {@link Future} that can be used to retrieve the battery level
     */
    public synchronized Future<Integer> getBattery(long freshness, TimeUnit timeUnit) {
        SettableFuture<Integer> result;
        if (mBatteryLevel == null || isFetchRequired(freshness, timeUnit)) {
            if (mPendingRequest == null) {
                // no request underway - start a new one
                mPendingRequest = SettableFuture.create();
                initiateBatteryQuery();
            } else {
                // fall through - return the already created future from the request already
                // underway
            }
            result = mPendingRequest;
        } else {
            // cache is populated within desired freshness
            result = SettableFuture.create();
            result.set(mBatteryLevel);
        }
        return result;
    }

    private boolean isFetchRequired(long freshness, TimeUnit timeUnit) {
        long freshnessMs = timeUnit.toMillis(freshness);
        return (System.currentTimeMillis() - mLastSuccessTime) > freshnessMs;
    }

    private void initiateBatteryQuery() {
        String threadName = String.format("query-battery-%s", mDevice.getSerialNumber());
        Thread fetchThread = new Thread(threadName) {
            @Override
            public void run() {
                Exception exception = null;
                try {
                    // first try to get it from sysfs
                    SysFsBatteryLevelReceiver sysBattReceiver = new SysFsBatteryLevelReceiver();
                    mDevice.executeShellCommand("cat /sys/class/power_supply/*/capacity",
                            sysBattReceiver, BATTERY_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (!setBatteryLevel(sysBattReceiver.getBatteryLevel())) {
                        // failed! try dumpsys
                        BatteryReceiver receiver = new BatteryReceiver();
                        mDevice.executeShellCommand("dumpsys battery", receiver, BATTERY_TIMEOUT,
                                TimeUnit.MILLISECONDS);
                        if (setBatteryLevel(receiver.getBatteryLevel())) {
                            return;
                        }
                    }
                    exception = new IOException("Unrecognized response to battery level queries");
                } catch (TimeoutException e) {
                    exception = e;
                } catch (AdbCommandRejectedException e) {
                    exception = e;
                } catch (ShellCommandUnresponsiveException e) {
                    exception = e;
                } catch (IOException e) {
                    exception = e;
                }
                handleBatteryLevelFailure(exception);
            }
        };
        fetchThread.setDaemon(true);
        fetchThread.start();
    }

    private synchronized boolean setBatteryLevel(Integer batteryLevel) {
        if (batteryLevel == null) {
            return false;
        }
        mLastSuccessTime = System.currentTimeMillis();
        mBatteryLevel = batteryLevel;
        if (mPendingRequest != null) {
            mPendingRequest.set(mBatteryLevel);
        }
        mPendingRequest = null;
        return true;
    }

    private synchronized void handleBatteryLevelFailure(Exception e) {
        Log.w(LOG_TAG, String.format(
                "%s getting battery level for device %s: %s",
                e.getClass().getSimpleName(), mDevice.getSerialNumber(), e.getMessage()));
        if (mPendingRequest != null) {
            if (!mPendingRequest.setException(e)) {
                // should never happen
                Log.e(LOG_TAG, "Future.setException failed");
                mPendingRequest.set(null);
            }
        }
        mPendingRequest = null;
    }
}
