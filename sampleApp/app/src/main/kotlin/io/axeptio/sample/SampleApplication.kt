package io.axeptio.sample

import android.app.Application
import io.axeptio.sample.config.ConfigRepository
import io.axeptio.sample.config.SDKConfigurer
import io.axeptio.sdk.AxeptioSDK

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val repository = ConfigRepository.create(this)
        // Initialize once, as early as possible. The sample starts on the demo project's
        // credentials until you save your own from the SDK Configuration screen.
        val config = repository.load() ?: repository.getDefault()
        // Before initialize(), so the listener also hears about a failed configuration fetch.
        AxeptioSDK.setEventListener(SampleEventLogger)
        SDKConfigurer.initialize(applicationContext, config)
    }
}
