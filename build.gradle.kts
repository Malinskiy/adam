buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
    }
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    group = "com.malinskiy"
}
