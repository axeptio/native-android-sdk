package io.axeptio.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The panel's test tag, which `scripts/emu-harness.sh events` finds it by. */
const val EventLogTag = "event_log"

/**
 * The SDK events recorded by [SampleEventLogger], newest first.
 *
 * One accessibility node for the whole log, described by its entries one per line: the UI tree
 * the emulator tests read only holds what is on screen, so entries below it would be missing.
 */
@Composable
fun EventLogPanel(
    entries: List<EventLog.Entry>,
    modifier: Modifier = Modifier,
) {
    val empty = stringResource(R.string.events_empty)
    val description = if (entries.isEmpty()) empty else entries.joinToString("\n") { it.message }
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.events_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    testTag = EventLogTag
                    contentDescription = description
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = empty,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            entries.forEach { entry ->
                Column {
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = entry.time.format(timeFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
