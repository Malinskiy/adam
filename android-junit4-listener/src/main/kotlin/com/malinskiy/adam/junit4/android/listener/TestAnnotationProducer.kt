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
 * INSTRUMENTATION_STATUS: com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v2=[androidx.test.filters.SmallTest(), io.qameta.allure.kotlin.Severity(value=critical), io.qameta.allure.kotlin.Story(value=Slow), org.junit.Test(expected=class org.junit.Test$None:timeout=0), io.qameta.allure.kotlin.Owner(value=user2), io.qameta.allure.kotlin.Feature(value=Text on main screen), io.qameta.allure.kotlin.Epic(value=General), org.junit.runner.RunWith(value=class io.qameta.allure.android.runners.AllureAndroidJUnit4), kotlin.Metadata(bytecodeVersion=[I@bdf6b25:data1=[Ljava.lang.String;@46414fa:data2=[Ljava.lang.String;@5d4aab:extraInt=0:extraString=:kind=1:metadataVersion=[I@fbb1508:packageName=), io.qameta.allure.kotlin.Severity(value=critical), io.qameta.allure.kotlin.Story(value=Slow)]
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
            val annotations: List<String> =
                (description.annotations.toList() + description.testClass.annotations.toList()).mapNotNull { annotation ->
                    val fqn = annotation.annotationClass.qualifiedName
                    val parameters =
                        annotation.annotationClass.memberProperties.joinToString(separator = ":") { "${it.name}=${it.getter.call(annotation)}" }
                    "$fqn($parameters)"
                }
            val bundle = Bundle(1)
            bundle.putStringArrayList(
                "com.malinskiy.adam.junit4.android.listener.TestAnnotationProducer.v2",
                ArrayList(annotations)
            )
            InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
        }
    }
}
