# Axeptio Native Android SDK — sample app

A small Jetpack Compose app that integrates the SDK exactly as your app would: one Maven repository
and one dependency (`io.axeptio:sdk`). See the [SDK README](../README.md) for the full integration
guide.

## Requirements

- Android Studio with support for Android Gradle Plugin 9.1
- JDK 21
- A device or emulator running Android 13 (API 33) or later

## Run it

Open this `sampleApp/` folder in Android Studio and run the `app` configuration, or from a terminal:

```sh
./gradlew :app:installDebug
```

The SDK is resolved from the `maven/` folder next to this project (`../maven`), so run it from a
clone of the whole repository. The SDK version is set by `axeptioVersion` in `gradle.properties`.

The `release` build type is minified with R8 (`./gradlew :app:assembleRelease`); it uses the debug
signing config so it still installs on a plain emulator/device, and needs no extra ProGuard rules —
the SDK ships its own (see the [SDK README](../README.md#r8--proguard)).

## Credentials

The app starts on a public Axeptio demo project (Brands flow), so it works without any setup. The
defaults live in `app/src/main/kotlin/io/axeptio/sample/config/AxeptioConfigManager.kt`.

To try your own project, tap **SDK Configuration**, enter your project ID, app version, token and
optional configuration ID, pick the service (Brands or Publisher) and environment, then tap
**Save & Initialize SDK**. The app shuts the SDK down and initializes it again with the new values;
they are kept across launches.

## What it shows

| Where                                       | What it demonstrates                                                                 |
|---------------------------------------------|--------------------------------------------------------------------------------------|
| `SampleApplication`                         | Initializing the SDK once, in `Application.onCreate()`                               |
| `config/SDKConfigurer`                      | The initialization options, logging, and the Android permissions the SDK can request |
| `MainActivity.observeConsentStatus`         | Collecting `consentStatusFlow` with `repeatOnLifecycle`, and showing the consent flow once when consent is required |
| `SampleEventLogger`                         | An `AxeptioEventListener` registered before `initialize()`: flow closed, consents updated, errors |
| **Events** (`EventLogPanel`, `EventLog`)    | The events above and each new consent status, newest first                           |
| **Show flow starting with Consents**        | `AxeptioSDK.showConsentFlow`                                                         |
| **Open Consent Manager**                    | `AxeptioSDK.showConsentManager`, for users who want to change their choices          |
| **Open Permissions Screen**                 | `AxeptioSDK.showPermissionsScreen`                                                   |
| **Show consent details** (`ConsentDetails`) | The query API (remaining days, Axeptio token, vendor consents, TC string) wrapped as coroutines, and the `IABTCF_*` values in the default `SharedPreferences` |
| **SDK Configuration**                       | Re-initializing with `AxeptioSDK.shutdown()` + `initialize()`                        |
| **Clear local consents**                    | `AxeptioSDK.clearConsentData()`; the consent flow then shows again                   |

The manifest declares every permission the sample passes to `withPermissions()`; your app only needs
the permissions it actually requests — the SDK declares `INTERNET` itself.
