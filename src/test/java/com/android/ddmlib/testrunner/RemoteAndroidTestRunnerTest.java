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

import com.android.ddmlib.IShellEnabledDevice;
import com.android.ddmlib.IShellOutputReceiver;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link RemoteAndroidTestRunner}.
 */
public class RemoteAndroidTestRunnerTest extends TestCase {

    private RemoteAndroidTestRunner mRunner;
    private IShellEnabledDevice mMockDevice;
    private ITestRunListener mMockListener;

    private static final String TEST_PACKAGE = "com.test";
    private static final String TEST_RUNNER = "com.test.InstrumentationTestRunner";

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        mMockDevice = EasyMock.createMock(IShellEnabledDevice.class);
        EasyMock.expect(mMockDevice.getName()).andStubReturn("serial");
        mMockListener = EasyMock.createNiceMock(ITestRunListener.class);
        mRunner = new RemoteAndroidTestRunner(TEST_PACKAGE, TEST_RUNNER, mMockDevice);
    }

    /**
     * Test the basic case building of the instrumentation runner command with no arguments.
     */
    public void testRun() throws Exception {
        String expectedCmd = EasyMock.eq(String.format("am instrument -w -r   %s/%s", TEST_PACKAGE,
                TEST_RUNNER));
        runAndVerify(expectedCmd);
    }

    /**
     * Test the building of the instrumentation runner command with log set.
     */
    public void testRun_withLog() throws Exception {
        mRunner.setLogOnly(true);
        String expectedCmd = EasyMock.contains("-e log true");
        runAndVerify(expectedCmd);
    }

    /**
     * Test the building of the instrumentation runner command with method set.
     */
    public void testRun_withMethod() throws Exception {
        final String className = "FooTest";
        final String testName = "fooTest";
        mRunner.setMethodName(className, testName);
        String expectedCmd = EasyMock.contains(String.format("-e class %s#%s", className,
                testName));
        runAndVerify(expectedCmd);
    }

    /**
     * Test the building of the instrumentation runner command with test package set.
     */
    public void testRun_withPackage() throws Exception {
        final String packageName = "foo.test";
        mRunner.setTestPackageName(packageName);
        String expectedCmd = EasyMock.contains(String.format("-e package %s", packageName));
        runAndVerify(expectedCmd);
    }

    /**
     * Test the building of the instrumentation runner command with extra argument added.
     */
    public void testRun_withAddInstrumentationArg() throws Exception {
        final String extraArgName = "blah";
        final String extraArgValue = "blahValue";
        mRunner.addInstrumentationArg(extraArgName, extraArgValue);
        String expectedCmd = EasyMock.contains(String.format("-e %s %s", extraArgName,
                extraArgValue));
        runAndVerify(expectedCmd);
    }

    /**
     * Test additional run options.
     */
    public void testRun_runOptions() throws Exception {
        mRunner.setRunOptions("--no-window-animation");
        String expectedCmd =
                EasyMock.eq(
                        String.format(
                                "am instrument -w -r --no-window-animation  %s/%s",
                                TEST_PACKAGE,
                                TEST_RUNNER));
        runAndVerify(expectedCmd);
    }

    /**
     * Test run when the device throws a IOException
     */
    @SuppressWarnings("unchecked")
    public void testRun_ioException() throws Exception {
        mMockDevice.executeShellCommand((String)EasyMock.anyObject(), (IShellOutputReceiver)
                EasyMock.anyObject(), EasyMock.eq(0L), EasyMock.eq(TimeUnit.MILLISECONDS));
        EasyMock.expectLastCall().andThrow(new IOException());
        // verify that the listeners run started, run failure, and run ended methods are called
        mMockListener.testRunStarted(TEST_PACKAGE, 0);
        mMockListener.testRunFailed((String)EasyMock.anyObject());
        mMockListener.testRunEnded(EasyMock.anyLong(), EasyMock.eq(Collections.EMPTY_MAP));

        EasyMock.replay(mMockDevice, mMockListener);
        try {
            mRunner.run(mMockListener);
            fail("IOException not thrown");
        } catch (IOException e) {
            // expected
        }
        EasyMock.verify(mMockDevice, mMockListener);
    }

    /**
     * Calls {@link RemoteAndroidTestRunner#run(ITestRunListener...)} and verifies the given
     * <var>expectedCmd</var> pattern was received by the mock device.
     */
    private void runAndVerify(String expectedCmd) throws Exception {
        mMockDevice.executeShellCommand(expectedCmd, (IShellOutputReceiver)
                EasyMock.anyObject(), EasyMock.eq(0L), EasyMock.eq(TimeUnit.MILLISECONDS));
        EasyMock.replay(mMockDevice);
        mRunner.run(mMockListener);
        EasyMock.verify(mMockDevice);
    }
}
