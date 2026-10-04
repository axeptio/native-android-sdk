package io.axeptio.sample

import io.axeptio.sdk.model.ConsentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime

/**
 * The events the sample's Events panel shows, newest first. Fed by [SampleEventLogger].
 *
 * [now] stamps each entry; tests pass a fixed clock.
 */
class EventLog(private val now: () -> LocalTime = LocalTime::now) {

    /** An event as the Events panel shows it: what happened, and when. */
    data class Entry(val message: String, val time: LocalTime)

    private val recorded = MutableStateFlow<List<Entry>>(emptyList())
    private var lastStatus: ConsentStatus? = null

    /** The recorded events, newest first, at most [MaxEntries]. */
    val entries: StateFlow<List<Entry>> = recorded.asStateFlow()

    /**
     * Adds [message] as the newest entry, on one line: the panel describes itself with its
     * entries, one per line.
     */
    fun add(message: String) {
        val entry = Entry(message.replace('\n', ' '), now())
        recorded.update { (listOf(entry) + it).take(MaxEntries) }
    }

    /**
     * Adds `Consent status: <status>` when [status] differs from the last one added: the sample
     * collects the status again at every start, which repeats the current one. Call it on the main
     * thread.
     */
    fun addStatus(status: ConsentStatus) {
        if (status == lastStatus) return
        lastStatus = status
        add("Consent status: $status")
    }

    companion object {
        /** The panel renders every entry at once: only the newest ones are kept. */
        const val MaxEntries = 50
    }
}
