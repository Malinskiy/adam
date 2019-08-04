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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Interface for running a Android test command remotely and reporting result to a listener.
 */
public interface IRemoteAndroidTestRunner {

    enum TestSize {
        /** Run tests annotated with SmallTest */
        SMALL("small"),
        /** Run tests annotated with MediumTest */
        MEDIUM("medium"),
        /** Run tests annotated with LargeTest */
        LARGE("large");

        private String mRunnerValue;

        /**
         * Create a {@link TestSize}.
         *
         * @param runnerValue the {@link String} value that represents the size that is passed to
         * device. Defined on device in android.test.InstrumentationTestRunner.
         */
        TestSize(String runnerValue) {
            mRunnerValue = runnerValue;
        }

        String getRunnerValue() {
            return mRunnerValue;
        }

        /**
         * Return the {@link TestSize} corresponding to the given Android platform defined value.
         *
         * @throws IllegalArgumentException if {@link TestSize} cannot be found.
         */
        public static TestSize getTestSize(String value) {
            // build the error message in the success case too, to avoid two for loops
            StringBuilder msgBuilder = new StringBuilder("Unknown TestSize ");
            msgBuilder.append(value);
            msgBuilder.append(", Must be one of ");
            for (TestSize size : values()) {
                if (size.getRunnerValue().equals(value)) {
                    return size;
                }
                msgBuilder.append(size.getRunnerValue());
                msgBuilder.append(", ");
            }
            throw new IllegalArgumentException(msgBuilder.toString());
        }
    }

    /**
     * Returns the application package name.
     */
    String getPackageName();

    /**
     * Returns the runnerName.
     */
    String getRunnerName();

    /**
     * Sets to run only tests in this class
     * Must be called before 'run'.
     *
     * @param className fully qualified class name (eg x.y.z)
     */
    void setClassName(String className);

    /**
     * Sets to run only tests in the provided classes
     * Must be called before 'run'.
     * <p>
     * If providing more than one class, requires a InstrumentationTestRunner that supports
     * the multiple class argument syntax.
     *
     * @param classNames array of fully qualified class names (eg x.y.z)
     */
    void setClassNames(String[] classNames);

    /**
     * Sets to run only specified test method
     * Must be called before 'run'.
     *
     * @param className fully qualified class name (eg x.y.z)
     * @param testName method name
     */
    void setMethodName(String className, String testName);

    /**
     * Sets to run all tests in specified package
     * Must be called before 'run'.
     *
     * @param packageName fully qualified package name (eg x.y.z)
     */
    void setTestPackageName(String packageName);

    /**
     * Sets to run only tests of given size.
     * Must be called before 'run'.
     *
     * @param size the {@link TestSize} to run.
     */
    void setTestSize(TestSize size);

    /**
     * Adds a argument to include in instrumentation command.
     * <p/>
     * Must be called before 'run'. If an argument with given name has already been provided, it's
     * value will be overridden.
     *
     * @param name the name of the instrumentation bundle argument
     * @param value the value of the argument
     */
    void addInstrumentationArg(String name, String value);

    /**
     * Removes a previously added argument.
     *
     * @param name the name of the instrumentation bundle argument to remove
     */
    void removeInstrumentationArg(String name);

    /**
     * Adds a boolean argument to include in instrumentation command.
     * <p/>
     * @see RemoteAndroidTestRunner#addInstrumentationArg
     *
     * @param name the name of the instrumentation bundle argument
     * @param value the value of the argument
     */
    void addBooleanArg(String name, boolean value);

    /**
     * Sets this test run to log only mode - skips test execution.
     */
    void setLogOnly(boolean logOnly);

    /**
     * Sets this debug mode of this test run. If true, the Android test runner will wait for a
     * debugger to attach before proceeding with test execution.
     */
    void setDebug(boolean debug);

    /**
     * Sets this code coverage mode of this test run.
     */
    void setCoverage(boolean coverage);

    /**
     * Sets this test run to test collection mode. If true, will skip test execution and will set
     * all appropriate runner arguments required for a successful test collection.
     */
    void setTestCollection(boolean collection);

    /**
     * @deprecated Use {@link #setMaxTimeToOutputResponse(long, java.util.concurrent.TimeUnit)}.
     */
    @Deprecated
    void setMaxtimeToOutputResponse(int maxTimeToOutputResponse);

    /**
     * Sets the maximum time allowed between output of the shell command running the tests on
     * the devices.
     * <p/>
     * This allows setting a timeout in case the tests can become stuck and never finish. This is
     * different from the normal timeout on the connection.
     * <p/>
     * By default no timeout will be specified.
     *
     * @param maxTimeToOutputResponse the maximum amount of time during which the command is allowed
     *            to not output any response. A value of 0 means the method will wait forever
     *            (until the <var>receiver</var> cancels the execution) for command output and
     *            never throw.
     * @param maxTimeUnits Units for non-zero {@code maxTimeToOutputResponse} values.
     *
     * @see IDevice#executeShellCommand(String, com.android.ddmlib.IShellOutputReceiver, int)
     */
    void setMaxTimeToOutputResponse(long maxTimeToOutputResponse, TimeUnit maxTimeUnits);

    /**
     * Set a custom run name to be reported to the {@link ITestRunListener} on {@link #run}
     * <p/>
     * If unspecified, will use package name
     *
     * @param runName
     */
    void setRunName(String runName);

    /**
     * Execute this test run.
     * <p/>
     * Convenience method for {@link #run(Collection)}.
     *
     * @param listeners listens for test results
     * @throws TimeoutException in case of a timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws ShellCommandUnresponsiveException if the device did not output any test result for
     * a period longer than the max time to output.
     * @throws IOException if connection to device was lost.
     *
     * @see #setMaxtimeToOutputResponse(int)
     */
    void run(ITestRunListener... listeners)
            throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException,
            IOException;

    /**
     * Execute this test run.
     *
     * @param listeners collection of listeners for test results
     * @throws TimeoutException in case of a timeout on the connection.
     * @throws AdbCommandRejectedException if adb rejects the command
     * @throws ShellCommandUnresponsiveException if the device did not output any test result for
     * a period longer than the max time to output.
     * @throws IOException if connection to device was lost.
     *
     * @see #setMaxtimeToOutputResponse(int)
     */
    void run(Collection<ITestRunListener> listeners)
            throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException,
            IOException;

    /**
     * Requests cancellation of this test run.
     */
    void cancel();

}
