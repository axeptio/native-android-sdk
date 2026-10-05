package io.axeptio.sample

import android.util.Log
import io.axeptio.sdk.AxeptioEventListener
import io.axeptio.sdk.model.AxeptioError
import io.axeptio.sdk.model.ConsentStatus
import io.axeptio.sdk.model.GoogleConsentStatus
import io.axeptio.sdk.model.GoogleConsentV2
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

    /**
     * An app using Firebase forwards [consent] here: `Firebase.analytics.setConsent(...)` (see the
     * README). The sample has no Firebase: it records the signals, G for granted, D for denied.
     */
    override fun onGoogleConsentModeUpdate(consent: GoogleConsentV2) {
        val signals = describe(consent)
        Log.d(TAG, "Event: google consent $signals")
        log.add("Google consent: $signals")
    }

    /** `analytics=G ad_storage=D ad_user_data=D ad_personalization=D`: stable, for the tests. */
    fun describe(consent: GoogleConsentV2): String =
        "analytics=${consent.analyticsStorage.letter()} ad_storage=${consent.adStorage.letter()} " +
            "ad_user_data=${consent.adUserData.letter()} ad_personalization=${consent.adPersonalization.letter()}"

    private fun GoogleConsentStatus.letter() = if (this == GoogleConsentStatus.GRANTED) "G" else "D"

    override fun onError(error: AxeptioError) {
        Log.w(TAG, "Event: error $error - ${error.message}")
        log.add("Error: ${error.message}")
    }

    /** Records [status] when it changed; see [EventLog.addStatus]. Call it on the main thread. */
    fun onConsentStatus(status: ConsentStatus) {
        log.addStatus(status)
    }
}
