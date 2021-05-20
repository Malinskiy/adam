buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
        classpath(BuildPlugins.androidGradle)
    }
}

subprojects {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    group = "com.malinskiy.adam"
}
