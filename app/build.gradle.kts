plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.app.distribution)
}

val rutauApiBaseUrl = "https://rutau-back.onrender.com/api/v1/"
val rutauRealtimeDatabaseUrl = "https://ruta-u-ebf74-default-rtdb.firebaseio.com/"

android {
    namespace = "uvg.edu.rutau"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.apprutau.rutau"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$rutauApiBaseUrl\"")
            buildConfigField("String", "REALTIME_DATABASE_URL", "\"$rutauRealtimeDatabaseUrl\"")
            buildConfigField("boolean", "CRASHLYTICS_COLLECTION_ENABLED", "true")
            manifestPlaceholders["usesCleartextTraffic"] = false
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
        }
        release {
            optimization {
                enable = false
            }
            buildConfigField("String", "API_BASE_URL", "\"$rutauApiBaseUrl\"")
            buildConfigField("String", "REALTIME_DATABASE_URL", "\"$rutauRealtimeDatabaseUrl\"")
            buildConfigField("boolean", "CRASHLYTICS_COLLECTION_ENABLED", "true")
            manifestPlaceholders["usesCleartextTraffic"] = false
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

firebaseAppDistributionDefault {
    artifactType = "APK"
    testers = "carlosaltan850@gmail.com"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
