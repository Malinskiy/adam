/*
 * Copyright (C) 2015 The Android Open Source Project
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

public class AdbVersionTest extends TestCase {
    public void testParser() {
        AdbVersion version = AdbVersion.parseFrom("Android Debug Bridge version 1.0.32");
        assertEquals("1.0.32", version.toString());

        version = AdbVersion.parseFrom("Android Debug Bridge version 1.0.32 8b22c293a0e5-android");
        assertEquals(AdbVersion.parseFrom("1.0.32"), version);

        version = AdbVersion.parseFrom("1.0.unknown");
        assertEquals(AdbVersion.UNKNOWN, version);
    }

    public void testComponents() {
        AdbVersion version = AdbVersion
                .parseFrom("Android Debug Bridge version 1.23.32 8b22c293a0e5-android");
        assertEquals(1, version.major);
        assertEquals(23, version.minor);
        assertEquals(32, version.micro);
    }

    public void testComparison() {
        AdbVersion min = AdbVersion.parseFrom("1.0.20");
        AdbVersion now = AdbVersion.parseFrom("1.0.32");
        assertTrue(now.compareTo(min) > 0);
        assertTrue(min.compareTo(now) < 0);
        assertTrue(now.compareTo(now) == 0);

        AdbVersion f = AdbVersion.parseFrom("2.0.32");
        assertTrue(f.compareTo(now) > 0);
    }
}
