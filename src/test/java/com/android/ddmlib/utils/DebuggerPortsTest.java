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
package com.android.ddmlib.utils;

import junit.framework.TestCase;

public class DebuggerPortsTest extends TestCase {
    public void testNextFreePort() {
        DebuggerPorts freePorts = new DebuggerPorts(9000);
        assertEquals(9000, freePorts.next());
        assertEquals(9001, freePorts.next());
    }

    public void testReleasePort() {
        DebuggerPorts freePorts = new DebuggerPorts(9000);
        int first = freePorts.next();
        int second = freePorts.next();

        freePorts.free(first);
        assertEquals(first, freePorts.next());
        assertEquals(second + 1, freePorts.next());

        freePorts.free(second);
        assertEquals(second, freePorts.next());
    }
}
