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
INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v3=[io.qameta.allure.kotlin.TmsLink(29Lvalue=https://marathonlabs.io), org.junit.Test(34Lexpected=class org.junit.Test$None, 9Ltimeout=0), io.qameta.allure.kotlin.Story(10Lvalue=Slow), org.junit.runner.RunWith(64Lvalue=class io.qameta.allure.android.runners.AllureAndroidJUnit4), io.qameta.allure.kotlin.Owner(11Lvalue=user2), kotlin.Metadata(26LbytecodeVersion=[I@7817b26, 33Ldata1=[Ljava.lang.String;@b0b3367, 33Ldata2=[Ljava.lang.String;@6187514, 11LextraInt=48, 12LextraString=, 6Lkind=1, 26LmetadataVersion=[I@d8e21bd, 12LpackageName=), io.qameta.allure.kotlin.Epic(13Lvalue=General), io.qameta.allure.kotlin.Feature(25Lvalue=Text on main screen), io.qameta.allure.kotlin.Severity(14Lvalue=critical)]
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
