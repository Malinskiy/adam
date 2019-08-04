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
package com.android.ddmlib.testrunner;

import com.android.ddmlib.testrunner.TestResult.TestStatus;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * Unit tests for {@link TestRunResult}
 */
public class TestRunResultTest extends TestCase {

    public void testGetNumTestsInState() {
        TestIdentifier test = new TestIdentifier("FooTest", "testBar");
        TestRunResult result = new TestRunResult();
        assertEquals(0, result.getNumTestsInState(TestStatus.PASSED));
        result.testStarted(test);
        assertEquals(0, result.getNumTestsInState(TestStatus.PASSED));
        assertEquals(1, result.getNumTestsInState(TestStatus.INCOMPLETE));
        result.testEnded(test, Collections.EMPTY_MAP);
        assertEquals(1, result.getNumTestsInState(TestStatus.PASSED));
        assertEquals(0, result.getNumTestsInState(TestStatus.INCOMPLETE));
    }
}
