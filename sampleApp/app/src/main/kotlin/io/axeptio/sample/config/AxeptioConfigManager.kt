package io.axeptio.sample.config

import android.content.Context
import android.content.SharedPreferences
import io.axeptio.sdk.configuration.AxeptioEnvironment
import io.axeptio.sdk.configuration.AxeptioService

class AxeptioConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("axeptio_config", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_PROJECT_ID = "69dfafa35cda9feae7d2947a"
        const val DEFAULT_TOKEN = "project_69dfafa35cda9feae7d2947a_pro_3afbe616928ee6e4462aed560c5f2098"
        const val DEFAULT_APP_VERSION = "1.2.3"

        private const val KEY_PROJECT_ID = "projectId"
        private const val KEY_APP_VERSION = "appVersion"
        private const val KEY_TARGET_SERVICE = "targetService"
        private const val KEY_TOKEN = "token"
        private const val KEY_ENVIRONMENT = "environment"
        private const val KEY_CONFIG_ID = "configId"

        private val DEFAULT_TARGET_SERVICE_ENUM = AxeptioService.Brands

        inline fun <reified T : Enum<T>> enumValueOrNull(name: String?): T? {
            if (name.isNullOrBlank()) return null
            return runCatching { enumValueOf<T>(name) }.getOrNull()
        }
    }

    fun saveConfig(config: AppConfig) {
        prefs.edit().also {
            it.putString(KEY_PROJECT_ID, config.projectId)
            it.putString(KEY_APP_VERSION, config.appVersion)
            it.putString(KEY_TARGET_SERVICE, config.targetService.name)
            it.putString(KEY_TOKEN, config.token)
            it.putString(KEY_ENVIRONMENT, config.environment.name)
            it.putString(KEY_CONFIG_ID, config.configId)
            it.commit()
        }
    }

    fun loadConfig(): AppConfig? {
        val projectId = prefs.getString(KEY_PROJECT_ID, null) ?: return null
        val appVersion = prefs.getString(KEY_APP_VERSION, null) ?: return null
        val targetServiceName = prefs.getString(KEY_TARGET_SERVICE, null)
        val token = prefs.getString(KEY_TOKEN, null)
        val environmentName = prefs.getString(KEY_ENVIRONMENT, null)
        val configId = prefs.getString(KEY_CONFIG_ID, null)

        val targetService =
            enumValueOrNull<AxeptioService>(targetServiceName) ?: DEFAULT_TARGET_SERVICE_ENUM
        val environment =
            enumValueOrNull<AxeptioEnvironment>(environmentName) ?: AxeptioEnvironment.Production

        return AppConfig(
            projectId = projectId,
            appVersion = appVersion,
            targetService = targetService,
            token = token,
            environment = environment,
            configId = configId,
        )
    }

    fun getDefaultConfig(): AppConfig = AppConfig(
        projectId = DEFAULT_PROJECT_ID,
        token = DEFAULT_TOKEN,
        appVersion = DEFAULT_APP_VERSION,
        targetService = DEFAULT_TARGET_SERVICE_ENUM,
        environment = AxeptioEnvironment.Production,
    )

    fun hasConfig(): Boolean = prefs.getString(KEY_PROJECT_ID, null) != null
}
