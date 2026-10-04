<img alt="Axeptio Native Android SDK" src="https://github.com/user-attachments/assets/c4c2d3a6-52a1-4515-b27f-4041af19fcf6" width="600" height="300"/>

# Axeptio Native Android SDK

[![Latest release](https://img.shields.io/github/v/release/axeptio/native-android-sdk)](https://github.com/axeptio/native-android-sdk/releases) [![License](https://img.shields.io/badge/license-Axeptio-blue.svg)](LICENSE) [![Kotlin](https://img.shields.io/badge/Kotlin-2.2%2B-blue)](https://kotlinlang.org) [![Android API](https://img.shields.io/badge/Android%20API-%3E%3D%2033-blue)](https://developer.android.com/about/versions/13)

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
* `compileSdk 36` or later, with an Android Gradle Plugin version that supports it
* JDK 21 (the SDK is compiled to Java 21 bytecode)
* Kotlin 2.2 or later (the SDK is built with Kotlin 2.3; older compilers cannot read its metadata)

The SDK declares the `android.permission.INTERNET` permission itself; Gradle merges it into your
app's manifest automatically, so you don't need to add it.

The public API is designed for **Kotlin**. Calling it from Java is not supported (it relies on
`suspend` functions, `Flow`, `kotlin.Result` and a lambda-with-receiver builder).

### Before you start

You need an Axeptio project set up for mobile in the
[Axeptio back-office](https://admin.axeptio.eu). The SDK is initialized with:

| Parameter       | Required | What it is                                                                                                                                                     |
|-----------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `projectId`     | Yes      | Your Axeptio project identifier, from the back-office.                                                                                                         |
| `appVersion`    | Yes      | Sent with every configuration request as the `version` parameter. Use your app's version name.                                                                |
| `token`         | No       | Your project's API token, from the back-office. When set, it is sent as a `Bearer` authorization header on every request.                                     |
| `targetService` | No       | `AxeptioService.Brands` (default) or `AxeptioService.Publisher` (IAB TCF). Without `configId`, must match the configurations defined in your project.         |
| `configId`      | No       | Pins one configuration of the project; its flow then wins over `targetService` — see [Configuration ID](#configuration-id-optional).                          |

If you are unsure which values to use, contact [Axeptio support](#support).

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
    implementation("io.axeptio:sdk:1.2.0")
}
```

Or using Gradle Groovy DSL in `build.gradle`:

```groovy
dependencies {
    implementation 'io.axeptio:sdk:1.2.0'
}
```

Check the [releases page](https://github.com/axeptio/native-android-sdk/releases) for the latest
version and the release notes of each. Pre-releases (for example
`X.Y.Z-beta.N`) are published to the same repository and are opt-in: pin them explicitly to try
upcoming features. A new release can take a few minutes to become resolvable, because
`raw.githubusercontent.com` caches files.

To keep Gradle from querying the Axeptio repository for every other dependency, you can restrict it
to the `io.axeptio` group:

```kotlin
maven {
    url = uri("https://raw.githubusercontent.com/axeptio/native-android-sdk/master/maven")
    content { includeGroup("io.axeptio") }
}
```

#### Transitive dependencies

That single coordinate transitively pulls in the SDK's internal modules and its runtime
dependencies. You do not declare anything else, but Gradle resolves each of them to the highest
version requested in your build, so check for conflicts if your app uses the same libraries:

| Library               | Version used by the SDK |
|-----------------------|-------------------------|
| Ktor client (OkHttp)  | 3.4                     |
| Koin                  | 4.2                     |
| Coil                  | 3.4                     |
| Kotlin coroutines     | 1.10                    |
| kotlinx.serialization | 1.11                    |
| AndroidX / Compose    | Compose BOM 2026.03     |

The SDK runs its own isolated Koin instance, so it never touches your app's global Koin
container.

#### R8 / ProGuard

The SDK ships its own R8/ProGuard consumer rules inside the AAR. If your release build enables
minification, no extra configuration is needed — Gradle applies the SDK's rules automatically.

### Usage

Initialize the SDK once before using any other method — for example in your `Application.onCreate()`:

```kotlin
import android.app.Application
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.configuration.AxeptioService

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AxeptioSDK.initialize(this) {
            projectId = "your-project-id"
            appVersion = "1.0.0"          // your app's version name
            token = "your-api-token"
            targetService = AxeptioService.Brands
            // configId = "your-config-id"  // optional — see below
        }
    }
}
```

Register it in your `AndroidManifest.xml`: `<application android:name=".MyApplication" …>`.

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

An Axeptio project can hold several configurations. By default the SDK picks one for you:

* **Brands** — the configuration matching the user's location (and device language), resolved by
  Axeptio's geolocation service. If that lookup fails, the project's default configuration.
* **Publisher (TCF)** — the TCF configuration whose language matches the device language,
  otherwise the first TCF configuration of the project.

Pass `configId` to always use a specific configuration instead:

```kotlin
AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    configId = "your-config-id"
}
```

A pinned `configId` decides the flow, as on iOS: the SDK looks it up among all the project's
configurations, so pinning a TCF configuration runs the Publisher (TCF) flow even when
`targetService` is omitted or `AxeptioService.Brands`, and pinning a Brands configuration runs the
Brands flow even with `AxeptioService.Publisher`. Neither geolocation nor the device language is
used. If `configId` matches no configuration of the project, the SDK logs a warning and uses the
project's default configuration.

> **Note:** consent is stored per configuration. When the resolved configuration changes (for
> example you change `configId`, or the user moves to a region served by another configuration),
> the stored consent is discarded and the user is asked again.

#### Environment (optional)

By default the SDK talks to Axeptio's production backend. Pass `environment` to point it at
Axeptio's staging backend instead — only do this if Axeptio has set up your project there:

```kotlin
import io.axeptio.sdk.configuration.AxeptioEnvironment

AxeptioSDK.initialize(this) {
    projectId = "your-project-id"
    appVersion = "1.0.0"
    token = "your-api-token"
    environment = AxeptioEnvironment.Staging
}
```

When `environment` is omitted the SDK defaults to `AxeptioEnvironment.Production`. The configuration
and vendor data the SDK caches for offline use, and the user token and configuration id it stores,
are kept apart per environment. The user's consent and the `IABTCF_*` keys are not: they are the
user's on this device.

### Requesting Android runtime permissions

Pass the permissions you want the SDK to manage during the consent flow:

```kotlin
import android.app.Application
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.configuration.AxeptioPermission

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
    }
}
```

Each permission type may appear only once: passing duplicates makes `initialize()` throw
`IllegalArgumentException`.

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
| `BodySensors`       | `android.permission.BODY_SENSORS`, plus `android.permission.health.READ_HEART_RATE` when targeting API 36 (see below)                            |
| `PhoneAccount`      | `android.permission.READ_PHONE_STATE`                                                                                                             |
| `Fitness`           | `android.permission.ACTIVITY_RECOGNITION`                                                                                                         |

For example, to use `AxeptioPermission.Camera()` and `AxeptioPermission.LocationFine()`, declare in
your manifest:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Android 16 (API 36) replaces `BODY_SENSORS` with `android.permission.health.READ_HEART_RATE` for apps
targeting API 36, so on Android 16+ `BodySensors` requests both and the system asks for the one your
app declares. An app targeting API 36 declares:

```xml
<uses-permission android:name="android.permission.BODY_SENSORS" android:maxSdkVersion="35" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
```

Android also requires such an app to show its privacy policy from an activity handling
`android.intent.action.VIEW_PERMISSION_USAGE` in the `android.intent.category.HEALTH_PERMISSIONS`
category (see the [Android 16 behaviour changes](https://developer.android.com/about/versions/16/behavior-changes-16)).

### Logging

Control SDK log output (logcat tag `AxeptioSDK`) via `loggerLevel` at initialization:

```kotlin
import io.axeptio.sdk.model.AxeptioLogLevel

