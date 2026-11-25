import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.shp.evernote"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.shp.evernote"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // One stable debug key across machines and CI runs, so a new debug
        // APK installs over the previous one instead of failing with
        // INSTALL_FAILED_UPDATE_INCOMPATIBLE. The keystore is not in the
        // repository: CI decodes it from the DEBUG_KEYSTORE_BASE64 secret
        // into the path given by DEBUG_KEYSTORE; locally, set
        // debug.keystore=/path/to/debug.keystore in local.properties. When
        // neither is present, the SDK's default debug key is used.
        val keystorePath = System.getenv("DEBUG_KEYSTORE")
            ?: localProperty("debug.keystore")
        if (keystorePath != null && file(keystorePath).exists()) {
            getByName("debug") {
                storeFile = file(keystorePath)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // No signing config on purpose: distribution is undecided and
            // release builds are not produced yet. Debug builds only for now.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        // CI treats warnings as errors so lint drift is caught early.
        warningsAsErrors = true
        abortOnError = true
        // Version-freshness checks hit the network and fail the day a new
        // release ships; dependency updates are a deliberate change instead.
        disable += listOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
    }
}

/** Reads a key from local.properties, or null when the file or key is absent. */
fun localProperty(key: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return null
    return Properties().apply { file.inputStream().use { load(it) } }.getProperty(key)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.runtime.livedata)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
