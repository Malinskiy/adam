object Versions {
    val adam = System.getenv("GIT_TAG_NAME") ?: "0.3.2"
    val kotlin = "1.4.32"
    val coroutines = "1.4.3"
    val coroutinesDebug = coroutines


    val annotations = "21.0.1"
    val ktor = "1.5.2"
    val logging = "2.0.8"
    val vertx = "4.0.3"

    val assertk = "0.24"
    val junit4 = "4.13.2"
    val junit5 = "5.7.2"
    val junit5commons = "1.7.2"
    val imageComparison = "4.4.0"
    val dokka = kotlin

    val grpc = "1.32.1"
    val grpcKotlin = "1.0.0"
    val grpcOkhttp = "1.38.0"
    val protobufGradle = "0.8.16"
    val protobuf = "3.17.2"
    val javax = "1.3.2"

    val androidGradle = "4.2.0"
    val testMonitor = "1.3.0"
    val gradleVersionsPlugin = "0.39.0"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val androidGradle = "com.android.tools.build:gradle:${Versions.androidGradle}"
}

object Libraries {
    val annotations = "org.jetbrains:annotations:${Versions.annotations}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val ktorNetwork = "io.ktor:ktor-network-jvm:${Versions.ktor}"
    val logging = "io.github.microutils:kotlin-logging:${Versions.logging}"
    val vertxCore = "io.vertx:vertx-core:${Versions.vertx}"
    val vertxKotlin = "io.vertx:vertx-lang-kotlin:${Versions.vertx}"
    val vertxCoroutines = "io.vertx:vertx-lang-kotlin-coroutines:${Versions.vertx}"
    val protobufLite = "com.google.protobuf:protobuf-javalite:${Versions.protobuf}"
    val grpcKotlinStubLite = "io.grpc:grpc-kotlin-stub-lite:${Versions.grpcKotlin}"
    val grpcOkhttp = "io.grpc:grpc-okhttp:${Versions.grpcOkhttp}"
    val javaxAnnotations = "javax.annotation:javax.annotation-api:${Versions.javax}"
}

object AndroidX {
    val testMonitor = "androidx.test:monitor:${Versions.testMonitor}@aar"
}

object TestLibraries {
    val assertk = "com.willowtreeapps.assertk:assertk:${Versions.assertk}"
    val junit4 = "junit:junit:${Versions.junit4}"
    val junit5 = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    val junit5commons = "org.junit.platform:junit-platform-commons:${Versions.junit5commons}"
    val imageComparison = "com.github.romankh3:image-comparison:${Versions.imageComparison}"
    val coroutinesDebug = "org.jetbrains.kotlinx:kotlinx-coroutines-debug:${Versions.coroutinesDebug}"
}
