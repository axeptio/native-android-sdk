plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version of the published Axeptio SDK to depend on. Overridable with
// `-PaxeptioVersion=…`; the publish workflow pins it to each release.
val axeptioVersion = providers.gradleProperty("axeptioVersion").getOrElse("1.0.0")

android {
    namespace = "io.axeptio.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.axeptio.sampleApp"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = axeptioVersion
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // The Axeptio SDK — resolved from the Maven repo declared in settings.gradle.kts.
    // Pulls its foundation modules + Ktor/Koin/Coil/AndroidX transitively.
    implementation("io.axeptio:sdk:$axeptioVersion")

    // The sample's own UI is built with Compose (the SDK does not require
    // consumers to use Compose — it renders its consent UI itself).
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
