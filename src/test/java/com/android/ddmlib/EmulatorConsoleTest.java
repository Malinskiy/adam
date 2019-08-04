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
package com.android.ddmlib;

import junit.framework.TestCase;

/**
 * Unit tests for {@link EmulatorConsole}.
 */
public class EmulatorConsoleTest extends TestCase {

    /**
     * Test success case for {@link EmulatorConsole#getEmulatorPort(String)}.
     */
    public void testGetEmulatorPort() {
        assertEquals(Integer.valueOf(5554), EmulatorConsole.getEmulatorPort("emulator-5554"));
    }

    /**
     * Test {@link EmulatorConsole#getEmulatorPort(String)} when input serial has invalid format.
     */
    public void testGetEmulatorPort_invalid() {
        assertNull(EmulatorConsole.getEmulatorPort("invalidserial"));
    }

    /**
     * Test {@link EmulatorConsole#getEmulatorPort(String)} when port is not a number.
     */
    public void testGetEmulatorPort_nan() {
        assertNull(EmulatorConsole.getEmulatorPort("emulator-NaN"));
    }
}
