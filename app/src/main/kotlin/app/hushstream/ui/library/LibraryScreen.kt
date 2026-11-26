package app.hushstream.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hushstream.core.domain.sync.LibraryEntry
import app.hushstream.core.domain.sync.LibraryRepository
import app.hushstream.design.HushShapes
import app.hushstream.design.HushSpacing
import app.hushstream.design.HushTokens
import app.hushstream.ui.common.StateMessage

/** Titles the user saved on this device. Account and sources live in Settings. */
@Composable
fun LibraryScreen(
    library: LibraryRepository,
    onOpen: (String) -> Unit,
) {
    val colors = HushTokens.colors
    var saved by remember { mutableStateOf<List<LibraryEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        saved = library.all().filter { it.saved }.sortedByDescending { it.updatedAtEpochMs }
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s8),
    ) {
        when {
            !loaded -> Text(
                text = "Loading your library",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.labelSecondary,
            )
            saved.isEmpty() -> StateMessage(
                title = "Nothing saved yet",
                body = "Open a title and tap Save to keep it here.",
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = HushSpacing.s8),
                horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
                verticalArrangement = Arrangement.spacedBy(HushSpacing.s16),
            ) {
                items(saved, key = { it.mediaId }) { entry ->
                    SavedTile(entry = entry, onOpen = onOpen)
                }
            }
        }
    }
}

@Composable
private fun SavedTile(entry: LibraryEntry, onOpen: (String) -> Unit) {
    val colors = HushTokens.colors
    Column(
        modifier = Modifier
            .clip(HushShapes.m)
            .clickable(onClickLabel = "Open ${entry.title}", role = Role.Button) { onOpen(entry.mediaId) },
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s8),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(HushShapes.m)
                .background(colors.surfaceRaised),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = entry.title.firstOrNull()?.uppercase() ?: "",
                style = MaterialTheme.typography.displayMedium,
                color = colors.labelTertiary,
                modifier = Modifier.padding(HushSpacing.s12),
            )
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
