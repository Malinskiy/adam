plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("com.android.tools.build:gradle:4.1.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
}
