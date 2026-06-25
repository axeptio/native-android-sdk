# Axeptio Android SDK

The Axeptio SDK for Android — collect, manage and surface user consents natively in your app.

## Requirements

* Android 13 (API 33) or later (`minSdk 33`)
* `compileSdk 36` or later
* JDK 21 (the SDK is compiled to Java 21 bytecode)
* Kotlin 2.x

## Quick Start

### Installation

The SDK is published as Android libraries (AAR) to a public Maven repository on GitHub. Add the repository and the dependency, and Gradle resolves the SDK and everything it needs transitively.

#### Gradle (recommended)

Add the Axeptio Maven repository to your `settings.gradle.kts` (or top-level `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/axeptio/native-android-sdk/master/maven") }
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

> The repository serves every released version side by side — you select the version with the dependency coordinate below, not in the URL.

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

That single coordinate transitively pulls in the SDK's internal modules and its runtime dependencies (Ktor, Koin, Coil, AndroidX, Kotlin coroutines). You do not declare anything else.

Replace `<version>` with the latest release from the [releases page](https://github.com/axeptio/native-android-sdk/releases).

### Usage

Initialize the SDK once before using any other method — for example in your `Application.onCreate()`:

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

#### Configuration ID (optional)

By default the SDK loads the configuration associated with your `projectId`. If your Axeptio project has multiple configurations, pass `configId` to select a specific one:

```kotlin
AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    configId = "your-config-id"
}
```

When `configId` is omitted the SDK falls back to the default configuration for the project.

Collect the consent status and react to changes in your `Activity` or `Fragment`:

```kotlin
lifecycleScope.launch {
    AxeptioSDK.consentStatusFlow.collect { status ->
        when (status) {
            ConsentStatus.FIRST_TIME      -> AxeptioSDK.showConsentFlow(this@MainActivity)
            ConsentStatus.VENDORS_CHANGED -> AxeptioSDK.showConsentManager(this@MainActivity)
            ConsentStatus.CONSENT_EXPIRED -> AxeptioSDK.showConsentManager(this@MainActivity)
            ConsentStatus.VALID_CONSENT   -> { /* no action needed */ }
            else                          -> { /* handle other states */ }
        }
    }
}
```

### ConsentStatus reference

| Status | Meaning | Recommended action |
|---|---|---|
| `FIRST_TIME` | No previous consent stored | Call `showConsentFlow()` |
| `VENDORS_CHANGED` | Vendor list changed since last consent | Call `showConsentManager()` |
| `CONSENT_EXPIRED` | Stored consent is older than 190 days | Call `showConsentManager()` |
| `VALID_CONSENT` | Consent is current and up to date | No action needed |
| `CONSENTS_SYNC_SUCCESSFUL` | Consents synced with the backend | Informative only |
| `SYNC_FAILED_GIVEUP` | Sync failed after 10 attempts | Show error UI |
| `CONFIG_FETCH_FAILED` | Could not fetch remote configuration | Show error UI; SDK retries automatically |
| `NOT_INITIALIZED` | SDK not yet initialized | Call `AxeptioSDK.initialize()` first |

### Presenting consent screens

The SDK renders its own screens from a `ComponentActivity` — your app does not need to use Jetpack Compose.

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

The SDK requests permissions at runtime, but you must still declare the corresponding Android permissions in your app's `AndroidManifest.xml`. Each `AxeptioPermission` type maps to the following manifest permission(s):

| `AxeptioPermission` | Manifest permission(s) to declare |
|---|---|
| `Camera` | `android.permission.CAMERA` |
| `Microphone` | `android.permission.RECORD_AUDIO` |
| `Notifications` | `android.permission.POST_NOTIFICATIONS` |
| `LocationFine` | `android.permission.ACCESS_FINE_LOCATION`, `android.permission.ACCESS_COARSE_LOCATION` |
| `Contacts` | `android.permission.READ_CONTACTS`, `android.permission.WRITE_CONTACTS` |
| `Calendar` | `android.permission.READ_CALENDAR`, `android.permission.WRITE_CALENDAR` |
| `Bluetooth` | `android.permission.BLUETOOTH_SCAN`, `android.permission.BLUETOOTH_CONNECT` |
| `PhotoLibrary` | `android.permission.READ_MEDIA_IMAGES`, `android.permission.READ_MEDIA_VIDEO`, `android.permission.READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+) |
| `BodySensors` | `android.permission.BODY_SENSORS` |
| `PhoneAccount` | `android.permission.READ_PHONE_STATE` |
| `Fitness` | `android.permission.ACTIVITY_RECOGNITION` |

For example, to use `AxeptioPermission.Camera()` and `AxeptioPermission.LocationFine()`, declare in your manifest:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Clearing consent data

```kotlin
AxeptioSDK.clearConsentData { result ->
    result.onSuccess { /* consent data cleared */ }
    result.onFailure { /* handle error */ }
}
```

## Example App

A sample app is included alongside the published SDK to show it in action. It is a standalone Gradle project that consumes the SDK from the bundled Maven repository (`maven { url = uri("../maven") }`) exactly as your own app would.

1. Clone the SDK repository:

   ```sh
   git clone https://github.com/axeptio/native-android-sdk.git
   ```

2. Open the `sampleApp/` folder in Android Studio (or run it from the command line).
3. Set your Axeptio credentials in `sampleApp/app/src/main/kotlin/io/axeptio/sample/config` (or via the in-app **SDK Configuration** screen), choose a device or emulator, and **Run** the `app` configuration:

   ```sh
   cd sampleApp
   ./gradlew :app:assembleDebug
   ```

The sample app lets you configure project credentials at runtime and demonstrates all consent flows, the permissions screen, and consent status handling.
