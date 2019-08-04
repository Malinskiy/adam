/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the 'raw output mode' results of an instrumentation test run from shell and informs a
 * ITestRunListener of the results.
 *
 * <p>Expects the following output:
 *
 * <p>If fatal error occurred when attempted to run the tests:
 * <pre>
 * INSTRUMENTATION_STATUS: Error=error Message
 * INSTRUMENTATION_FAILED:
 * </pre>
 * <p>or
 * <pre>
 * INSTRUMENTATION_RESULT: shortMsg=error Message
 * </pre>
 *
 * <p>Otherwise, expect a series of test results, each one containing a set of status key/value
 * pairs, delimited by a start(1)/pass(0)/fail(-2)/error(-1) status code result. At end of test
 * run, expects that the elapsed test time in seconds will be displayed
 *
 * <p>For example:
 * <pre>
 * INSTRUMENTATION_STATUS_CODE: 1
 * INSTRUMENTATION_STATUS: class=com.foo.FooTest
 * INSTRUMENTATION_STATUS: test=testFoo
 * INSTRUMENTATION_STATUS: numtests=2
 * INSTRUMENTATION_STATUS: stack=com.foo.FooTest#testFoo:312
 *    com.foo.X
 * INSTRUMENTATION_STATUS_CODE: -2
 * ...
 *
 * Time: X
 * </pre>
 * <p>Note that the "value" portion of the key-value pair may wrap over several text lines
 */
public class InstrumentationResultParser extends MultiLineReceiver {

    /** Relevant test status keys. */
    private static class StatusKeys {
        private static final String TEST = "test";
        private static final String CLASS = "class";
        private static final String STACK = "stack";
        private static final String NUMTESTS = "numtests";
        private static final String ERROR = "Error";
        private static final String SHORTMSG = "shortMsg";
    }

    /** The set of expected status keys. Used to filter which keys should be stored as metrics */
    private static final Set<String> KNOWN_KEYS = new HashSet<String>();
    static {
        KNOWN_KEYS.add(StatusKeys.TEST);
        KNOWN_KEYS.add(StatusKeys.CLASS);
        KNOWN_KEYS.add(StatusKeys.STACK);
        KNOWN_KEYS.add(StatusKeys.NUMTESTS);
        KNOWN_KEYS.add(StatusKeys.ERROR);
        KNOWN_KEYS.add(StatusKeys.SHORTMSG);
        // unused, but regularly occurring status keys.
        KNOWN_KEYS.add("stream");
        KNOWN_KEYS.add("id");
        KNOWN_KEYS.add("current");
    }

    /** Test result status codes. */
    private static class StatusCodes {
        private static final int START = 1;
        private static final int IN_PROGRESS = 2;

        // codes used for test completed
        private static final int ASSUMPTION_FAILURE = -4;
        private static final int IGNORED = -3;
        private static final int FAILURE = -2;
        private static final int ERROR = -1;
        private static final int OK = 0;
    }

    /** Prefixes used to identify output. */
    private static class Prefixes {
        private static final String STATUS = "INSTRUMENTATION_STATUS: ";
        private static final String STATUS_CODE = "INSTRUMENTATION_STATUS_CODE: ";
        private static final String STATUS_FAILED = "INSTRUMENTATION_FAILED: ";
        private static final String CODE = "INSTRUMENTATION_CODE: ";
        private static final String RESULT = "INSTRUMENTATION_RESULT: ";
        private static final String TIME_REPORT = "Time: ";
    }

    private final Collection<ITestRunListener> mTestListeners;

    /**
     * Test result data
     */
    private static class TestResult {
        private Integer mCode = null;
        private String mTestName = null;
        private String mTestClass = null;
        private String mStackTrace = null;
        private Integer mNumTests = null;

        /** Returns true if all expected values have been parsed */
        boolean isComplete() {
            return mCode != null && mTestName != null && mTestClass != null;
        }

        /** Provides a more user readable string for TestResult, if possible */
        @Override
        public String toString() {
            StringBuilder output = new StringBuilder();
            if (mTestClass != null ) {
                output.append(mTestClass);
                output.append('#');
            }
            if (mTestName != null) {
                output.append(mTestName);
            }
            if (output.length() > 0) {
                return output.toString();
            }
            return "unknown result";
        }
    }

