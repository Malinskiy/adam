/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.Arrays;
import java.util.Map;

/**
 * Container for a result of a single test.
 */
public class TestResult {

    public enum TestStatus {
        /** Test failed. */
        FAILURE,
        /** Test passed */
        PASSED,
        /** Test started but not ended */
        INCOMPLETE,
        /** Test assumption failure */
        ASSUMPTION_FAILURE,
        /** Test ignored */
        IGNORED,
    }

    private TestStatus mStatus;
    private String mStackTrace;
    private Map<String, String> mMetrics;
    // the start and end time of the test, measured via {@link System#currentTimeMillis()}
    private long mStartTime = 0;
    private long mEndTime = 0;

    public TestResult() {
        mStatus = TestStatus.INCOMPLETE;
        mStartTime = System.currentTimeMillis();
    }

    /**
     * Get the {@link TestStatus} result of the test.
     */
    public TestStatus getStatus() {
        return mStatus;
    }

    /**
     * Get the associated {@link String} stack trace. Should be <code>null</code> if
     * {@link #getStatus()} is {@link TestStatus.PASSED}.
     */
    public String getStackTrace() {
        return mStackTrace;
    }

    /**
     * Get the associated test metrics.
     */
    public Map<String, String> getMetrics() {
        return mMetrics;
    }

    /**
     * Set the test metrics, overriding any previous values.
     */
    public void setMetrics(Map<String, String> metrics) {
        mMetrics = metrics;
    }

    /**
     * Return the {@link System#currentTimeMillis()} time that the
     * {@link ITestInvocationListener#testStarted(TestIdentifier)} event was received.
     */
    public long getStartTime() {
        return mStartTime;
    }

    /**
     * Return the {@link System#currentTimeMillis()} time that the
     * {@link ITestInvocationListener#testEnded(TestIdentifier)} event was received.
     */
    public long getEndTime() {
        return mEndTime;
    }

    /**
     * Set the {@link TestStatus}.
     */
    public TestResult setStatus(TestStatus status) {
       mStatus = status;
       return this;
    }

    /**
     * Set the stack trace.
     */
    public void setStackTrace(String trace) {
        mStackTrace = trace;
    }

    /**
     * Sets the end time
     */
    public void setEndTime(long currentTimeMillis) {
        mEndTime = currentTimeMillis;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {mMetrics, mStackTrace, mStatus});
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TestResult other = (TestResult) obj;
        return equal(mMetrics, other.mMetrics) &&
               equal(mStackTrace, other.mStackTrace) &&
               equal(mStatus, other.mStatus);
    }

    private static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
