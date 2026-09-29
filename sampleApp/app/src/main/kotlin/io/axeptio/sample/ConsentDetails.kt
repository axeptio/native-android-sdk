package io.axeptio.sample

import android.content.Context
import io.axeptio.sdk.AxeptioSDK
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Snapshot of what the SDK currently stores, read through its public query API.
 * Each field holds either the value or the error message of its query.
 */
data class ConsentDetails(
    val remainingDays: String,
    val axeptioToken: String,
    val brandsVendorConsents: String,
    val tcfTcString: String,
    val tcfVendorConsents: String,
    val iabTcfGdprApplies: String,
)

/**
 * The SDK's query functions deliver their result through a callback on a background thread.
 * Wrapping them in [suspendCancellableCoroutine] lets you call them sequentially from a
 * coroutine and resume on the caller's dispatcher (the main thread here).
 */
private suspend fun <T> query(block: ((Result<T>) -> Unit) -> Unit): String =
    suspendCancellableCoroutine { continuation ->
        block { result ->
            continuation.resume(
                result.fold(
                    onSuccess = { it?.toString() ?: "none" },
                    onFailure = { "error: ${it.message ?: it::class.simpleName}" },
                )
            )
        }
    }

suspend fun loadConsentDetails(context: Context): ConsentDetails {
    // In the Publisher flow the SDK also writes the standard IABTCF_* keys to the default
    // SharedPreferences, where third-party ad SDKs read them.
    val defaultPrefs = context.getSharedPreferences(
        "${context.packageName}_preferences",
        Context.MODE_PRIVATE,
    )
    return ConsentDetails(
        remainingDays = query { AxeptioSDK.getRemainingDaysForConsent(callback = it) },
        axeptioToken = query(AxeptioSDK::getAxeptioToken),
        brandsVendorConsents = query(AxeptioSDK::getBrandsVendorConsents),
        tcfTcString = query(AxeptioSDK::getTcfTcString),
        tcfVendorConsents = query(AxeptioSDK::getTcfVendorConsents),
        iabTcfGdprApplies = defaultPrefs.all["IABTCF_gdprApplies"]?.toString() ?: "none",
    )
}