    /** the name to provide to {@link ITestRunListener#testRunStarted(String, int)} */
    private final String mTestRunName;

    /** Stores the status values for the test result currently being parsed */
    private TestResult mCurrentTestResult = null;

    /** Stores the status values for the test result last parsed */
    private TestResult mLastTestResult = null;

    /** Stores the current "key" portion of the status key-value being parsed. */
    private String mCurrentKey = null;

    /** Stores the current "value" portion of the status key-value being parsed. */
    private StringBuilder mCurrentValue = null;

    /** True if start of test has already been reported to listener. */
    private boolean mTestStartReported = false;

    /** True if the completion of the test run has been detected. */
    private boolean mTestRunFinished = false;

    /** True if test run failure has already been reported to listener. */
    private boolean mTestRunFailReported = false;

    /** The elapsed time of the test run, in milliseconds. */
    private long mTestTime = 0;

    /** True if current test run has been canceled by user. */
    private boolean mIsCancelled = false;

    /** The number of tests currently run  */
    private int mNumTestsRun = 0;

    /** The number of tests expected to run  */
    private int mNumTestsExpected = 0;

    /** True if the parser is parsing a line beginning with "INSTRUMENTATION_RESULT" */
    private boolean mInInstrumentationResultKey = false;

    /**
     * Stores key-value pairs under INSTRUMENTATION_RESULT header, these are printed at the
     * end of a test run, if applicable
     */
    private Map<String, String> mInstrumentationResultBundle = new HashMap<String, String>();

    /**
     * Stores key-value pairs of metrics emitted during the execution of each test case.  Note that
     * standard keys that are stored in the TestResults class are filtered out of this Map.
     */
    private Map<String, String> mTestMetrics = new HashMap<String, String>();

    private static final String LOG_TAG = "InstrumentationResultParser";

    /** Error message supplied when no parseable test results are received from test run. */
    static final String NO_TEST_RESULTS_MSG = "No test results";

    /** Error message supplied when a test start bundle is parsed, but not the test end bundle. */
    static final String INCOMPLETE_TEST_ERR_MSG_PREFIX = "Test failed to run to completion";
    static final String INCOMPLETE_TEST_ERR_MSG_POSTFIX = "Check device logcat for details";

    /** Error message supplied when the test run is incomplete. */
    static final String INCOMPLETE_RUN_ERR_MSG_PREFIX = "Test run failed to complete";

    /**
     * Creates the InstrumentationResultParser.
     *
     * @param runName the test run name to provide to
     *            {@link ITestRunListener#testRunStarted(String, int)}
     * @param listeners informed of test results as the tests are executing
     */
    public InstrumentationResultParser(String runName, Collection<ITestRunListener> listeners) {
        mTestRunName = runName;
        mTestListeners = new ArrayList<ITestRunListener>(listeners);
    }

    /**
     * Creates the InstrumentationResultParser for a single listener.
     *
     * @param runName the test run name to provide to
     *            {@link ITestRunListener#testRunStarted(String, int)}
     * @param listener informed of test results as the tests are executing
     */
    public InstrumentationResultParser(String runName, ITestRunListener listener) {
        this(runName, Collections.singletonList(listener));
    }

