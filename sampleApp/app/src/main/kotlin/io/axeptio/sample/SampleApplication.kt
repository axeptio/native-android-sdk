package io.axeptio.sample

import android.app.Application
import io.axeptio.sample.config.ConfigRepository
import io.axeptio.sample.config.SDKConfigurer

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val repository = ConfigRepository.create(this)
        val initialConfig = if (repository.hasConfig()) {
            repository.load()
        } else {
            repository.getDefault().also { repository.save(it) }
        }
        initialConfig?.let { SDKConfigurer.initialize(applicationContext, it) }
    }
}
