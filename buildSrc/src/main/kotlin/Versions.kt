object Versions {
    val adam = System.getenv("GIT_TAG_NAME") ?: "0.5.5"
    val kotlin = "1.9.10"
    val coroutines = "1.7.3"
    val coroutinesDebug = coroutines

    val annotations = "24.0.1"
    val ktor = "2.3.6"
    val logging = "3.0.5"
    val vertx = "4.4.6"
    val apacheCommonsPool2 = "2.11.1"

    val assertk = "0.27.0"
    val junit4 = "4.13.2"
    val junit5 = "5.10.1"
    val junit5commons = "1.10.1"
    val imageComparison = "4.4.0"
    val dokka = kotlin

    val grpc = "1.59.0"
    val grpcKotlin = "1.4.0"
    val grpcOkhttp = "1.59.0"
    val protobufGradle = "0.9.4"
    val protobuf = "3.25.0"
    val javax = "1.3.2"

    val androidGradle = "8.1.3"
    val testMonitor = "1.6.1"
    val testRunner = "1.5.2"
    val gradleVersionsPlugin = "0.49.0"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val androidGradle = "com.android.tools.build:gradle:${Versions.androidGradle}"
}

object Libraries {
    val annotations = "org.jetbrains:annotations:${Versions.annotations}"
    val apacheCommonsPool2 = "org.apache.commons:commons-pool2:${Versions.apacheCommonsPool2}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val ktorNetwork = "io.ktor:ktor-network-jvm:${Versions.ktor}"
    val logging = "io.github.microutils:kotlin-logging:${Versions.logging}"
    val vertxCore = "io.vertx:vertx-core:${Versions.vertx}"
    val vertxKotlin = "io.vertx:vertx-lang-kotlin:${Versions.vertx}"
    val vertxCoroutines = "io.vertx:vertx-lang-kotlin-coroutines:${Versions.vertx}"
    val protobufLite = "com.google.protobuf:protobuf-javalite:${Versions.protobuf}"
    val grpcStub = "io.grpc:grpc-stub:${Versions.grpc}"
    val grpcKotlinStub = "io.grpc:grpc-kotlin-stub:${Versions.grpcKotlin}"
    val grpcProtobufLite = "io.grpc:grpc-protobuf-lite:${Versions.grpc}"
    val grpcOkhttp = "io.grpc:grpc-okhttp:${Versions.grpcOkhttp}"
    val javaxAnnotations = "javax.annotation:javax.annotation-api:${Versions.javax}"
}

object AndroidX {
    val testMonitor = "androidx.test:monitor:${Versions.testMonitor}@aar"
    val androidXScreenshot = "androidx.test:runner:${Versions.testRunner}@aar"
}

object TestLibraries {
    val assertk = "com.willowtreeapps.assertk:assertk:${Versions.assertk}"
    val junit4 = "junit:junit:${Versions.junit4}"
    val junit5 = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    val junit5commons = "org.junit.platform:junit-platform-commons:${Versions.junit5commons}"
    val imageComparison = "com.github.romankh3:image-comparison:${Versions.imageComparison}"
    val coroutinesDebug = "org.jetbrains.kotlinx:kotlinx-coroutines-debug:${Versions.coroutinesDebug}"
}
