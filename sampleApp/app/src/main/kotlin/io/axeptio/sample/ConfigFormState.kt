package io.axeptio.sample

import io.axeptio.sample.config.AppConfig
import io.axeptio.sdk.configuration.AxeptioEnvironment
import io.axeptio.sdk.configuration.AxeptioService
import io.axeptio.sdk.configuration.AxeptioThemeMode

internal data class ConfigFormState(
    val projectId: String,
    val appVersion: String,
    val token: String,
    val configId: String,
    val targetService: AxeptioService,
    val themeMode: AxeptioThemeMode,
    val environment: AxeptioEnvironment,
) {
    fun toAppConfig(): AppConfig = AppConfig(
        projectId = projectId,
        appVersion = appVersion,
        token = token.ifBlank { null },
        configId = configId.ifBlank { null },
        targetService = targetService,
        forceThemeMode = themeMode,
        environment = environment,
    )

    companion object {
        fun from(config: AppConfig) = ConfigFormState(
            projectId = config.projectId,
            appVersion = config.appVersion,
            token = config.token ?: "",
            configId = config.configId ?: "",
            targetService = config.targetService,
            themeMode = config.forceThemeMode ?: AxeptioThemeMode.Light,
            environment = config.environment,
        )
    }
}
