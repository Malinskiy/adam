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

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AndroidDebugBridgeTest extends TestCase {
    private String mAndroidHome;
    private File mAdbPath;

    @Override
    protected void setUp() throws Exception {
        mAndroidHome = System.getenv("ANDROID_HOME");
        assertNotNull(
                "This test requires ANDROID_HOME environment variable to point to a valid SDK",
                mAndroidHome);

        mAdbPath = new File(mAndroidHome, "platform-tools" + File.separator + "adb");

        AndroidDebugBridge.initIfNeeded(false);
    }

    // https://code.google.com/p/android/issues/detail?id=63170
    public void testCanRecreateAdb() throws IOException {
        AndroidDebugBridge adb = AndroidDebugBridge.createBridge(mAdbPath.getCanonicalPath(), true);
        assertNotNull(adb);
        AndroidDebugBridge.terminate();

        adb = AndroidDebugBridge.createBridge(mAdbPath.getCanonicalPath(), true);
        assertNotNull(adb);
        AndroidDebugBridge.terminate();
    }

    // Some consumers of ddmlib rely on adb being on the path, and hence being
    // able to create a bridge by simply passing in "adb" as the path to adb.
    // We should be able to create a bridge in such a case as well.
    // This test will fail if adb is not currently on the path. It is disabled since we
    // can't enforce that condition (adb on $PATH) very well from a test..
    public void disabled_testEmptyAdbPath() throws Exception {
        AdbVersion version = AndroidDebugBridge.getAdbVersion(
                new File("adb")).get(5, TimeUnit.SECONDS);
        assertTrue(version.compareTo(AdbVersion.parseFrom("1.0.20")) > 0);
    }

    public void testAdbVersion() throws Exception {
        AdbVersion version = AndroidDebugBridge
                .getAdbVersion(mAdbPath).get(5, TimeUnit.SECONDS);
        assertNotSame(version, AdbVersion.UNKNOWN);
        assertTrue(version.compareTo(AdbVersion.parseFrom("1.0.20")) > 0);
    }
}
