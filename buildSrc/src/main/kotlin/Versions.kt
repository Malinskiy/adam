object Versions {
    val admlib = System.getenv("DEPLOY_VERSION_OVERRIDE") ?: "0.0.1"

    val kotlin = "1.3.41"
    val coroutines = "1.2.1"

    val junitGradle = "1.0.0"
    val dokka = "0.9.17"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val junitGradle = "org.junit.platform:junit-platform-gradle-plugin:${Versions.junitGradle}"
    val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
}

object Libraries {

}

object TestLibraries {

}