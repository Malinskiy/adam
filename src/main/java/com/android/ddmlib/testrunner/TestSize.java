/*
 * Copyright (C) 2019 Anton Malinskiy
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

public enum TestSize {
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
