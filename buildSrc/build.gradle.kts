plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("com.android.tools.build:gradle:8.0.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
}
