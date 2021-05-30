object Versions {
    val adam = System.getenv("GIT_TAG_NAME") ?: "0.3.0"
    val kotlin = "1.4.20"
    val coroutines = "1.3.9"

    val annotations = "16.0.2"
    val kxml = "2.3.0"
    val ktor = "1.5.2"
    val logging = "1.7.6"
    val vertx = "4.0.3"

    val assertk = "0.19"
    val junit4 = "4.13.2"
    val junit5 = "5.4.2"
    val junit5commons = "1.7.2"
    val imageComparison = "4.3.0"
    val dokka = "1.4.20"
    val coroutinesDebug = "1.4.0"

    val grpc = "1.32.1"
    val grpcKotlin = "1.0.0"
    val protobufGradle = "0.8.14"
    val protobuf = "3.14.0"
    val javax = "1.3.2"

    val androidGradle = "4.1.0"
    val testMonitor = "1.3.0"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val androidGradle = "com.android.tools.build:gradle:${Versions.androidGradle}"
}

object Libraries {
    val annotations = "org.jetbrains:annotations:${Versions.annotations}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val kxml = "net.sf.kxml:kxml2:${Versions.kxml}"
    val ktorNetwork = "io.ktor:ktor-network-jvm:${Versions.ktor}"
    val logging = "io.github.microutils:kotlin-logging:${Versions.logging}"
    val vertxCore = "io.vertx:vertx-core:${Versions.vertx}"
    val vertxKotlin = "io.vertx:vertx-lang-kotlin:${Versions.vertx}"
    val vertxCoroutines = "io.vertx:vertx-lang-kotlin-coroutines:${Versions.vertx}"
    val protobufLite = "com.google.protobuf:protobuf-javalite:${Versions.protobuf}"
    val grpcKotlinStubLite = "io.grpc:grpc-kotlin-stub-lite:${Versions.grpcKotlin}"
    val grpcOkhttp = "io.grpc:grpc-okhttp:1.34.1"
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
