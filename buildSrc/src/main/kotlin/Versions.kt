object Versions {
    val admlib = System.getenv("DEPLOY_VERSION_OVERRIDE") ?: "0.0.1"
    val kotlin = "1.3.41"
    val coroutines = "1.2.2"

    val guava = "28.0-jre"
    val annotations = "16.0.2"
    val kxml = "2.3.0"
    val ktor = "1.2.3"
    val serializationRuntime = "0.11.1"

    val junitGradle = "1.0.0"
    val easymock = "3.1"
    val assertk = "0.19"
    val junit = "4.12"
    val dokka = "0.9.17"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val junitGradle = "org.junit.platform:junit-platform-gradle-plugin:${Versions.junitGradle}"
    val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
}

object Libraries {
    val guava = "com.google.guava:guava:${Versions.guava}"
    val annotations = "org.jetbrains:annotations:${Versions.annotations}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}}"
    val kxml = "net.sf.kxml:kxml2:${Versions.kxml}"
    val ktorNetwork = "io.ktor:ktor-network:${Versions.ktor}"
}

object TestLibraries {
    val easymock = "org.easymock:easymock:${Versions.easymock}"
    val assertk = "com.willowtreeapps.assertk:assertk:${Versions.assertk}"
    val junit = "junit:junit:${Versions.junit}"
}