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

import com.android.ddmlib.BatteryFetcher.SysFsBatteryLevelReceiver;

import junit.framework.TestCase;

import java.util.Random;

public class SysFsBatteryLevelReceiverTest extends TestCase {

    private SysFsBatteryLevelReceiver mReceiver;
    private Integer mExpected1, mExpected2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReceiver = new SysFsBatteryLevelReceiver();
        Random r = new Random(System.currentTimeMillis());
        mExpected1 = r.nextInt(101);
        mExpected2 = r.nextInt(101);
    }

    public void testSingleLine() {
        String[] lines = {mExpected1.toString()};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testWithTrailingWhitespace1() {
        String[] lines = {mExpected1 + " "};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testWithTrailingWhitespace2() {
        String[] lines = {mExpected1 + "\n"};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testWithTrailingWhitespace3() {
        String[] lines = {mExpected1 + "\r"};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testWithTrailingWhitespace4() {
        String[] lines = {mExpected1 + "\r\n"};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testMultipleLinesSame() {
        String[] lines = {mExpected1 + "\n", mExpected2.toString()};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testMultipleLinesDifferent() {
        String[] lines = {mExpected1 + "\n", mExpected2.toString()};
        mReceiver.processNewLines(lines);
        assertEquals(mExpected1, mReceiver.getBatteryLevel());
    }

    public void testInvalid() {
        String[] lines = {"foo\n", "bar", "yadda"};
        mReceiver.processNewLines(lines);
        assertNull(mReceiver.getBatteryLevel());
    }
}
