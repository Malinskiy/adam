/*
 * Copyright (C) 2021 Anton Malinskiy
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

package com.malinskiy.adam.junit4.android.listener

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.notification.RunListener
import kotlin.reflect.full.memberProperties

/**
 * JUnit4 listener that produces test annotations, e.g. using am instrument:
 * INSTRUMENTATION_STATUS_CODE: 0
 * INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
 * INSTRUMENTATION_STATUS: current=4
 * INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
 * INSTRUMENTATION_STATUS: numtests=39
 * INSTRUMENTATION_STATUS: stream=
 * com.example.FailedAssumptionTest:
 * INSTRUMENTATION_STATUS: test=ignoreTest
 * INSTRUMENTATION_STATUS_CODE: 1
 * INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v4=[64Lorg.junit.Test(34Lexpected=class org.junit.Test$None9Ltimeout=0), 46Lio.qameta.allure.kotlin.Epic(13Lvalue=General), 93Lorg.junit.runner.RunWith(64Lvalue=class io.qameta.allure.android.runners.AllureAndroidJUnit4), 65Lio.qameta.allure.kotlin.Feature(29Lvalue=Graphics on main screen), 45Lio.qameta.allure.kotlin.Owner(11Lvalue=user2), 44Lio.qameta.allure.kotlin.Story(10Lvalue=Slow), 51Lio.qameta.allure.kotlin.Severity(14Lvalue=critical), 197Lkotlin.Metadata(26LbytecodeVersion=[I@199c8a631Ldata1=[Ljava.lang.String;@132e733Ldata2=[Ljava.lang.String;@3af3e9411LextraInt=4812LextraString=6Lkind=126LmetadataVersion=[I@126ed3d12LpackageName=)]
 * INSTRUMENTATION_STATUS_CODE: 2
 * INSTRUMENTATION_STATUS: class=com.example.FailedAssumptionTest
 * INSTRUMENTATION_STATUS: current=4
 * INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
 * INSTRUMENTATION_STATUS: numtests=39
 * INSTRUMENTATION_STATUS: stream=.
 * INSTRUMENTATION_STATUS: test=ignoreTest
 */
class TestAnnotationProducer : RunListener() {
    override fun testStarted(description: Description?) {
        super.testStarted(description)
        if (description?.isTest == true) {
            val annotations: Set<String> =
                (description.annotations.toList() + description.testClass.annotations.toList()).mapNotNull { annotation ->
                    val fqn = annotation.annotationClass.qualifiedName
                    val parameters =
                        annotation.annotationClass.memberProperties.joinToString("") {
                            val serialized = "${it.name}=${it.getter.call(annotation)}"
                            "${serialized.length}L$serialized"
                        }
                    val annotation = "$fqn($parameters)"
                    "${annotation.length}L$annotation"
                }.toSet()
            val bundle = Bundle(1)
            bundle.putStringArrayList(
                "com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v4",
                ArrayList(annotations)
            )
            InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
        }
    }
}
