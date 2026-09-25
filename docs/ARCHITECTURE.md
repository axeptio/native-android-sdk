# Axeptio Android SDK — Architecture

Internal reference for engineers maintaining this SDK. For consumer-facing usage, see the
top-level [README.md](../README.md).

## Module layering

Dependencies flow strictly downward — confirmed via every module's `build.gradle.kts`, no
back-edges exist:

```
sdk/
  └── foundation/feature/{host, consents, permissions}
        └── foundation/data/{configuration, analytics, permissions}
              └── foundation/core/{network, datastore, config, utils, di}

foundation/design-system  (UI-only leaf, consumed by all feature/* modules)
```

- `sdk/` is the only module public consumers depend on. It pulls in every feature and data
  module, plus `core/di`, `core/config` (as `api`, since `AxeptioConfig` is part of the public
  surface), `core/datastore`, `core/network`, and `core/utils`.
- Feature modules never depend on each other directly, except `feature/host`, which orchestrates
  `feature/consents` and `feature/permissions` as the presentation shell.
- Data modules depend only on core modules — never on feature code or design-system.
- `design-system` depends only on `core/di` and `core/config` — it has no data-layer knowledge,
  confirming it's a pure Compose UI/theming layer.

## Module responsibilities

**`foundation/core/di`** — `AxContext` wraps an *isolated* Koin `KoinApplication` (built via
`koinApplication { }`, not global `startKoin`), so the SDK's DI graph never collides with a host
app's own Koin setup. `init()` builds the graph, `close()` tears it down and nulls the reference;
accessing `koin` before `init()` throws.

**`foundation/core/config`** — Pure model/config module (`AxeptioConfig`, `FlowType`,
`Environment`, `ScreenType`, permission enums). No logic — just shared value types every layer
depends on, which is why it sits below `datastore`/`network` in the graph.

**`foundation/core/network`** — Ktor HTTP client setup on an OkHttp engine, plus a `Result`-style
call wrapper (`CallResult`/`KtorCall`) and retry utilities.

**`foundation/core/datastore`** — Wraps Jetpack DataStore; a factory creates typed `DataStore<T>`
instances keyed by DI qualifiers, with a Kotlinx-JSON serializer for persistence.

