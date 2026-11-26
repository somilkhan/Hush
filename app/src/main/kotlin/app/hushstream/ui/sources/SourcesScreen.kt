package app.hushstream.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.hushstream.core.domain.addon.AddonRegistry
import app.hushstream.core.domain.addon.InstalledAddon
import app.hushstream.design.HushSpacing
import app.hushstream.design.HushTokens
import app.hushstream.ui.common.GlassTopBar
import app.hushstream.ui.common.HushBackButton
import kotlinx.coroutines.launch

/** Full-screen manager for Stremio-protocol addons. Only https manifest links are accepted. */
@Composable
fun SourcesScreen(
    registry: AddonRegistry,
    onBack: () -> Unit,
    onChanged: () -> Unit,
) {
    val colors = HushTokens.colors
    val scope = rememberCoroutineScope()
    var addons by remember { mutableStateOf<List<InstalledAddon>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { addons = registry.all() }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "Sources",
            navigationIcon = { HushBackButton(onClick = onBack) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(HushSpacing.s12),
        ) {
            Text(
                text = "Paste an addon's manifest link. Titles, search and streams come from the addons you add.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.labelSecondary,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
                label = { Text("Manifest link") },
                placeholder = { Text("https://example.com/manifest.json") },
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        registry.add(input)
                            .onSuccess { added ->
                                input = ""
                                message = "Added ${added.name}"
                                onChanged()
                            }
                            .onFailure { error -> message = error.message ?: "Could not add this addon" }
                        addons = registry.all()
                        busy = false
                    }
                },
                enabled = input.isNotBlank() && !busy,
                modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
            ) { Text(if (busy) "Checking addon" else "Add addon") }

            message?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.labelSecondary)
            }

            Text(
                text = "Installed",
                style = MaterialTheme.typography.titleMedium,
                color = colors.label,
            )
            if (addons.isEmpty()) {
                Text(
                    text = "No addons yet. The demo catalogue is shown until you add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.labelSecondary,
                )
            }
            addons.forEach { addon ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = addon.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = addon.manifestUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.labelTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                registry.remove(addon.manifestUrl)
                                addons = registry.all()
                                onChanged()
                            }
                        },
                        modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
                    ) { Text("Remove") }
                }
            }
        }
    }
}
