plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    namespace = "com.example.esign"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.esign"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    defaultConfig {
        applicationId = "com.example.esign"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

//    implementation ("com.itextpdf:itextpdf:5.5.13")
//    implementation ("org.bouncycastle:bcprov-jdk15on:1.67")
//    implementation ("org.bouncycastle:bcpkix-jdk15on:1.67")
////    implementation ("com.github.chrisbanes:PhotoView:2.0.0")
////    implementation ("com.github.bumptech.glide:glide:4.9.0")
//////    implementation ("com.github.castorflex.verticalviewpager:library:19.0.1")
////    implementation ("com.google.code.gson:gson:2.8.5")
    implementation ("com.itextpdf:itextpdf:5.5.13")
    implementation ("org.bouncycastle:bcprov-jdk15on:1.61")
    implementation ("org.bouncycastle:bcpkix-jdk15on:1.61")
    implementation ("com.github.castorflex.verticalviewpager:library:19.0.1")
    implementation ("com.google.code.gson:gson:2.9.0")
    implementation ("com.intuit.sdp:sdp-android:1.1.0")
    implementation ("com.github.clans:fab:1.6.4")
}