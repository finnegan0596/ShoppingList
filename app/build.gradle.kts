import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// --- Environment wiring (see docs/development-environment.md) ---------------
// Non-prod / preview API — used by *debug* builds.
val previewApiBaseUrl = "https://shopping-list-api.martintinycart.workers.dev"

// Production API — used by *release* builds only. A debug build can never
// resolve here, and a release build can never resolve to preview.
val productionApiBaseUrl = "https://grocerybuddy-prod.martintinycart.workers.dev"

/**
 * Optional local override for the *debug* API target, so a developer can point
 * the debug app at a local or alternate Worker without editing tracked files.
 *
 * Resolved in order, first non-blank wins:
 *   1. Gradle property  -> `-PREMOTE_API_BASE_URL=...`, or `REMOTE_API_BASE_URL=...`
 *                          in `~/.gradle/gradle.properties`
 *   2. `local.properties` -> `REMOTE_API_BASE_URL=...`
 *   3. Environment variable -> `REMOTE_API_BASE_URL`
 *
 * Debug only: release builds always use [productionApiBaseUrl].
 */
fun debugApiBaseUrl(): String {
    val localPropsFile = rootProject.file("local.properties")
    val localPropsValue = if (localPropsFile.exists()) {
        Properties().apply {
            localPropsFile.inputStream().use { load(it) }
        }.getProperty("REMOTE_API_BASE_URL")
    } else {
        null
    }

    val override = sequenceOf(
        providers.gradleProperty("REMOTE_API_BASE_URL").orNull,
        System.getProperty("REMOTE_API_BASE_URL"),
        localPropsValue,
        System.getenv("REMOTE_API_BASE_URL")
    ).firstOrNull { !it.isNullOrBlank() }

    return (override ?: previewApiBaseUrl).trimEnd('/')
}

android {
    namespace = "com.finnegan0596.shoppinglist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.finnegan0596.shoppinglist"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // NOTE: REMOTE_API_BASE_URL is intentionally NOT set here. It is a
        // per-build-type field (debug = non-prod, release = production) so a
        // release build can never fall back to the non-prod API.

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "REMOTE_API_BASE_URL",
                "\"${debugApiBaseUrl()}\""
            )
        }
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the sideloaded GitHub release APK
            // stays installable. Replace with a real keystore + CI secrets when
            // one exists (see README "Automated releases").
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField(
                "String",
                "REMOTE_API_BASE_URL",
                "\"$productionApiBaseUrl\""
            )
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
