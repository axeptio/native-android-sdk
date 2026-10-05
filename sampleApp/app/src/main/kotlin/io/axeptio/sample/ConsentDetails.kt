package io.axeptio.sample

import android.content.Context
import android.os.Looper
import io.axeptio.sdk.AxeptioSDK
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Snapshot of what the SDK currently stores, read through its public query API.
 * Each field holds either the value or the error message of its query.
 */
data class ConsentDetails(
    /** "main" when every query called back on the main thread, as the SDK guarantees. */
    val callbackThread: String,
    val remainingDays: String,
    val axeptioToken: String,
    val brandsVendorConsents: String,
    val tcfTcString: String,
    val tcfVendorConsents: String,
    val iabTcfGdprApplies: String,
    /**
     * The Google Consent Mode v2 signals, or "none": Consent Mode is off for the configuration, or
     * no consent with signals is stored.
     */
    val googleConsentMode: String,
)

private class Answer(val text: String, val onMainThread: Boolean)

/**
 * The SDK's query functions deliver their result through a callback on the main thread.
 * Wrapping them in [suspendCancellableCoroutine] lets you call them sequentially from a
 * coroutine.
 */
private suspend fun <T> query(block: ((Result<T>) -> Unit) -> Unit): Answer =
    suspendCancellableCoroutine { continuation ->
        block { result ->
            val text = result.fold(
                onSuccess = { it?.toString() ?: "none" },
                onFailure = { "error: ${it.message ?: it::class.simpleName}" },
            )
            continuation.resume(Answer(text, onMainThread = Looper.myLooper() == Looper.getMainLooper()))
        }
    }

suspend fun loadConsentDetails(context: Context): ConsentDetails {
    // In the Publisher flow the SDK also writes the standard IABTCF_* keys to the default
    // SharedPreferences, where third-party ad SDKs read them.
    val defaultPrefs = context.getSharedPreferences(
        "${context.packageName}_preferences",
        Context.MODE_PRIVATE,
    )
    val remainingDays = query { AxeptioSDK.getRemainingDaysForConsent(callback = it) }
    val axeptioToken = query(AxeptioSDK::getAxeptioToken)
    val brandsVendorConsents = query(AxeptioSDK::getBrandsVendorConsents)
    val tcfTcString = query(AxeptioSDK::getTcfTcString)
    val tcfVendorConsents = query(AxeptioSDK::getTcfVendorConsents)
    val googleConsentMode = query { callback ->
        AxeptioSDK.getGoogleConsentMode { result ->
            callback(result.map { consent -> consent?.let(SampleEventLogger::describe) ?: "none" })
        }
    }
    val answers =
        listOf(remainingDays, axeptioToken, brandsVendorConsents, tcfTcString, tcfVendorConsents, googleConsentMode)
    return ConsentDetails(
        callbackThread = if (answers.all { it.onMainThread }) "main" else "background",
        remainingDays = remainingDays.text,
        axeptioToken = axeptioToken.text,
        brandsVendorConsents = brandsVendorConsents.text,
        tcfTcString = tcfTcString.text,
        tcfVendorConsents = tcfVendorConsents.text,
        iabTcfGdprApplies = defaultPrefs.all["IABTCF_gdprApplies"]?.toString() ?: "none",
        googleConsentMode = googleConsentMode.text,
    )
}
