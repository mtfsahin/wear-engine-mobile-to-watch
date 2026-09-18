import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Secrets live in local.properties, which git ignores. Copy local.properties.example and
 * fill in your own values. An environment variable with the same name also works, which
 * is what a CI job would use. Nothing here fails the build when a value is missing: the
 * app then shows which key to add.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun secret(key: String, envKey: String, default: String = ""): String =
    localProperties.getProperty(key) ?: System.getenv(envKey) ?: default

val healthKitDeepLink = secret(
    "healthkit.deepLink",
    "HEALTH_KIT_DEEP_LINK",
    "wearhealthkit://oauth/callback",
)
val deepLinkUri = java.net.URI(healthKitDeepLink)

android {
    namespace = "com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Huawei Health Kit Cloud configuration, read by healthkit/HealthKitConfig.kt.
        buildConfigField(
            "String",
            "HEALTH_KIT_CLIENT_ID",
            "\"${secret("healthkit.clientId", "HEALTH_KIT_CLIENT_ID")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_CLIENT_SECRET",
            "\"${secret("healthkit.clientSecret", "HEALTH_KIT_CLIENT_SECRET")}\"",
        )
        buildConfigField("String", "HEALTH_KIT_DEEP_LINK", "\"$healthKitDeepLink\"")
        buildConfigField(
            "String",
            "HEALTH_KIT_SCHEME_SECRET",
            "\"${secret("healthkit.schemeSecret", "HEALTH_KIT_SCHEME_SECRET")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_AUTH_METHOD",
            "\"${secret("healthkit.authMethod", "HEALTH_KIT_AUTH_METHOD", "WEB_OAUTH")}\"",
        )

        // The deep link in AndroidManifest.xml is built from the same single value.
        manifestPlaceholders["healthKitScheme"] = deepLinkUri.scheme ?: "wearhealthkit"
        manifestPlaceholders["healthKitHost"] = deepLinkUri.host ?: "oauth"
        manifestPlaceholders["healthKitPath"] =
            deepLinkUri.path?.takeIf { it.isNotBlank() } ?: "/callback"
        manifestPlaceholders["wearEngineAppId"] =
            secret("wearengine.appId", "WEAR_ENGINE_APP_ID", "your_wearengine_app_id")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Wear Engine: phone to watch messages and files.
    implementation(libs.huawei.hms.wearengine)

    // Method 1: the Custom Tab that shows Huawei's authorization page.
    implementation(libs.androidx.browser)

    // Method 2: the login-free authorization SDK. It comes from the Huawei Maven
    // repository, which settings.gradle.kts already lists.
    implementation(libs.huawei.hms.health)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
