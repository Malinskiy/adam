/*
 * Copyright (C) 2022 Anton Malinskiy
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

package com.malinskiy.adam.junit4.android.screencapture

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import java.io.File

/**
 * ScreenCaptureProcessor that emits screen capture paths as test status, e.g. using am instrument:
 * INSTRUMENTATION_STATUS_CODE: 0
 * INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
 * INSTRUMENTATION_STATUS: current=4
 * INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
 * INSTRUMENTATION_STATUS: numtests=39
 * INSTRUMENTATION_STATUS: stream=
 * com.example.FailedAssumptionTest:
 * INSTRUMENTATION_STATUS: test=ignoreTest
 * INSTRUMENTATION_STATUS_CODE: 1
 * INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.screencapture.AdamScreenCaptureProcessor.v1=/sdcard/images/screenshot/screenshot-1.png
 * INSTRUMENTATION_STATUS_CODE: 2
 * INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
 * INSTRUMENTATION_STATUS: current=4
 * INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
 * INSTRUMENTATION_STATUS: numtests=39
 * INSTRUMENTATION_STATUS: stream=.
 * INSTRUMENTATION_STATUS: test=ignoreTest
 */
class AdamScreenCaptureProcessor : BasicScreenCaptureProcessor() {
    override fun process(capture: ScreenCapture): String? {
        val filename = super.process(capture)
        val absoluteScreenCapturePath = File(mDefaultScreenshotPath, filename)

        val bundle = Bundle(1)
        bundle.putString(
            "com.malinskiy.adam.junit4.android.screencapture.AdamScreenCaptureProcessor.v1",
            absoluteScreenCapturePath.absolutePath
        )
        InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
        return filename
    }
}
