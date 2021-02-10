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

package com.malinskiy.adam.request.testrunner

/**
 * @param pkg The fully-qualified Java package name for one of the packages in the test application.
 * Any test case class that uses this package name is executed.
 * Notice that this is not an Android package name;
 * a test package has a single Android package name but may have several Java packages within it.
 *
 * @param clazz <class_name>
 *     The fully-qualified Java class name for one of the test case classes. Only this test case class is executed.
 *
 * or
 *
 * <class_name>#method name
 *      A fully-qualified test case class name, and one of its methods.
 *      Only this method is executed.
 *      Note the hash mark (#) between the class name and the method name.
 *
 *  @param functional Runs all test classes that extend InstrumentationTestCase.
 *
 *  @param unit Runs all test classes that do not extend either InstrumentationTestCase or PerformanceTestCase.
 *
 *  @param filterSize Runs a test method annotated by size. The annotations are @SmallTest, @MediumTest, and @LargeTest.
 *
 *  @param performance Runs all test classes that implement PerformanceTestCase.
 *
 *  @param debug Runs tests in debug mode.
 *
 *  @param log Loads and logs all specified tests, but does not run them.
 *  The test information appears in STDOUT. Use this to verify combinations of other filters and test specifications.
 *
 *  @param emma Runs an EMMA code coverage analysis and writes the output to /data/<app_package>/coverage.ec on the device.
 *  To override the file location, use the [coverageFile] key that is described in the following entry.
 *
 *  @param coverageFile Overrides the default location of the EMMA coverage file on the device.
 *  Specify this value as a path and filename in UNIX format. The default filename is described in the entry for the [emma] key.
 */
data class InstrumentOptions(
    val pkg: List<String> = emptyList(),
    val clazz: List<String> = emptyList(),
    val functional: Boolean? = null,
    val unit: Boolean? = null,
    val filterSize: InstrumentationSizeOption? = null,
    val performance: Boolean? = null,
    val debug: Boolean? = null,
    val log: Boolean? = null,
    val emma: Boolean? = null,
    val coverageFile: String? = null,
    val overrides: Map<String, String> = mapOf()
) {
    override fun toString() = StringBuilder().apply {
        if (pkg.isNotEmpty()) append(" -e package " + pkg.joinToString(separator = ","))
        if (clazz.isNotEmpty()) append(" -e class " + clazz.joinToString(separator = ","))
        if (functional != null) append(" -e func $functional")
        if (unit != null) append(" -e unit $unit")
        if (filterSize != null) append(" -e size ${filterSize.name.toLowerCase()}")
        if (performance != null) append(" -e perf $performance")
        if (debug != null) append(" -e debug $debug")
        if (log != null) append(" -e log $log")
        if (emma != null) append(" -e emma $emma")
        if (coverageFile != null) append(" -e coverageFile $coverageFile")

        append(overrides.map { " -e ${it.key} ${it.value}" }.joinToString(separator = ""))
    }.toString()
}