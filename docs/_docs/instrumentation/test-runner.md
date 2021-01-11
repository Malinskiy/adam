---
layout: default
title:  "Test runner (am instrument)"
nav_order: 6
---

## Test runner request
{: .no_toc }

1. TOC
{:toc}


Executing tests can be done using the `TestRunnerRequest`:

```kotlin
val channel = adb.execute(
                    request = TestRunnerRequest(
                                  testPackage = "com.example.test",
                                  instrumentOptions = InstrumentOptions(
                                      clazz = listOf("com.example.MyTest")
                                  )
                              ),
                    scope = GlobalScope,
                    serial = "emulator-5554"
                )

var logPart: String? = null
do {
    logPart?.let { print(it) }
    logPart = channel.receiveOrNull()
} while (logPart != null)
```

The output is a `ReadChannel<String>` that returns the output of the `am instrument` command.

### Required parameters
To execute tests you have to provide the `testPackage` and default `InstrumentOptions()`

### Runner class
Default test runner class is `android.support.test.runner.AndroidJUnitRunner` but can be changed using the `runnerClass` option. 

For all the options check the
 [source](https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Am.java#155) of the `am`
  command

### No hidden api checks
Disables restrictions on the use of hidden APIs

### No window animation
Turn off window animations while running

### User ID
Specify user instrumentation runs in. Defaults to current user if not specified"

### ABI

### Profiling output
Write profiling data to specified path on the device

### Output log path
Write test log to specified path  
  
## Instrument options

### pkg 
The fully-qualified Java package name for one of the packages in the test application.
Any test case class that uses this package name is executed.
Notice that this is not an Android package name;
a test package has a single Android package name but may have several Java packages within it.

### clazz
The fully-qualified Java class name for one of the test case classes. Only this test case class is executed.

or

`<class_name>#method name`.  A fully-qualified test case class name, and one of its methods.
Only this method is executed.
Note the hash mark (#) between the class name and the method name.


### functional 
Runs all test classes that extend `InstrumentationTestCase`.

### unit 
Runs all test classes that do not extend either `InstrumentationTestCase` or `PerformanceTestCase`.

### filterSize
Runs a test method annotated by size. The annotations are `@SmallTest`, `@MediumTest`, and `@LargeTest`.

### performance
Runs all test classes that implement `PerformanceTestCase`.

### debug 
Runs tests in debug mode.

### log 
Loads and logs all specified tests, but does not run them.

The test information appears in STDOUT. Use this to verify combinations of other filters and test specifications.

### emma
Runs an EMMA code coverage analysis and writes the output to /data/<app_package>/coverage.ec on the device.

To override the file location, use the [coverageFile] key that is described in the following entry.

### coverageFile
Overrides the default location of the EMMA coverage file on the device.
Specify this value as a path and filename in UNIX format. The default filename is described in the entry for the [emma] key.

## Parsing the output
To understand what's happening with the tests you will need to parse the output of the `am instrument`. To help with this you can use the
 `InstrumentationResponseTransformer`:

```kotlin
val transformer = InstrumentationResponseTransformer()
val channel = adb.execute(testRunnerRequest)

var logPart: String? = null
do {
    logPart?.let {
        for (line in it.lines()) {
            val bytes = ((line + '\n') as java.lang.String).getBytes(Charset.forName("ISO-8859-1"))
            transformer.process(bytes, 0, bytes.size)
            transformer.transform()?.let { events: List<TestEvent> ->
                //Do something with the events
            }
        }
    }
    withTimeout(180_000) {
        logPart = channel.receiveOrNull()
    }
} while (logPart != null)

transformer.close()?.let { events: List<TestEvent> ->
    //This is important because test run can abruptly finish and not report back the finish
    //Handle the events
}
```