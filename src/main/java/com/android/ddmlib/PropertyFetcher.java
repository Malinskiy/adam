/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches and caches 'getprop' values from device.
 */
class PropertyFetcher {
    /** the amount of time to wait between unsuccessful prop fetch attempts */
    private static final String GETPROP_COMMAND = "getprop"; //$NON-NLS-1$
    private static final Pattern GETPROP_PATTERN = Pattern.compile("^\\[([^]]+)\\]\\:\\s*\\[(.*)\\]$"); //$NON-NLS-1$
    private static final int GETPROP_TIMEOUT_SEC = 2;
    private static final int EXPECTED_PROP_COUNT = 150;

    private enum CacheState {
        UNPOPULATED, FETCHING, POPULATED
    }

    /**
     * Shell output parser for a getprop command
     */
    @VisibleForTesting
    static class GetPropReceiver extends MultiLineReceiver {

        private final Map<String, String> mCollectedProperties =
                Maps.newHashMapWithExpectedSize(EXPECTED_PROP_COUNT);

        @Override
        public void processNewLines(String[] lines) {
            // We receive an array of lines. We're expecting
            // to have the build info in the first line, and the build
            // date in the 2nd line. There seems to be an empty line
            // after all that.

            for (String line : lines) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Matcher m = GETPROP_PATTERN.matcher(line);
                if (m.matches()) {
                    String label = m.group(1);
                    String value = m.group(2);

                    if (!label.isEmpty()) {
                        mCollectedProperties.put(label, value);
                    }
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        Map<String, String> getCollectedProperties() {
            return mCollectedProperties;
        }
    }

    private final Map<String, String> mProperties = Maps.newHashMapWithExpectedSize(
            EXPECTED_PROP_COUNT);
    private final IDevice mDevice;
    private CacheState mCacheState = CacheState.UNPOPULATED;
    private final Map<String, SettableFuture<String>> mPendingRequests =
            Maps.newHashMapWithExpectedSize(4);

    public PropertyFetcher(IDevice device) {
        mDevice = device;
    }

    /**
     * Returns the full list of cached properties.
     */
    public synchronized Map<String, String> getProperties() {
        return mProperties;
    }

    /**
     * Make a possibly asynchronous request for a system property value.
     *
     * @param name the property name to retrieve
     * @return a {@link Future} that can be used to retrieve the prop value
     */
    @NonNull
    public synchronized Future<String> getProperty(@NonNull String name) {
        SettableFuture<String> result;
        if (mCacheState.equals(CacheState.FETCHING)) {
            result = addPendingRequest(name);
        } else if (mDevice.isOnline() && mCacheState.equals(CacheState.UNPOPULATED) || !isRoProp(name)) {
            // cache is empty, or this is a volatile prop that requires a query
            result = addPendingRequest(name);
            mCacheState = CacheState.FETCHING;
            initiatePropertiesQuery();
        } else {
            result = SettableFuture.create();
            // cache is populated and this is a ro prop
            result.set(mProperties.get(name));
        }
        return result;
    }

    private SettableFuture<String> addPendingRequest(String name) {
        SettableFuture<String> future = mPendingRequests.get(name);
        if (future == null) {
            future = SettableFuture.create();
            mPendingRequests.put(name, future);
        }
        return future;
    }

    private void initiatePropertiesQuery() {
        String threadName = String.format("query-prop-%s", mDevice.getSerialNumber());
        Thread propThread = new Thread(threadName) {
            @Override
            public void run() {
                try {
                    GetPropReceiver propReceiver = new GetPropReceiver();
                    mDevice.executeShellCommand(GETPROP_COMMAND, propReceiver, GETPROP_TIMEOUT_SEC,
                            TimeUnit.SECONDS);
                    populateCache(propReceiver.getCollectedProperties());
                } catch (Exception e) {
                    handleException(e);
                }
            }
        };
        propThread.setDaemon(true);
        propThread.start();
    }

    private synchronized void populateCache(@NonNull Map<String, String> props) {
        mCacheState = props.isEmpty() ? CacheState.UNPOPULATED : CacheState.POPULATED;
        if (!props.isEmpty()) {
            mProperties.putAll(props);
        }
        for (Map.Entry<String, SettableFuture<String>> entry : mPendingRequests.entrySet()) {
            entry.getValue().set(mProperties.get(entry.getKey()));
        }
        mPendingRequests.clear();
    }

    private synchronized void handleException(Exception e) {
        mCacheState = CacheState.UNPOPULATED;
        Log.w("PropertyFetcher",
                String.format("%s getting properties for device %s: %s",
                        e.getClass().getSimpleName(), mDevice.getSerialNumber(),
                        e.getMessage()));
        for (Map.Entry<String, SettableFuture<String>> entry : mPendingRequests.entrySet()) {
            entry.getValue().setException(e);
        }
        mPendingRequests.clear();
    }

    /**
     * Return true if cache is populated.
     *
     * @deprecated implementation detail
     */
    @Deprecated
    public synchronized boolean arePropertiesSet() {
        return CacheState.POPULATED.equals(mCacheState);
    }

    private static boolean isRoProp(@NonNull String propName) {
        return propName.startsWith("ro.");
    }
}
