package io.axeptio.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.axeptio.sample.config.ConfigRepository
import io.axeptio.sample.config.SDKConfigurer
import io.axeptio.sample.theme.SampleAppTheme
import io.axeptio.sdk.AxeptioSDK
import io.axeptio.sdk.model.ConsentStatus
import kotlinx.coroutines.launch

private const val TAG = "AxeptioSample"
private const val KeyConsentFlowLaunched = "consentFlowLaunched"

class MainActivity : ComponentActivity() {

    // Whether the consent flow was already launched for the current "consent required" status.
    // Saved across recreation (e.g. rotation) so the flow is not launched a second time.
    private var consentFlowLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consentFlowLaunched = savedInstanceState?.getBoolean(KeyConsentFlowLaunched) ?: false
        observeConsentStatus()

        val repository = ConfigRepository.create(this)
        setContent {
            SampleAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainContent(
                        activity = this@MainActivity,
                        repository = repository,
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KeyConsentFlowLaunched, consentFlowLaunched)
    }

    /**
     * Collects the consent status only while the activity is visible, and shows the consent
     * flow when the SDK reports that consent is required.
     */
    private fun observeConsentStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AxeptioSDK.consentStatusFlow.collect(::onConsentStatus)
            }
        }
    }

    private fun onConsentStatus(status: ConsentStatus) {
        Log.d(TAG, "Consent status updated: $status")
        when (status) {
            is ConsentStatus.Ready -> if (status.shouldDisplayConsents) {
                // New user, changed vendors or expired consent.
                if (!consentFlowLaunched) {
                    consentFlowLaunched = true
                    AxeptioSDK.showConsentFlow(this)
                }
            } else {
                // Consent is stored and current: consent-dependent SDKs can start here.
                consentFlowLaunched = false
            }

            // Before initialize(), or after shutdown() while the SDK is re-initialized.
            is ConsentStatus.NotInitialized -> consentFlowLaunched = false

            // The SDK retries on the next initialization.
            is ConsentStatus.ConfigFetchFailed -> Log.w(TAG, "Could not fetch the configuration")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    activity: ComponentActivity,
    repository: ConfigRepository,
) {
    val scope = rememberCoroutineScope()
    var currentConfig by remember { mutableStateOf(repository.load() ?: repository.getDefault()) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var consentDetails by remember { mutableStateOf<ConsentDetails?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SampleScreen(
        consentDetails = consentDetails,
        actions = SampleActions(
            onShowConsentFlow = {
                AxeptioSDK.showConsentFlow(activity)
            },
            onShowConsentManager = {
                AxeptioSDK.showConsentManager(activity)
            },
            onShowPermissions = {
                AxeptioSDK.showPermissionsScreen(activity)
            },
            onShowConsentDetails = {
                scope.launch { consentDetails = loadConsentDetails(activity) }
            },
            onShowConfig = { showConfigSheet = true },
            onClearConsentData = {
                scope.launch {
                    val cleared = AxeptioSDK.clearConsentData()
                    Log.d(TAG, "Consent data cleared: $cleared")
                    consentDetails = null
                }
            },
        ),
    )

    if (showConfigSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showConfigSheet = false }
        ) {
            ConfigScreen(
                initialConfig = currentConfig,
                onConfigSaved = { config ->
                    repository.save(config)
                    // Re-initializing requires a shutdown first; consentStatusFlow keeps
                    // emitting across both calls, so the collector above needs no change.
                    AxeptioSDK.shutdown()
                    SDKConfigurer.initialize(activity.applicationContext, config)
                    currentConfig = config
                    consentDetails = null
                    showConfigSheet = false
                }
            )
        }
    }
}

private data class SampleActions(
    val onShowConsentFlow: () -> Unit = {},
    val onShowConsentManager: () -> Unit = {},
    val onShowPermissions: () -> Unit = {},
    val onShowConsentDetails: () -> Unit = {},
    val onShowConfig: () -> Unit = {},
    val onClearConsentData: () -> Unit = {},
)

@Composable
private fun SampleScreen(
    consentDetails: ConsentDetails?,
    actions: SampleActions,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
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
            onClick = actions.onShowConsentFlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_show_consent_flow))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = actions.onShowConsentManager,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_open_consent_manager))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = actions.onShowPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_open_permissions_screen))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = actions.onShowConsentDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_show_consent_details))
        }

        consentDetails?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.consent_details,
                    it.remainingDays,
                    it.axeptioToken,
                    it.brandsVendorConsents,
                    it.tcfTcString,
                    it.tcfVendorConsents,
                    it.iabTcfGdprApplies,
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = actions.onShowConfig,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_sdk_configuration))
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = actions.onClearConsentData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_sdk_clear_consents))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SampleScreenPreview() {
    SampleScreen(consentDetails = null, actions = SampleActions())
}
