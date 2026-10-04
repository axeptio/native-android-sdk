package io.axeptio.sample

import android.util.Log
import io.axeptio.sdk.AxeptioEventListener
import io.axeptio.sdk.model.AxeptioError
import io.axeptio.sdk.model.ConsentStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Records what the SDK reports, to show both event APIs at work: an [AxeptioEventListener] and
 * the consent status [MainActivity] collects. Each event is logged and added to [entries], which
 * the sample's Events panel shows. Registered once by [SampleApplication], before the SDK is
 * initialized, so the configuration fetch's errors are recorded too; it lives as long as the app,
 * so it never needs removing. The emulator tests match both the log lines and the entries
 * (docs/testing/EMULATOR_TESTS.md): keep them stable.
 */
object SampleEventLogger : AxeptioEventListener {

    private const val TAG = "AxeptioSample"

    private val log = EventLog()

    /** The recorded events, newest first. */
    val entries: StateFlow<List<EventLog.Entry>> = log.entries

    override fun onConsentFlowClosed() {
        Log.d(TAG, "Event: consent flow closed")
        log.add("Consent flow closed")
    }

    override fun onConsentsUpdated() {
        Log.d(TAG, "Event: consents updated")
        log.add("Consents updated")
    }

    override fun onError(error: AxeptioError) {
        Log.w(TAG, "Event: error $error - ${error.message}")
        log.add("Error: ${error.message}")
    }

    /** Records [status] when it changed; see [EventLog.addStatus]. Call it on the main thread. */
    fun onConsentStatus(status: ConsentStatus) {
        log.addStatus(status)
    }
}
