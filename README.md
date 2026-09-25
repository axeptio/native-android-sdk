# Axeptio Android SDK

The Axeptio SDK for Android — collect, manage and surface user consents natively in your app.

## Features

- **Two cookie flows** - Brands, or Publisher following the IAB TCF standard, resolved from your
  remote Axeptio configuration.
- **TCF compliant** - exposes the TC string and per-vendor consents for third-party SDKs that expect
  them.
- **System permissions** - request Android runtime permissions (camera, location, notifications, and
  more) from the same configurable flow.
- **Persistent syncing** - consent decisions are cached locally and retried against the Axeptio
  backend with exponential backoff if a sync fails. Unsynced consents retry automatically on the
  next launch.
- **Consent state at hand** - query consent status, the TC string, and per-vendor consents at any
  time, or observe `consentStatusFlow` for changes.
- **26 languages** built in.

## Requirements

* Android 13 (API 33) or later (`minSdk 33`)
* `compileSdk 36` or later
* JDK 21 (the SDK is compiled to Java 21 bytecode)
* Kotlin 2.x

## Quick Start

### Installation

The SDK is published as Android libraries (AAR) to a public Maven repository on GitHub. Add the
repository and the dependency, and Gradle resolves the SDK and everything it needs transitively.

#### Gradle (recommended)

Add the Axeptio Maven repository to your `settings.gradle.kts` (or top-level `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/axeptio/native-android-sdk/master/maven")
        }
    }
}
```

Or using Gradle Groovy DSL in `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://raw.githubusercontent.com/axeptio/native-android-sdk/master/maven' }
    }
}
```

> The repository serves every released version side by side — you select the version with the
> dependency coordinate below, not in the URL.

Then add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.axeptio:sdk:<version>")
}
```

Or using Gradle Groovy DSL in `build.gradle`:

```groovy
dependencies {
    implementation 'io.axeptio:sdk:<version>'
}
```

That single coordinate transitively pulls in the SDK's internal modules and its runtime
dependencies (Ktor, Koin, Coil, AndroidX, Kotlin coroutines). You do not declare anything else.

Replace `<version>` with the latest release from
the [releases page](https://github.com/axeptio/native-android-sdk/releases).

### Usage

Initialize the SDK once before using any other method — for example in your`Application.onCreate()`:

```kotlin
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.configuration.AxeptioService

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AxeptioSDK.initialize(this) {
            projectId = "your-project-id"
            appVersion = "1.0.0"
            token = "your-api-token"
            targetService = AxeptioService.Brands
            // configId = "your-config-id"  // optional — see below
        }
    }
}
```

Use `AxeptioService.Publisher` instead for the IAB TCF flow:

```kotlin
AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    targetService = AxeptioService.Publisher
}
```

If `targetService` is omitted, the SDK defaults to `AxeptioService.Brands`.

#### Configuration ID (optional)

By default the SDK loads the configuration associated with your `projectId`. If your Axeptio project
has multiple configurations, pass `configId` to select a specific one:

```kotlin
AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    configId = "your-config-id"
}
```

When `configId` is omitted the SDK falls back to the default configuration for the project.

#### Environment (optional)

By default the SDK talks to Axeptio's production backend. Pass `environment` to point it at
staging instead, for testing before you go live:

```kotlin
AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    environment = AxeptioEnvironment.Staging
}
```

When `environment` is omitted the SDK defaults to `AxeptioEnvironment.Production`.

#### Shutting down

Call `shutdown()` to tear down the SDK — for example between test runs, or if you need to fully
reset state and re-initialize with different credentials. It detaches all reactive flows and
releases the SDK's internal dependency graph; a subsequent `initialize()` call starts clean.

```kotlin
AxeptioSDK.shutdown()
```

Collect the consent status and react to changes in your `Activity` or `Fragment`:

```kotlin
lifecycleScope.launch {
    AxeptioSDK.consentStatusFlow.collect { status ->
        if (status is ConsentStatus.Ready && status.shouldDisplayConsents) {
            AxeptioSDK.showConsentFlow(this@MainActivity)
        }
    }
}
```

### ConsentStatus reference

| Status              | Meaning                                       | Recommended action                   |
|---------------------|-----------------------------------------------|--------------------------------------|
| `Ready(true)`       | Consent is required (new, changed or expired) | Call `showConsentFlow()`             |
| `Ready(false)`      | Consent is current and up to date             | No action needed                     |
| `ConfigFetchFailed` | Could not fetch remote configuration          | Retry on next app initialization     |
| `NotInitialized`    | SDK not yet initialized                       | Call `AxeptioSDK.initialize()` first |

### Consent queries

The SDK provides methods to query the current consent state and associated data.

### Clearing consent data

These require a coroutine context (e.g., `lifecycleScope.launch`).

```kotlin
lifecycleScope.launch {
    // Clear all stored consent data
    val cleared = AxeptioSDK.clearConsentData()
}
```

#### Callback-based functions

These can be called from anywhere and deliver results via a callback.

```kotlin
// Check how many days until consent expires (negative if already expired)
// ttlDays sets the consent lifetime in days and defaults to 190 if omitted
AxeptioSDK.getRemainingDaysForConsent(ttlDays = 190) { result ->
    result.onSuccess { days -> /* Int */ }
}

