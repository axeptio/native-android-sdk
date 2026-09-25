package io.axeptio.sample.config

import android.content.Context
import android.util.Log
import io.axeptio.foundation.core.config.AxeptioPermission
import io.axeptio.sample.R
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.model.AxeptioLogLevel

object SDKConfigurer {

    private fun Context.getSupportedPermissions() = listOf(
        AxeptioPermission.Camera(),
        AxeptioPermission.Calendar(),
        AxeptioPermission.Bluetooth(),
        AxeptioPermission.Contacts(),
        AxeptioPermission.PhotoLibrary(),
        AxeptioPermission.BodySensors(),
        AxeptioPermission.PhoneAccount(),
        AxeptioPermission.Fitness(),
        AxeptioPermission.LocationFine(
            title = getString(R.string.permission_location_title),
            description = getString(R.string.permission_location_description),
        ),
        AxeptioPermission.Microphone(
            title = getString(R.string.permission_microphone_title),
            description = getString(R.string.permission_microphone_description)
        ),
        AxeptioPermission.Notifications(
            title = getString(R.string.permission_notifications_title),
            description = getString(R.string.permission_notifications_description)
        )
    )

    fun initialize(applicationContext: Context, config: AppConfig) {
        AxeptioSDK.initialize(
            context = applicationContext,
            configure = {
                projectId = config.projectId
                appVersion = config.appVersion
                token = config.token
                configId = config.configId
                targetService = config.targetService
                environment = config.environment
                loggerLevel = AxeptioLogLevel.DEBUG
                withPermissions(applicationContext.getSupportedPermissions())
            }
        )

        Log.d("SDKConfigurer", "SDK initialized with projectId=${config.projectId}")
    }
}
