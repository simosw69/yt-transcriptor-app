plugins {
    alias(libs.plugins.androidApplication)
}

import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.example.yt_transcriptor_app"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.yt_transcriptor_app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envFile = project.rootProject.file(".env")
        val properties = Properties()
        if (envFile.exists()) {
            properties.load(FileInputStream(envFile))
        }
        val apiKey = properties.getProperty("RAPIDAPI_KEY") ?: "\"\""
        buildConfigField("String", "RAPIDAPI_KEY", apiKey)
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}