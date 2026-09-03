plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "app.pulse.vpn"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.pulse.vpn"
        minSdk = 26
        targetSdk = 36
        versionCode = 41
        versionName = "0.5.0-native"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // Пока это alpha-сборка: делаем APK устанавливаемым без секретов в Git.
            // Перед публикацией в Google Play заменить на собственный release-keystore.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin { jvmToolchain(17) }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    packaging.resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.yaml:snakeyaml:2.5")
    implementation("com.tencent:mmkv:2.4.0")
    implementation("com.github.singbox-android:libbox:1.13.15")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.lifecycle:lifecycle-livedata-core-ktx:2.9.2")
    implementation("androidx.annotation:annotation:1.9.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
