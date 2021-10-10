plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("com.android.tools.build:gradle:7.0.2")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.5.0")
}
