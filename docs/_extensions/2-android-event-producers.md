---
layout: default
title:  "Android test metric producers"
nav_order: 2
---

Adam provides several producers of test statuses that inform test runners about the test execution.

## TestAnnotationProducer

Artifact: `com.malinskiy.adam:android-junit4-test-annotation-producer:${LATEST_VERSION}`

This producer emits current test annotations as a metric, e.g.:

```
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=
com.example.FailedAssumptionTest:
INSTRUMENTATION_STATUS: test=ignoreTest
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v2=[androidx.test.filters.SmallTest(), io.qameta.allure.kotlin.Severity(value=critical), io.qameta.allure.kotlin.Story(value=Slow), org.junit.Test(expected=class org.junit.Test$None:timeout=0), io.qameta.allure.kotlin.Owner(value=user2), io.qameta.allure.kotlin.Feature(value=Text on main screen), io.qameta.allure.kotlin.Epic(value=General), org.junit.runner.RunWith(value=class io.qameta.allure.android.runners.AllureAndroidJUnit4), kotlin.Metadata(bytecodeVersion=[I@bdf6b25:data1=[Ljava.lang.String;@46414fa:data2=[Ljava.lang.String;@5d4aab:extraInt=0:extraString=:kind=1:metadataVersion=[I@fbb1508:packageName=), io.qameta.allure.kotlin.Severity(value=critical), io.qameta.allure.kotlin.Story(value=Slow)]
INSTRUMENTATION_STATUS_CODE: 2
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=ignoreTest
```

This is useful, for example, when bytecode analysis is undesirable in complexity and it's easier to use the Android OS itself to report back
the annotations of each test.

## AdamScreenCaptureProcessor

Artifact: `com.malinskiy.adam:androidx-screencapture:${LATEST_VERSION}`

This producer emits test screen captures that utilise `androidx.test.runner.screenshot`, e.g.:

```
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=
com.example.FailedAssumptionTest:
INSTRUMENTATION_STATUS: test=ignoreTest
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.screencapture.AdamScreenCaptureProcessor.v1=/sdcard/images/screenshot/screenshot-1.png
INSTRUMENTATION_STATUS_CODE: 2
INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
INSTRUMENTATION_STATUS: current=4
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: numtests=39
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=ignoreTest
```

This is useful to inform the test runner of the test screenshots and perhaps attach them to the test report.
