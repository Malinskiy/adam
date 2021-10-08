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
 * INSTRUMENTATION_STATUS: annotations=[org.junit.Ignore, org.junit.Test, androidx.test.filters.SmallTest]
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
