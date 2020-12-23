object Versions {
    val adam = System.getenv("GIT_TAG_NAME") ?: "0.1.0"
    val kotlin = "1.4.10"
    val coroutines = "1.3.9"

    val annotations = "16.0.2"
    val kxml = "2.3.0"
    val ktor = "1.4.1"
    val logging = "1.7.6"

    val assertk = "0.19"
    val junit = "4.12"
    val imageComparison = "4.3.0"
    val dokka = "0.9.17"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
}

object Libraries {
    val annotations = "org.jetbrains:annotations:${Versions.annotations}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}}"
    val kxml = "net.sf.kxml:kxml2:${Versions.kxml}"
    val ktorNetwork = "io.ktor:ktor-network-jvm:${Versions.ktor}"
    val logging = "io.github.microutils:kotlin-logging:${Versions.logging}"
}

object TestLibraries {
    val assertk = "com.willowtreeapps.assertk:assertk:${Versions.assertk}"
    val junit = "junit:junit:${Versions.junit}"
    val imageComparison = "com.github.romankh3:image-comparison:${Versions.imageComparison}"
}
