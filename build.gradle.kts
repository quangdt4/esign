buildscript {
    repositories {
        mavenCentral()
    }
}


plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

apply(from = "/Users/quangdo2/Documents/other/esign/ktlint.gradle")