**`foundation/core/utils`** — Context helpers and a logger wrapper (Kermit), exposed as a Koin
module. `axLogger()` builds the SDK's `Logger` from the domain `LogLevel` (`core/config`) set via
`AxeptioConfigBuilder.loggerLevel`: `NONE` installs no writers, `DEBUG` logs at `Severity.Debug`
(host milestones + errors) to Logcat under tag `AxeptioSDK`. Independent of that public setting,
`InternalLogging.isInternalVerboseEnabled()` checks `Log.isLoggable("AxeptioSDK", VERBOSE)` — a
device-level switch (`adb shell setprop log.tag.AxeptioSDK VERBOSE`, no public API) that unlocks
full `Severity.Verbose` output including HTTP wire logs, for SDK development only. *(Landed on
`master` via `148e1044`/`5aa7bbf9`, ahead of this branch — see [README.md](../README.md#logging)
for the consumer-facing summary.)*

**`foundation/data/configuration`** — The largest module; owns remote config fetch, caching, and
consent decisions for both flow types:
- `ConfigurationFetcher` calls the config API, resolves the active `configId` (device-language
  match for the TCF/Publisher pool, geolocation for Brands), and mints/caches the device token.
- `ConfigurationCore` (internal) is the flow-agnostic orchestrator: on `startFetch()` it applies
  any cached configuration/vendors first (offline-first UX), then fetches fresh data and
  re-caches it, falling back to cache on network failure. For the Publisher/TCF flow it also
  fetches and caches TCF "standard-info" variables *per `configId`* via `PublisherVariablesCache`
  — this is the piece touched by the most recent commits, to stop variables from one `configId`
  leaking into another after a language-based config reselection.
- `DefaultConfigurationRepository` is a thin facade implementing the public
  `ConfigurationRepository` interface; it delegates config/vendor state to `ConfigurationCore`
  and routes consent operations to either `BrandsConsentRepository` or `TcfConsentRepository`,
  selected at runtime by the resolved `FlowType`. Each derives an internal `ConsentReasonState`
  (`FIRST_TIME`, `VENDORS_CHANGED`, `CONSENT_EXPIRED`, `VALID_CONSENT`,
  `CONSENTS_SYNC_SUCCESSFUL`, `SYNC_FAILED_GIVEUP`, `CONFIG_FETCH_FAILED`) — the public API never
  sees this type directly. `ConsentStatusMapper` ([sdk/internal](sdk/src/main/kotlin/io/axeptio/sdk/internal/ConsentStatusMapper.kt))
  is the seam that collapses those 7 reasons down to the 3 cases of the public `ConsentStatus`
  sealed class: `FIRST_TIME`/`VENDORS_CHANGED`/`CONSENT_EXPIRED` → `Ready(shouldDisplayConsents =
  true)`; `VALID_CONSENT`/`CONSENTS_SYNC_SUCCESSFUL`/`SYNC_FAILED_GIVEUP` → `Ready(false)`
  (sync-outcome states never required distinct host action, so they fold into the same "no action
  needed" case); `CONFIG_FETCH_FAILED` → `ConfigFetchFailed`. The sealed class (replacing a plain
  enum) lets `Ready` carry `shouldDisplayConsents` as a direct payload instead of a separate query.
- `ConsentSyncManager`/`TcfConsentSyncManager` push consent decisions to the backend with
  exponential backoff, up to 10 attempts capped at 300s.

**`foundation/data/analytics`** — Event reporting. `AnalyticsRepository.logEvent()` is
fire-and-forget: `AnalyticsContextProvider` snapshots whatever the SDK currently knows (flow type
→ `source`, project/config ids, user token, the current consent state as
`preferences.enabled`/`disabled`, app label, package name, user agent), `AnalyticsEventFactory`
turns that plus the event type into an `AnalyticsEventJson`, and it lands in
`AnalyticsEventQueue` — a DataStore-backed FIFO on disk, capped at 500 events, so nothing is lost
while offline or across process death. `AnalyticsEventDispatcher` drains it in batches of 20 to
`POST mobile/analytics/evts` every 60s, on `app:close`, and when the consent flow closes; a batch
leaves the queue only once accepted, except on a 4xx, which is dropped rather than retried
forever behind every later event. Event names match the webview SDKs (`cookies:open`,
`cookies:consent:accept|reject|partial`, `cookies:vendors:toggle*`, `app:open`/`app:close`) so
both SDKs feed the same downstream reports; `source` is `sdk-android-tcf` or `sdk-android-brands`.
Events are fired from the consent ViewModels and, for app lifecycle, by `AppLifecycleTracker` on
`ProcessLifecycleOwner`, started from `initAxModule()` via `startAnalytics()`.

**`foundation/data/permissions`** — Small, single-purpose repository pairing a Ktor-backed API
with a DataStore-backed store, following the same Default/Fake-implementation Koin pattern as
`configuration`.

**`foundation/feature/host`** — The presentation orchestrator. Exposes
`launchFirstRunStartWithConsents`/`launchConsentManager`/`launchPermissionsManager`, each starting
`HostActivity` (a `ComponentActivity`) with an `EntryMode` extra. `HostActivity` is fully Jetpack
Compose (`setContent { App(...) }`), providing `LocalActivity`/`LocalImageLoader` composition
locals. A `HostViewModel` drives loading/ready/error UI state, and a nav host composes the actual
consent/permission screens based on the resolved screen sequence.

**`foundation/feature/consents`**, **`foundation/feature/permissions`** — Compose screens and
view models for Brands consent details, Publisher/TCF purpose/vendor screens, and OS permission
requests, built on `design-system` components.

**`foundation/design-system`** — Pure Compose UI kit: buttons, cards, icons, permission
illustrations, the app theme, and URI-handling utilities. No business logic; consumed by all
three feature modules.

## DI lifecycle

`AxeptioSDK.initialize()` calls `initAxModule()`, which builds the isolated `AxContext` Koin
graph from every module's DI module, then eagerly fetches config (`ConfigurationRepository
.startFetch()`). `AxeptioSDK.shutdown()` detaches reactive flows (`SdkRuntime.onShutdown()`)
before calling `AxContext.close()` to tear the graph down. Public flows (`consentStatusFlow`)
attach to a `StateFlow<ConfigurationRepository?>` in `SdkRuntime` rather than the repository
directly, so they survive init/shutdown cycles without re-subscription.

## End-to-end data flow

```
initialize()
  → initAxModule()              build isolated Koin graph
  → ConfigurationRepository.startFetch()
      → apply cached config/vendors (offline-first)
      → fetch fresh config + vendors/TCF-variables over core/network
      → re-cache via core/datastore (ConfigurationCache, VendorsByCategoryCache,
        PublisherVariablesCache — scoped per configId)
      → on failure with no cache available: ConsentReasonState.CONFIG_FETCH_FAILED,
        terminal for this session — no background retry, host must re-initialize
  → BrandsConsentRepository / TcfConsentRepository derive ConsentReasonState
  → ConsentStatusMapper collapses it to the public ConsentStatus
    (Ready(shouldDisplayConsents) / NotInitialized / ConfigFetchFailed)
  → AxeptioSDK.consentStatusFlow emits

showConsentFlow() / showConsentManager() / showPermissionsScreen()
  → HostActivity (Compose) renders feature/consents | feature/permissions screens,
    styled by design-system

user submits consent
  → postConsents() / postTcfConsents()
  → persisted locally, then synced via ConsentSyncManager / TcfConsentSyncManager
    (retry with exponential backoff, up to 10 attempts, capped at 300s)

shutdown()
  → SdkRuntime.onShutdown() detaches flows
  → AxContext.close() tears down the Koin graph
```
