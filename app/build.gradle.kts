plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val bookflowSigningFile = file("bookflow-signing.jks")
val bookflowSigningPassword = System.getenv("BOOKFLOW_SIGNING_PASSWORD")
val bookflowSigningAvailable = bookflowSigningFile.isFile && !bookflowSigningPassword.isNullOrBlank()
if (System.getenv("BOOKFLOW_REQUIRE_PERSISTENT_SIGNING") == "true") {
    check(bookflowSigningAvailable) {
        "Persistent BookFlow signing keystore or password missing. Refusing unsigned/ephemeral CI APK."
    }
}

val firebaseConfigured = file("google-services.json").isFile

if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "app.bookflow.reader"
    signingConfigs {
        if (bookflowSigningAvailable) {
            create("bookflowPersistent") {
                storeFile = bookflowSigningFile
                storeType = "PKCS12"
                storePassword = bookflowSigningPassword
                keyAlias = "bookflow"
                keyPassword = bookflowSigningPassword
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (bookflowSigningAvailable) {
                signingConfig = signingConfigs.getByName("bookflowPersistent")
            }
        }
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "app.bookflow.reader"
        minSdk = 26
        targetSdk = 35
        versionCode = 22
        versionName = "0.2.0-alpha18"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources { noCompress += "onnx" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-session:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation(files("libs/sherpa-onnx.aar"))
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
