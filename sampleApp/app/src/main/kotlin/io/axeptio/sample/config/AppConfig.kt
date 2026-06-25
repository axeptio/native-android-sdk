package io.axeptio.sample.config

import io.axeptio.sdk.configuration.AxeptioEnvironment
import io.axeptio.sdk.configuration.AxeptioService
import io.axeptio.sdk.configuration.AxeptioThemeMode

/**
 * App-level config holder. Lives in the sample app, not in the SDK.
 * Decouples the sample UI from SDK internal types.
 */
data class AppConfig(
    val projectId: String,
    val appVersion: String,
    val token: String? = null,
    val configId: String? = null,
    // Parameters for test purposes only
    val targetService: AxeptioService = AxeptioService.Brands, // Will be fetched from backend
    val forceThemeMode: AxeptioThemeMode? = null, // Will be fetched from backend
    val environment: AxeptioEnvironment = AxeptioEnvironment.Production,
)
