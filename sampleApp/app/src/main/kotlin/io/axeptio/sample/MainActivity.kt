package io.axeptio.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.axeptio.sample.config.ConfigRepository
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.model.ConsentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ConfigRepository.create(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainContent(
                        activity = this@MainActivity,
                        repository = repository,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    activity: ComponentActivity,
    repository: ConfigRepository,
) {
    val savedConfig = remember { repository.load() }
    var currentConfig by remember { mutableStateOf(savedConfig) }
    var showConfigSheet by remember { mutableStateOf(currentConfig == null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        AxeptioSDK.consentStatusFlow
            .collect { status ->
                Log.d("SDKConfigurer", "Consent status updated: $status")

                when (status) {
                    ConsentStatus.FIRST_TIME -> {
                        activity.showToast(
                            "User has not given consent yet. Show the consent flow to " +
                                "collect their preferences."
                        )

                        AxeptioSDK.showConsentFlow(activity)
                    }

                    ConsentStatus.VENDORS_CHANGED -> {
                        activity.showToast(
                            "One or more vendors have changed since the user last gave consent. " +
                                "Please review and update your consent preferences."
                        )

                        AxeptioSDK.showConsentManager(activity)
                    }

                    ConsentStatus.CONSENT_EXPIRED -> {
                        activity.showToast(
                            "User's consent has expired. Please review and update your consent preferences."
                        )

                        AxeptioSDK.showConsentManager(activity)
                    }

                    ConsentStatus.SYNC_FAILED_GIVEUP -> {
                        Log.e(
                            "SDKConfigurer",
                            "Failed to sync consent status with server after multiple attempts. " +
                                "User's consent status may be outdated."
                        )
                    }

                    ConsentStatus.CONSENTS_SYNC_SUCCESSFUL -> {
                        Log.d(
                            "SDKConfigurer",
                            "Consents synchronized successfully with Axeptio servers."
                        )
                    }

                    ConsentStatus.VALID_CONSENT -> {
                        Log.d(
                            "SDKConfigurer",
                            "User's consent is valid and up to date. No action needed."
                        )
                    }

                    ConsentStatus.NOT_INITIALIZED -> {
                        Log.d(
                            "SDKConfigurer",
                            "SDK not initialized yet; waiting for initialization to complete"
                        )
                    }

                    ConsentStatus.CONFIG_FETCH_FAILED -> {
                        Log.e(
                            "SDKConfigurer",
                            "Failed to fetch configuration from server. " +
                                "User may not be prompted for consent until this is resolved."
                        )
                    }
                }
            }
    }

    SampleScreen(
        onShowConsentFlow = {
            AxeptioSDK.showConsentFlow(activity)
        },
        onShowConsentManager = {
            AxeptioSDK.showConsentManager(activity)
        },
        onShowPermissions = {
            AxeptioSDK.showPermissionsScreen(activity)
        },
        onShowConfig = { showConfigSheet = true },
        onClearConsentData = {
            AxeptioSDK.clearConsentData { result ->
                activity.lifecycleScope.launch {
                    try {
                        val success = result.getOrThrow()
                        if (success) {
                            activity.showToast("Consent data cleared successfully")
                        } else {
                            activity.showToast("Failed to clear consent data")
                        }
                    } catch (e: Exception) {
                        activity.showToast("Error clearing consent data: ${e.message}")
                    }
                }
            }
        }
    )

    if (showConfigSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showConfigSheet = false }
        ) {
            ConfigScreen(
                initialConfig = currentConfig ?: repository.getDefault(),
                onConfigSaved = { config ->
                    repository.save(config)
                    restartApp(activity)
                }
            )
        }
    }
}

/**
 * Restarts the app to re-initialize the SDK with the new configuration.
 * NOTE: This is a sample-app specific utility and not a recommended pattern
 * for production apps. In a real app, you would typically handle configuration
 * changes more gracefully or use a library like ProcessPhoenix for reliable restarts.
 */
private fun restartApp(activity: ComponentActivity) {
    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finish()
        Runtime.getRuntime().exit(0)
    }
}

private suspend fun Context.showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
    withContext(Dispatchers.Main.immediate) {
        Toast.makeText(this@showToast, message, duration).show()
    }
}

@Composable
private fun SampleScreen(
    onShowConsentFlow: () -> Unit,
    onShowConsentManager: () -> Unit,
    onShowPermissions: () -> Unit,
    onShowConfig: () -> Unit,
    onClearConsentData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.sample_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onShowConsentFlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_show_consent_flow))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onShowConsentManager,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_open_consent_manager))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onShowPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_open_permissions_screen))
        }

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = onShowConfig,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_sdk_configuration))
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onClearConsentData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_sdk_clear_consents))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SampleScreenPreview() {
    SampleScreen(
        onShowConsentFlow = {},
        onShowConsentManager = {},
        onShowPermissions = {},
        onShowConfig = {},
        onClearConsentData = {},
    )
}
