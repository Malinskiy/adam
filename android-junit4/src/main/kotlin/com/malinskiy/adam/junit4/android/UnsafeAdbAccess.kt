package com.malinskiy.adam.junit4.android

@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This implementation of adb is unsafe: unrestricted access to adb can introduce side-effects as well as unpredictable behaviour. " +
            "Its usage should be marked with '@com.malinskiy.adam.junit4.android.UnsafeAdbAccess' or '@OptIn(com.malinskiy.adam.junit4.android.UnsafeAdbAccess::class)' " +
            "if you accept the potential risks"
)
annotation class UnsafeAdbAccess
