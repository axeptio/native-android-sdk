package io.axeptio.sample

import io.axeptio.sample.config.AppConfig
import io.axeptio.sdk.configuration.AxeptioEnvironment
import io.axeptio.sdk.configuration.AxeptioService

internal data class ConfigFormState(
    val projectId: String,
    val appVersion: String,
    val token: String,
    val configId: String,
    val targetService: AxeptioService,
    val environment: AxeptioEnvironment,
) {
    fun toAppConfig(): AppConfig = AppConfig(
        projectId = projectId,
        appVersion = appVersion,
        token = token.ifBlank { null },
        configId = configId.ifBlank { null },
        targetService = targetService,
        environment = environment,
    )

    companion object {
        fun from(config: AppConfig) = ConfigFormState(
            projectId = config.projectId,
            appVersion = config.appVersion,
            token = config.token ?: "",
            configId = config.configId ?: "",
            targetService = config.targetService,
            environment = config.environment,
        )
    }
}
