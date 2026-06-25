// Standalone consumer project. It does not use the SDK's build-logic or version
// catalog — it depends on the Axeptio SDK exactly like a third-party app would:
// one Maven repository (see settings.gradle.kts) + one dependency coordinate.
//
// Plugin versions mirror the SDK's own toolchain (AGP 9.1.0 + the Compose
// compiler that ships with the SDK's Kotlin); Kotlin itself is compiled by
// AGP's built-in Kotlin support, so no `kotlin.android` plugin is applied.
plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}
