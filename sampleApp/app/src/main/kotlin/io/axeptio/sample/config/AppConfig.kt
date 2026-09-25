package io.axeptio.sample.config

import io.axeptio.sdk.configuration.AxeptioEnvironment
import io.axeptio.sdk.configuration.AxeptioService

/**
 * App-level config holder. Lives in the sample app, not in the SDK.
 * Decouples the sample UI from SDK internal types.
 */
data class AppConfig(
    val projectId: String,
    val appVersion: String,
    val token: String? = null,
    val configId: String? = null,
    val targetService: AxeptioService = AxeptioService.Brands,
    val environment: AxeptioEnvironment = AxeptioEnvironment.Production,
)