AxeptioSDK.initialize(this) {
    // ...
    loggerLevel = AxeptioLogLevel.DEBUG
}
```

| Level            | Output                                                                                                   |
|------------------|------------------------------------------------------------------------------------------------------------|
| `NONE` (default) | Nothing                                                                                                  |
| `DEBUG`          | Initialization, the selected configuration, consent saved, IAB TCF values written, warnings and errors  |

#### Showing the consent flow

Collect the consent status in your `Activity` and show the consent flow when it is needed. Use
`repeatOnLifecycle` so collection stops while the activity is in the background:

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.model.ConsentStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AxeptioSDK.consentStatusFlow.collect { status ->
                    if (status is ConsentStatus.Ready && status.shouldDisplayConsents) {
                        AxeptioSDK.showConsentFlow(this@MainActivity)
                    }
                }
            }
        }
    }
}
```

In a `Fragment`, launch from `viewLifecycleOwner.lifecycleScope` and collect within
`viewLifecycleOwner.repeatOnLifecycle(...)`, passing `requireActivity()` to `showConsentFlow`.

Once the user completes the flow and the consent is stored, the status changes to `Ready(false)` —
observe that transition if you need to know when to initialize consent-dependent SDKs.

The SDK renders its own screens from a `ComponentActivity` — your app does not need to use Jetpack
Compose. Only call these after `initialize()`: calling one before `initialize()` (or after
`shutdown()`) throws `SdkNotInitializedException`. Initializing in `Application.onCreate()`, before
any activity can call these, is the simplest way to avoid it. (The iOS SDK reports `notInitialized`
to its error handler instead of throwing.)