    /**
     * Processes the instrumentation test output from shell.
     *
     * @see MultiLineReceiver#processNewLines
     */
    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            parse(line);
            // in verbose mode, dump all adb output to log
            Log.v(LOG_TAG, line);
        }
    }

    /**
     * Parse an individual output line. Expects a line that is one of:
     * <ul>
     * <li>
     * The start of a new status line (starts with Prefixes.STATUS or Prefixes.STATUS_CODE),
     * and thus there is a new key=value pair to parse, and the previous key-value pair is
     * finished.
     * </li>
     * <li>
     * A continuation of the previous status (the "value" portion of the key has wrapped
     * to the next line).
     * </li>
     * <li> A line reporting a fatal error in the test run (Prefixes.STATUS_FAILED) </li>
     * <li> A line reporting the total elapsed time of the test run. (Prefixes.TIME_REPORT) </li>
     * </ul>
     *
     * @param line  Text output line
     */
    private void parse(String line) {
        if (line.startsWith(Prefixes.STATUS_CODE)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            mInInstrumentationResultKey = false;
            parseStatusCode(line);
        } else if (line.startsWith(Prefixes.STATUS)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            mInInstrumentationResultKey = false;
            parseKey(line, Prefixes.STATUS.length());
        } else if (line.startsWith(Prefixes.RESULT)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            mInInstrumentationResultKey = true;
            parseKey(line, Prefixes.RESULT.length());
        } else if (line.startsWith(Prefixes.STATUS_FAILED) ||
                   line.startsWith(Prefixes.CODE)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            mInInstrumentationResultKey = false;
            // these codes signal the end of the instrumentation run
            mTestRunFinished = true;
            // just ignore the remaining data on this line
        } else if (line.startsWith(Prefixes.TIME_REPORT)) {
            parseTime(line);
        } else {
            if (mCurrentValue != null) {
                // this is a value that has wrapped to next line.
                mCurrentValue.append("\r\n");
                mCurrentValue.append(line);
            } else if (!line.trim().isEmpty()) {
                Log.d(LOG_TAG, "unrecognized line " + line);
            }
        }
    }

    /**
     * Stores the currently parsed key-value pair in the appropriate place.
     */
    private void submitCurrentKeyValue() {
        if (mCurrentKey != null && mCurrentValue != null) {
            String statusValue = mCurrentValue.toString();
            if (mInInstrumentationResultKey) {
                if (!KNOWN_KEYS.contains(mCurrentKey)) {
                    mInstrumentationResultBundle.put(mCurrentKey, statusValue);
                } else if (mCurrentKey.equals(StatusKeys.SHORTMSG)) {
                    // test run must have failed
                    handleTestRunFailed(String.format("Instrumentation run failed due to '%1$s'",
                            statusValue));
                }
            } else {
                TestResult testInfo = getCurrentTestInfo();

                if (mCurrentKey.equals(StatusKeys.CLASS)) {
                    testInfo.mTestClass = statusValue.trim();
                } else if (mCurrentKey.equals(StatusKeys.TEST)) {
                    testInfo.mTestName = statusValue.trim();
                } else if (mCurrentKey.equals(StatusKeys.NUMTESTS)) {
                    try {
                        testInfo.mNumTests = Integer.parseInt(statusValue);
                    } catch (NumberFormatException e) {
                        Log.w(LOG_TAG, "Unexpected integer number of tests, received "
                                + statusValue);
                    }
                } else if (mCurrentKey.equals(StatusKeys.ERROR)) {
                    // test run must have failed
                    handleTestRunFailed(statusValue);
                } else if (mCurrentKey.equals(StatusKeys.STACK)) {
                    testInfo.mStackTrace = statusValue;
                } else if (!KNOWN_KEYS.contains(mCurrentKey)) {
                    // Not one of the recognized key/value pairs, so dump it in mTestMetrics
                    mTestMetrics.put(mCurrentKey, statusValue);
                }
            }

            mCurrentKey = null;
            mCurrentValue = null;
        }
    }

    /**
     * A utility method to return the test metrics from the current test case execution and get
     * ready for the next one.
     */
    private Map<String, String> getAndResetTestMetrics() {
        Map<String, String> retVal = mTestMetrics;
        mTestMetrics = new HashMap<String, String>();
        return retVal;
    }

    private TestResult getCurrentTestInfo() {
        if (mCurrentTestResult == null) {
            mCurrentTestResult = new TestResult();
        }
        return mCurrentTestResult;
    }

    private void clearCurrentTestInfo() {
        mLastTestResult = mCurrentTestResult;
        mCurrentTestResult = null;
    }

    /**
     * Parses the key from the current line.
     * Expects format of "key=value".
     *
     * @param line full line of text to parse
     * @param keyStartPos the starting position of the key in the given line
     */
    private void parseKey(String line, int keyStartPos) {
        int endKeyPos = line.indexOf('=', keyStartPos);
        if (endKeyPos != -1) {
            mCurrentKey = line.substring(keyStartPos, endKeyPos).trim();
            parseValue(line, endKeyPos + 1);
        }
    }

    /**
     * Parses the start of a key=value pair.
     *
     * @param line - full line of text to parse
     * @param valueStartPos - the starting position of the value in the given line
     */
    private void parseValue(String line, int valueStartPos) {
        mCurrentValue = new StringBuilder();
        mCurrentValue.append(line.substring(valueStartPos));
    }

    /**
     * Parses out a status code result.
     */
    private void parseStatusCode(String line) {
        String value = line.substring(Prefixes.STATUS_CODE.length()).trim();
        TestResult testInfo = getCurrentTestInfo();
        testInfo.mCode = StatusCodes.ERROR;
        try {
            testInfo.mCode = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.w(LOG_TAG, "Expected integer status code, received: " + value);
            testInfo.mCode = StatusCodes.ERROR;
        }
        if (testInfo.mCode != StatusCodes.IN_PROGRESS) {
            // this means we're done with current test result bundle
            reportResult(testInfo);
            clearCurrentTestInfo();
        }
    }

    /**
     * Returns true if test run canceled.
     *
     * @see IShellOutputReceiver#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Requests cancellation of test run.
     */
    public void cancel() {
        mIsCancelled = true;
    }

    /**
     * Reports a test result to the test run listener. Must be called when a individual test
     * result has been fully parsed.
     *
     * @param statusMap key-value status pairs of test result
     */
    private void reportResult(TestResult testInfo) {
        if (!testInfo.isComplete()) {
            Log.w(LOG_TAG, "invalid instrumentation status bundle " + testInfo.toString());
            return;
        }
        reportTestRunStarted(testInfo);
        TestIdentifier testId = new TestIdentifier(testInfo.mTestClass, testInfo.mTestName);
        Map<String, String> metrics;

        switch (testInfo.mCode) {
            case StatusCodes.START:
                for (ITestRunListener listener : mTestListeners) {
                    listener.testStarted(testId);
                }
                break;
            case StatusCodes.FAILURE:
                metrics = getAndResetTestMetrics();
                for (ITestRunListener listener : mTestListeners) {
                    listener.testFailed(testId, getTrace(testInfo));
                    listener.testEnded(testId, metrics);
                }
                mNumTestsRun++;
                break;
            case StatusCodes.ERROR:
                // we're dealing with a legacy JUnit3 runner that still reports errors.
                // just report this as a failure, since thats what upstream JUnit4 does
                metrics = getAndResetTestMetrics();
                for (ITestRunListener listener : mTestListeners) {
                    listener.testFailed(testId, getTrace(testInfo));
                    listener.testEnded(testId, metrics);
                }
                mNumTestsRun++;
                break;
            case StatusCodes.IGNORED:
                metrics = getAndResetTestMetrics();
                for (ITestRunListener listener : mTestListeners) {
                    listener.testStarted(testId);
                    listener.testIgnored(testId);
                    listener.testEnded(testId, metrics);
                }
                mNumTestsRun++;
                break;
            case StatusCodes.ASSUMPTION_FAILURE:
                metrics = getAndResetTestMetrics();
                for (ITestRunListener listener : mTestListeners) {
                    listener.testAssumptionFailure(testId, getTrace(testInfo));
                    listener.testEnded(testId, metrics);
                }
                mNumTestsRun++;
                break;
            case StatusCodes.OK:
                metrics = getAndResetTestMetrics();
                for (ITestRunListener listener : mTestListeners) {
                    listener.testEnded(testId, metrics);
                }
                mNumTestsRun++;
                break;
            default:
                metrics = getAndResetTestMetrics();
                Log.e(LOG_TAG, "Unknown status code received: " + testInfo.mCode);
                for (ITestRunListener listener : mTestListeners) {
                    listener.testEnded(testId, metrics);
                }
                mNumTestsRun++;
            break;
        }

    }

    /**
     * Reports the start of a test run, and the total test count, if it has not been previously
     * reported.
     *
     * @param testInfo current test status values
     */
    private void reportTestRunStarted(TestResult testInfo) {
        // if start test run not reported yet
        if (!mTestStartReported && testInfo.mNumTests != null) {
            for (ITestRunListener listener : mTestListeners) {
                listener.testRunStarted(mTestRunName, testInfo.mNumTests);
            }
            mNumTestsExpected = testInfo.mNumTests;
            mTestStartReported = true;
        }
    }

    /**
     * Returns the stack trace of the current failed test, from the provided testInfo.
     */
    private String getTrace(TestResult testInfo) {
        if (testInfo.mStackTrace != null) {
            return testInfo.mStackTrace;
        } else {
            Log.e(LOG_TAG, "Could not find stack trace for failed test ");
            return new Throwable("Unknown failure").toString();
        }
    }

    /**
     * Parses out and store the elapsed time.
     */
    private void parseTime(String line) {
        final Pattern timePattern = Pattern.compile(String.format("%s\\s*([\\d\\.]+)",
                Prefixes.TIME_REPORT));
        Matcher timeMatcher = timePattern.matcher(line);
        if (timeMatcher.find()) {
            String timeString = timeMatcher.group(1);
            try {
                float timeSeconds = Float.parseFloat(timeString);
                mTestTime = (long) (timeSeconds * 1000);
            } catch (NumberFormatException e) {
                Log.w(LOG_TAG, String.format("Unexpected time format %1$s", line));
            }
        } else {
            Log.w(LOG_TAG, String.format("Unexpected time format %1$s", line));
        }
    }

    /**
     * Inform the parser of a instrumentation run failure. Should be called when the adb command
     * used to run the test fails.
     */
    public void handleTestRunFailed(String errorMsg) {
        errorMsg = (errorMsg == null ? "Unknown error" : errorMsg);
        Log.i(LOG_TAG, String.format("test run failed: '%1$s'", errorMsg));
        if (mLastTestResult != null &&
            mLastTestResult.isComplete() &&
            StatusCodes.START == mLastTestResult.mCode) {

            // received test start msg, but not test complete
            // assume test caused this, report as test failure
            TestIdentifier testId = new TestIdentifier(mLastTestResult.mTestClass,
                    mLastTestResult.mTestName);
            for (ITestRunListener listener : mTestListeners) {
                listener.testFailed(testId,
                    String.format("%1$s. Reason: '%2$s'. %3$s", INCOMPLETE_TEST_ERR_MSG_PREFIX,
                            errorMsg, INCOMPLETE_TEST_ERR_MSG_POSTFIX));
                listener.testEnded(testId, getAndResetTestMetrics());
            }
        }
        for (ITestRunListener listener : mTestListeners) {
            if (!mTestStartReported) {
                // test run wasn't started - must have crashed before it started
                listener.testRunStarted(mTestRunName, 0);
            }
            listener.testRunFailed(errorMsg);
            listener.testRunEnded(mTestTime, mInstrumentationResultBundle);
        }
        mTestStartReported = true;
        mTestRunFailReported = true;
    }

    /**
     * Called by parent when adb session is complete.
     */
    @Override
    public void done() {
        super.done();
        if (!mTestRunFailReported) {
            handleOutputDone();
        }
    }

    /**
     * Handles the end of the adb session when a test run failure has not been reported yet
     */
    private void handleOutputDone() {
        if (!mTestStartReported && !mTestRunFinished) {
            // no results
            handleTestRunFailed(NO_TEST_RESULTS_MSG);
        } else if (mNumTestsExpected > mNumTestsRun) {
            final String message =
                String.format("%1$s. Expected %2$d tests, received %3$d",
                        INCOMPLETE_RUN_ERR_MSG_PREFIX, mNumTestsExpected, mNumTestsRun);
            handleTestRunFailed(message);
        } else {
            for (ITestRunListener listener : mTestListeners) {
                if (!mTestStartReported) {
                    // test run wasn't started, but it finished successfully. Must be a run with
                    // no tests
                    listener.testRunStarted(mTestRunName, 0);
                }
                listener.testRunEnded(mTestTime, mInstrumentationResultBundle);
            }
        }
    }
}
