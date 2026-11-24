plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.plusnet.care"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.plusnet.care"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "3.2-Cloud"
    }
    signingConfigs {
        create("fixedSign") {
            storeFile = file("keystore/plusnet-release.jks")
            storePassword = "plusnet1234"
            keyAlias = "plusnet_key"
            keyPassword = "plusnet1234"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixedSign")
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("fixedSign")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