```kotlin
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.model.SdkNotInitializedException

// Show the first-run consent flow
try {
    AxeptioSDK.showConsentFlow(activity)
} catch (e: SdkNotInitializedException) {
    // AxeptioSDK.initialize() hasn't been called yet
}

// Open the consent manager (for updates / re-consent)
AxeptioSDK.showConsentManager(activity)

// Open the permissions screen
AxeptioSDK.showPermissionsScreen(activity)
```

### ConsentStatus reference

| Status              | Meaning                                                 | Recommended action                        |
|---------------------|----------------------------------------------------------|--------------------------------------------|
| `Ready(true)`       | Consent is required: none yet, expired, or asked again ([below](#when-the-sdk-asks-again)) | Call `showConsentFlow()`                  |
| `Ready(false)`      | Consent is current and up to date                        | No action needed                          |
| `ConfigFetchFailed` | Could not fetch the configuration and none is cached     | Retry: `shutdown()`, then `initialize()` again |
| `NotInitialized`    | `initialize()` has not been called, or `shutdown()` was  | Call `AxeptioSDK.initialize()` (again, after `shutdown()`) |

> **Note:** right after `initialize()`, nothing is emitted until the configuration has loaded or
> failed — an earlier collector keeps seeing `NotInitialized` until then. Don't call
> `initialize()` again in response: the SDK is already initialized at that point, so a second call
> throws `SdkAlreadyInitializedException` — the flow catches up on its own once loading finishes.

### When the SDK asks again

Like the Axeptio web CMPs, `Ready(true)` comes back for a user who already consented only when:

| Reason | Brands | TCF (Publisher) |
|---|---|---|
| Consent lifetime passed | 190 days | The configuration's `expirationTtlDays` (1 to 190 days), else 190 |
| The configuration asks for a new consent (its **askNewConsent** setting) | A vendor it configures has no choice from the user yet (a new vendor). A removed vendor doesn't count | Its CMP version differs from the one the consent was given under |
| Another configuration is resolved (e.g. another language or country) | ✓ | ✓ |

Without **askNewConsent**, vendor-list or CMP-version changes never ask again; a new TCF vendor
list (GVL version) alone never does. Both rules use the configuration in use - the one cached from
an earlier session until the network answers. The lifetime is fixed when the consent is saved, like
the web CMPs: a later change of `expirationTtlDays` only applies to new consents. A consent saved by
an earlier SDK version keeps the lifetime of the configuration in use when this version first loads
one.

### Consent queries

The SDK provides methods to query the current consent state and associated data. They can be
called from any thread and deliver their result through a callback.

> **Threading:** callbacks are invoked on a **background thread** (the SDK's `Dispatchers.IO`
> scope), except when the SDK isn't initialized: the `SdkNotInitializedException` failure is then
> delivered synchronously on the calling thread. Otherwise, switch to the main thread before
> touching your UI, for example with `runOnUiThread { … }` or
> `lifecycleScope.launch(Dispatchers.Main) { … }`. The [event listener](#sdk-events) is the
> exception: it is always called on the main thread.

Every query but `getAxeptioToken()`, made right after `initialize()`, waits for the configuration
(cached, else fetched). If `shutdown()` tears the SDK down before a query answers, its callback gets
`Result.failure(SdkNotInitializedException(...))`, on a background thread - it is always called
back. While an open SDK screen keeps that SDK instance alive, the query answers from it instead,
or fails the same way if that screen closes first.

```kotlin
// Days until the stored consent expires (negative once expired), counted against the lifetime the
// consent was saved with (TCF: the configuration's expirationTtlDays, else 190; Brands: 190) or a shorter
// ttlDays of your own - a longer one is capped at the lifetime. Returns that duration when no
// consent is stored yet. ttlDays never changes when the SDK itself asks again.
AxeptioSDK.getRemainingDaysForConsent { result ->
    result.onSuccess { days -> /* Int */ }
}

// Get the unique Axeptio user token
AxeptioSDK.getAxeptioToken { result ->
    result.onSuccess { token -> /* String? */ }
}

// Every vendor with a stored choice (Brands flow), keyed by display name — true or false
AxeptioSDK.getBrandsVendorConsents { result ->
    result.onSuccess { consents -> /* Map<String, Boolean> */ }
}

// Get TCF TC String
AxeptioSDK.getTcfTcString { result ->
    result.onSuccess { tcString -> /* String? */ }
}

// Every disclosed vendor's consent (Publisher/TCF flow), keyed by IAB vendor id. A vendor the
// user accepted for a special feature only reads true, though the TC string has no consent bit
// for it.
AxeptioSDK.getTcfVendorConsents { result ->
    result.onSuccess { consents -> /* Map<String, Boolean> */ }
}
```

### Clearing consent data

`clearConsentData()` is a `suspend` function, so call it from a coroutine:

```kotlin
lifecycleScope.launch {
    val cleared: Boolean = AxeptioSDK.clearConsentData()
}
```

It removes the stored consent — including, on the Publisher flow, the `IABTCF_*` values (below) —
and returns `true` on success, regardless of whether the configuration has finished loading yet.
`consentStatusFlow` then emits `Ready(true)` immediately, so an active collector shows the consent
flow again right away, and the [event listener](#sdk-events) gets `onConsentsUpdated()`. It throws
`SdkNotInitializedException` if the SDK is not initialized.

#### Shutting down

Call `shutdown()` to tear down the SDK — for example between test runs, or if you need to fully
reset state and re-initialize with different credentials. It detaches all reactive flows and
releases the SDK's internal dependency graph; a subsequent `initialize()` call starts clean.
Existing `consentStatusFlow` collectors stay subscribed: they receive `NotInitialized`
immediately, then the new status once you call `initialize()` again. Any open SDK screen (the
consent flow, consent manager or permissions screen) closes. Calling `shutdown()` when the SDK
isn't initialized is a no-op.

Once `shutdown()` returns, the [event listener](#sdk-events) hears nothing more from that SDK
instance, not even an event that happened just before. Two exceptions: `onConsentFlowClosed()`, for
a screen the shutdown closed, and an `AxeptioError.Internal` from a `consentStatusFlow` collection,
which isn't tied to an SDK instance. A consent query still waiting for the configuration fails with
`SdkNotInitializedException` (see [Consent queries](#consent-queries)). Like `removeEventListener()`, called from another thread while a
listener callback runs on the main thread, `shutdown()` waits for that callback to return.

```kotlin
AxeptioSDK.shutdown()
```

### SDK events

Set an `AxeptioEventListener` to hear when an SDK screen closes, when the user's consent changes,
and when the SDK hits an error. Override only the callbacks you need:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before initialize(), to also hear about a failed configuration fetch.
        AxeptioSDK.setEventListener(object : AxeptioEventListener {
            override fun onConsentFlowClosed() { /* the consent UI went away */ }
            override fun onConsentsUpdated() { /* re-read getTcfTcString(), getBrandsVendorConsents()… */ }
            override fun onError(error: AxeptioError) { Log.w("App", error.message) }
        })
        AxeptioSDK.initialize(this) { /* … */ }
    }
}
```

| Callback | When |
|---|---|
| `onConsentFlowClosed()` | A screen opened by `showConsentFlow()`, `showConsentManager()` or `showPermissionsScreen()` went away: finished, dismissed with Back, or closed by the SDK. Once per screen, never for a rotation. |
| `onConsentsUpdated()` | The consent stored on the device changed: a new choice was saved (Brands), its `IABTCF_*` keys were written (TCF), a previously unsynced choice was synced on a retry, or `clearConsentData()` cleared it. The query methods above already return the new values. |
| `onError(AxeptioError)` | The SDK hit an error: a failed configuration load - the configuration fetch, else its vendor list (Brands vendors or TCF configuration), else the user token request; once per load, reported even when cached data keeps the SDK working (`consentStatusFlow` tells whether the consent flow can be shown); a consent choice that couldn't be synced to the backend; or an unexpected internal error. |

`AxeptioError` cases: `InvalidAuthToken` (the backend rejected the token, HTTP 401),
`ConfigurationUnavailable` (the project has no configuration), and `Network(failure)` with
`NetworkFailure.Connectivity`, `Server(statusCode)` or `InvalidResponse`,
`ConsentSyncFailed(failure)` and `Internal(cause)`. Each has a `message` for logs. More cases may
come in minor releases, so give a `when` over them an `else` branch.

- `ConsentSyncFailed`: the user's choice is stored and applied on the device, but the SDK gave up
  sending it to the backend after its retries (typically offline). It retries at the next app
  starts on its own; the error is reported once per given-up choice, not again for those retries.
  `failure` is always `null` for now; a later release may say how the sync failed, as iOS does.
- `Internal`: an unexpected error inside the SDK, with its `cause` - for example
  `consentStatusFlow` failing to read the stored consent (see [Handling errors](#handling-errors)),
  or a failure in the SDK's background work, which never crashes your app. The latter is reported
  once, and never after `shutdown()`.

- **Threading:** callbacks run on the **main thread**; `setEventListener()` and
  `removeEventListener()` can be called from any thread.
- **One listener:** `setEventListener()` replaces the previous listener; `setEventListener(null)` or
  `removeEventListener()` removes it. Once that returns, the removed listener is never called again.
  The trade-off: called from another thread while a callback runs on the main thread, these calls
  (and `shutdown()`) **block** until that callback returns, so keep callbacks short and never make
  one wait on a thread that may be setting or removing the listener or shutting the SDK down
  (deadlock). On the main thread they never block.
- **Lifetime:** the SDK keeps a strong reference to the listener until it is removed, across
  `shutdown()` and a new `initialize()`. Register an `Application`-scoped listener (as above), or
  remove an `Activity`-scoped one in `onDestroy()` so the SDK doesn't leak the activity.
- **Failures:** anything a callback throws is logged and swallowed.
- **Java:** implement `AxeptioEventListener` and override only what you need - the other methods
  are default methods. Call `AxeptioSDK.INSTANCE.setEventListener(listener)`.

### IAB TCF storage (Publisher flow)

In the Publisher flow the SDK writes the standard IAB TCF v2 keys to the app's **default
`SharedPreferences`** (`PreferenceManager.getDefaultSharedPreferences(context)`), where ad and
analytics SDKs that support TCF read them automatically:

`IABTCF_TCString`, `IABTCF_CmpSdkID`, `IABTCF_CmpSdkVersion`, `IABTCF_PolicyVersion`,
`IABTCF_PublisherCC`, `IABTCF_gdprApplies`, `IABTCF_PurposeOneTreatment`,
`IABTCF_UseNonStandardTexts`, `IABTCF_VendorConsents`, `IABTCF_VendorLegitimateInterests`,
`IABTCF_DisclosedVendors`, `IABTCF_PurposeConsents`, `IABTCF_PurposeLegitimateInterests`,
`IABTCF_SpecialFeaturesOptIns`.

When the configuration selects purposes or stacks, the consent screens list purpose 1, those
purposes and the selected stacks' purposes besides the vendors' own, even when no vendor declares
them, like the iOS SDK. Without configured purposes or stacks, the screens list the vendors'
purposes only.

Accepting everything takes the whole TCF catalog, like the web CMP and the iOS SDK: every purpose is
consented, every purpose but 1 and 3-6 (consent only) is under legitimate interest, and every
special feature is opted in, in the TC string and the `IABTCF_*` keys. Saving custom choices is
narrower: a purpose no vendor declares has no toggle in the details, as on iOS, so it stays refused.

## Upgrading from 1.0.1

A few things changed since 1.0.1:

- **`AxeptioPermission` import.** Already importing it from `io.axeptio.sdk.configuration`?
  Nothing to do. Otherwise replace `import io.axeptio.foundation.core.config.AxeptioPermission`
  with `import io.axeptio.sdk.configuration.AxeptioPermission`. The old import still compiles, but
  `withPermissions()` then shows a deprecation warning, and that overload may be removed in a
  future major release. Three cases stop compiling until you switch:
  - `permissions = listOf(...)` assigned directly: switch to the new import.
  - An untyped empty list, `withPermissions(emptyList())` or `withPermissions(listOf())`: add the
    type, e.g. `withPermissions(emptyList<AxeptioPermission>())`, or remove the call (no
    permissions is the default).
  - Reading `permissionType` or `androidPermission` on a permission: use `manifestPermissions`
    instead, which lists the covered `android.permission.*` strings (`allManifestPermissions` is
    still available too).
- **INTERNET permission.** The SDK now declares `android.permission.INTERNET` itself. You can
  remove it from your own manifest if you only added it for the SDK — keeping it is harmless.
- **R8 / ProGuard rules.** Consumer rules now ship inside the AAR. Remove any `-keep io.axeptio`
  rules you had copied into your own ProGuard files.
- **`show*()` before `initialize()`.** Calling `showConsentFlow()`, `showConsentManager()` or
  `showPermissionsScreen()` before `initialize()` (or after `shutdown()`) now throws
  `SdkNotInitializedException` — see [Showing the consent flow](#showing-the-consent-flow).

## Handling errors

Calling `initialize()` more than once throws `SdkAlreadyInitializedException` — call `shutdown()`
first to re-initialize. Calling a query method or a `show*` method before `initialize()` (or after
`shutdown()`) throws `SdkNotInitializedException` for suspend functions and `show*` methods, or
delivers `Result.failure(SdkNotInitializedException(...))` for callback-based functions - it's never
silently swallowed.

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

Errors the SDK hits on its own, such as a configuration fetch rejected for a bad token, are
reported to the [event listener](#sdk-events)'s `onError()`.

When the configuration couldn't be fetched and none is cached, `consentStatusFlow` emits
`ConfigFetchFailed` and the SDK doesn't retry on its own. To retry (for example once the device is
back online), call `shutdown()` then `initialize()` again: a second `initialize()` without
`shutdown()` throws `SdkAlreadyInitializedException` (the iOS SDK takes it directly). Existing
`consentStatusFlow` collectors stay subscribed and see the new status.

`consentStatusFlow` never throws. Before `initialize()` and after `shutdown()` it emits
`ConsentStatus.NotInitialized`, then switches to live decisions once you call `initialize()` again
— no re-subscription needed. An unexpected internal error is different: it emits `NotInitialized`
once and then the flow **completes**, so collect it again (for example, the next
`repeatOnLifecycle` start) rather than treating that emission as the SDK being uninitialized. The
error is also reported to the event listener as `AxeptioError.Internal`, once per collection that
hit it. (The iOS stream never finishes on its own.)

## Localization

The SDK is localized in **26 languages**: English plus Bulgarian, Croatian, Czech, Danish, Dutch,
Estonian, Finnish, French, German, Greek, Hungarian, Irish, Italian, Latvian, Lithuanian, Maltese,
Norwegian Bokmål, Polish, Portuguese, Romanian, Russian, Slovak, Slovenian, Spanish, and Swedish.

All 26 are bundled into the AAR and merged straight into your app by Gradle, so the SDK's screens
resolve independently against the device's locale - they'll show, say, French on a French-language
device even if your own app has no French strings at all. Norwegian Bokmål resolves on both the
`nb` locale Android devices report and the legacy `no` code.

> **Important:** If your app strips locales at build time to reduce APK size (via
> `resourceConfigurations` or `androidResources.localeFilters` in Android Gradle Plugin), any
> locale excluded there is removed from the final APK entirely - including the SDK's - and falls
> back to the default `values/` (English) for those languages too. To keep Norwegian, keep `nb`
> (and `no` if your app uses it).

## Network and data collected

The SDK talks to `https://headless-api.axeptio.tech` over HTTPS for configuration and consent
data. Vendor and brand images shown on the consent screens are downloaded separately, from
whatever hosts your Axeptio configuration references. Use this list when you fill in Google Play's
Data safety form:

| Purpose                   | What is sent                                                                                           |
|---------------------------|--------------------------------------------------------------------------------------------------------|
| Configuration             | Project ID, configuration ID, `appVersion`, device language                                            |
| Location-based config     | Project ID and device language (Brands flow; the server derives the region from the request's IP)      |
| Consent records           | The user's vendor choices, the Axeptio user token, `appVersion`, platform and configuration ID         |
| TCF                       | Vendor list and TC string encoding requests (Publisher flow)                                           |
| Usage analytics           | Consent-flow events with timestamp, user agent, Axeptio user token, project and configuration IDs      |

The Axeptio user token is issued by Axeptio's backend on first use and identifies the consent
record; the SDK does not read the advertising ID.

## Migrating from the WebView SDK

This SDK replaces Axeptio's WebView-based
[`axeptio-android-sdk`](https://github.com/axeptio/axeptio-android-sdk) with native screens. The two
are separate products with different artifacts and APIs; do not include both.

| WebView SDK (`axeptio-android-sdk`)                    | Native SDK (this repository)                                     |
|--------------------------------------------------------|------------------------------------------------------------------|
| `AxeptioSDK.instance().initialize(activity, …)`        | `AxeptioSDK.initialize(context) { … }`, once, in `Application`   |
| `clientId`                                             | `projectId`                                                      |
| `cookiesVersion`                                       | `configId` (optional — resolved automatically when omitted)      |
| `token` (transfers an existing consent)                | Not supported. `token` is now your project's API token           |
| `AxeptioService.PUBLISHERS_TCF` / `BRANDS`             | `AxeptioService.Publisher` / `AxeptioService.Brands`             |
| `showConsentScreen(activity)`                          | `showConsentFlow(activity)` / `showConsentManager(activity)`     |
| `setEventListener` (`onPopupClosedEvent`, `onConsentSaved`, …) | Collect `consentStatusFlow`                               |
| `clearConsents()`                                      | `clearConsentData()`                                             |
| `getRemainingDaysForConsent()`                         | `getRemainingDaysForConsent(ttlDays) { … }`                      |
| `getVendorConsents()` / `isVendorConsented(id)`        | `getTcfVendorConsents { … }` (keyed by IAB vendor id) / `getBrandsVendorConsents { … }` (keyed by display name) |
| `token` / `appendAxeptioToken(uri)` for WebViews       | `getAxeptioToken { … }` (no URL helper)                          |
| `onGoogleConsentModeUpdate`                            | Not supported yet                                                |

### Not supported yet

* Google Consent Mode v2 updates
* IAB GPP strings
* A consent-change listener with the user's choices (observe `consentStatusFlow` instead)
* Sharing consent with WebViews through a URL helper
* Customizing the SDK's theme from code (the look comes from your Axeptio configuration)
* Java callers

## Example App

A sample app is included alongside the published SDK to show it in action. It is a standalone Gradle
project that consumes the SDK from the bundled Maven repository (`maven { url = uri("../maven") }`)
exactly as your own app would.

1. Clone the SDK repository:

   ```sh
   git clone https://github.com/axeptio/native-android-sdk.git
   ```

2. Open the `sampleApp/` folder in Android Studio (or run it from the command line).
3. Choose a device or emulator and **Run** the `app` configuration. It starts on a public Axeptio
   demo project; to use your own, change the defaults in
   `sampleApp/app/src/main/kotlin/io/axeptio/sample/config/AxeptioConfigManager.kt` or use the in-app
   **SDK Configuration** screen:

   ```sh
   cd sampleApp && ./gradlew :app:installDebug
   ```

The sample app shows initialization, lifecycle-aware consent status handling, the consent flow, the
consent manager, the permissions screen, the consent queries and re-initialization with new
credentials. See `sampleApp/README.md` for details.

## Support

For integration questions, bug reports or feature requests, contact Axeptio support at
**support@axeptio.eu** or visit the [help centre](https://support.axeptio.eu). Release notes are on the
[releases page](https://github.com/axeptio/native-android-sdk/releases). To report a security vulnerability, see [SECURITY.md](SECURITY.md).

## License

The Axeptio Native Android SDK is distributed under Axeptio's licensing terms — see [LICENSE](LICENSE).
