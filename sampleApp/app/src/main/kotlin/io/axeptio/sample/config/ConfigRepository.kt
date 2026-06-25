package io.axeptio.sample.config

import android.content.Context

class ConfigRepository(private val configManager: AxeptioConfigManager) {

    fun save(config: AppConfig): AppConfig {
        configManager.saveConfig(config)
        return config
    }

    fun load(): AppConfig? = configManager.loadConfig()

    fun getDefault(): AppConfig = configManager.getDefaultConfig()

    fun hasConfig(): Boolean = configManager.hasConfig()

    companion object {
        fun create(context: Context) = ConfigRepository(AxeptioConfigManager(context))
    }
}