// Get the unique Axeptio user token
AxeptioSDK.getAxeptioToken { result ->
    result.onSuccess { token -> /* String? */ }
}

// Get all consented vendors (Brands flow)
AxeptioSDK.getBrandsVendorConsents { result ->
    result.onSuccess { consents -> /* Map<String, Boolean> */ }
}

// Get TCF TC String
AxeptioSDK.getTcfTcString { result ->
    result.onSuccess { tcString -> /* String? */ }
}

// Get TCF Vendor Consents
AxeptioSDK.getTcfVendorConsents { result ->
    result.onSuccess { consents -> /* Map<String, Boolean> */ }
}
```

### Presenting consent screens

The SDK renders its own screens from a `ComponentActivity` — your app does not need to use Jetpack
Compose.

```kotlin
// Show the first-run consent flow
AxeptioSDK.showConsentFlow(activity)

// Open the consent manager (for updates / re-consent)
AxeptioSDK.showConsentManager(activity)

// Open the permissions screen
AxeptioSDK.showPermissionsScreen(activity)
```

### Requesting Android runtime permissions

Pass the permissions you want the SDK to manage during the consent flow:

```kotlin
import io.axeptio.sdk.configuration.AxeptioPermission

AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    withPermissions(
        listOf(
            AxeptioPermission.Camera(),
            AxeptioPermission.Microphone(
                title = "Microphone Access",
                description = "Used for voice features"
            ),
            AxeptioPermission.Notifications(),
            AxeptioPermission.LocationFine(
                title = "Precise Location",
                description = "Used to show nearby content"
            )
        )
    )
}
```

`title`/`description` are optional and only control the copy shown on the SDK's own rationale card,
displayed before the native Android permission dialog — if omitted, the SDK falls back to its own
localized default text. Unlike iOS, Android has no manifest key that's *required* alongside
`initialize()` (there's no Android equivalent of `NSCameraUsageDescription`); the only thing the OS
itself requires is the `<uses-permission>` declaration below.

The SDK requests permissions at runtime, but you must still declare the corresponding Android
permissions in your app's `AndroidManifest.xml`. Each `AxeptioPermission` type maps to the following
manifest permission(s):

| `AxeptioPermission` | Manifest permission(s) to declare                                                                                                                 |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `Camera`            | `android.permission.CAMERA`                                                                                                                       |
| `Microphone`        | `android.permission.RECORD_AUDIO`                                                                                                                 |
| `Notifications`     | `android.permission.POST_NOTIFICATIONS`                                                                                                           |
| `LocationFine`      | `android.permission.ACCESS_FINE_LOCATION`, `android.permission.ACCESS_COARSE_LOCATION`                                                            |
| `Contacts`          | `android.permission.READ_CONTACTS`, `android.permission.WRITE_CONTACTS`                                                                           |
| `Calendar`          | `android.permission.READ_CALENDAR`, `android.permission.WRITE_CALENDAR`                                                                           |
| `Bluetooth`         | `android.permission.BLUETOOTH_SCAN`, `android.permission.BLUETOOTH_CONNECT`                                                                       |
| `PhotoLibrary`      | `android.permission.READ_MEDIA_IMAGES`, `android.permission.READ_MEDIA_VIDEO`, `android.permission.READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+) |
| `BodySensors`       | `android.permission.BODY_SENSORS`                                                                                                                 |
| `PhoneAccount`      | `android.permission.READ_PHONE_STATE`                                                                                                             |
| `Fitness`           | `android.permission.ACTIVITY_RECOGNITION`                                                                                                         |

For example, to use `AxeptioPermission.Camera()` and `AxeptioPermission.LocationFine()`, declare in
your manifest:

```xml

<uses-permission android:name="android.permission.CAMERA" /><uses-permission
android:name="android.permission.ACCESS_FINE_LOCATION" /><uses-permission
android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Logging

Control SDK log output (logcat tag `AxeptioSDK`) via `loggerLevel` at initialization:

```kotlin
AxeptioSDK.initialize(this) {
    // ...
    loggerLevel = AxeptioLogLevel.DEBUG
}
```

| Level            | Output                                                                                                   |
|------------------|----------------------------------------------------------------------------------------------------------|
| `NONE` (default) | Nothing                                                                                                  |
| `DEBUG`          | Full integration narrative: configuration loading, consent decisions, sync results + warnings and errors |

## Handling errors

Calling `initialize()` more than once throws `SdkAlreadyInitializedException`. Calling any other
method before `initialize()` (or after `shutdown()`) throws `SdkNotInitializedException` for
suspend functions, or delivers `Result.failure(SdkNotInitializedException(...))` for
callback-based functions - it's never silently swallowed.

```kotlin
import io.axeptio.sdk.model.SdkNotInitializedException

AxeptioSDK.getAxeptioToken { result ->
    result.onFailure { error ->
        if (error is SdkNotInitializedException) {
            // AxeptioSDK.initialize() hasn't been called yet
        }
    }
}
```

`consentStatusFlow` never throws: it emits `ConsentStatus.NotInitialized` before `initialize()`,
after `shutdown()`, and if an unexpected upstream error occurs, rather than cancelling the
collector.

## Localization

The SDK is localized in **26 languages**: English plus Bulgarian, Croatian, Czech, Danish, Dutch,
Estonian, Finnish, French, German, Greek, Hungarian, Irish, Italian, Latvian, Lithuanian, Maltese,
Norwegian Bokmål, Polish, Portuguese, Romanian, Russian, Slovak, Slovenian, Spanish, and Swedish.

All 26 are bundled into the AAR and merged straight into your app by Gradle, so the SDK's screens
resolve independently against the device's locale - they'll show, say, French on a French-language
device even if your own app has no French strings at all.

> **Important:** If your app strips locales at build time to reduce APK size (via
> `resourceConfigurations` or `androidResources.localeFilters` in Android Gradle Plugin), any
> locale excluded there is removed from the final APK entirely - including the SDK's - and falls
> back to the default `values/` (English) for those languages too.

## Example App

A sample app is included alongside the published SDK to show it in action. It is a standalone Gradle
project that consumes the SDK from the bundled Maven repository (`maven { url = uri("../maven") }`)
exactly as your own app would.

1. Clone the SDK repository:

   ```sh
   git clone https://github.com/axeptio/native-android-sdk.git
   ```

2. Open the `sampleApp/` folder in Android Studio (or run it from the command line).
3. Set your Axeptio credentials in `sampleApp/app/src/main/kotlin/io/axeptio/sample/config` (or via
   the in-app **SDK Configuration** screen), choose a device or emulator, and **Run** the `app`
   configuration:

   ```sh
   cd sampleApp
   ./gradlew :app:assembleDebug
   ```

The sample app lets you configure project credentials at runtime and demonstrates all consent flows,
the permissions screen, and consent status handling.
