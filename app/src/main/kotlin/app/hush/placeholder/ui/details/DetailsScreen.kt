package app.hush.placeholder.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hush.placeholder.core.domain.model.MediaItem
import app.hush.placeholder.core.domain.model.MediaKind
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.core.domain.sync.LibraryEntry
import app.hush.placeholder.core.domain.sync.LibraryRepository
import app.hush.placeholder.design.HushShapes
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens
import app.hush.placeholder.ui.common.DemoBadge
import app.hush.placeholder.ui.common.GlassTopBar
import app.hush.placeholder.ui.common.HushBackButton
import app.hush.placeholder.ui.common.LoadingRows
import app.hush.placeholder.ui.common.StateMessage
import app.hush.placeholder.ui.common.kindLabel
import app.hush.placeholder.ui.common.viewModelFactoryOf
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    repository: MediaRepository,
    library: LibraryRepository,
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
) {
    val vm: DetailsViewModel = viewModel(
        key = "details-$mediaId",
        factory = viewModelFactoryOf { DetailsViewModel(repository, mediaId) },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = "Details",
            navigationIcon = { HushBackButton(onClick = onBack) },
        )
        when (val s = state) {
            DetailsUiState.Loading -> LoadingRows(description = "Loading details")
            DetailsUiState.NotFound -> StateMessage(
                title = "Not found",
                body = "This title is no longer in the demo catalogue.",
                actionLabel = "Back",
                onAction = onBack,
            )
            DetailsUiState.Error -> StateMessage(
                title = "Could not load",
                body = "Details failed to load.",
                actionLabel = "Retry",
                onAction = vm::load,
            )
            is DetailsUiState.Loaded -> DetailsContent(media = s.item, library = library, onPlay = onPlay)
        }
    }
}

@Composable
private fun DetailsContent(
    media: MediaItem,
    library: LibraryRepository,
    onPlay: (String) -> Unit,
) {
    val colors = HushTokens.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = HushSpacing.gutter, vertical = HushSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s12),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HushSpacing.s8)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(HushShapes.l)
                        .background(colors.surfaceRaised),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = media.title.firstOrNull()?.uppercase() ?: "",
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.labelTertiary,
                        modifier = Modifier.padding(HushSpacing.s16),
                    )
                    if (media.isDemo) DemoBadge(modifier = Modifier.align(Alignment.TopStart).padding(HushSpacing.s12))
                }
                if (media.isDemo) DemoBadge()
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.label,
                )
                Text(
                    text = "${kindLabel(media.kind)} · ${media.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.labelSecondary,
                )
                Text(
                    text = media.synopsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.label,
                )
                SaveToggle(media = media, library = library)
            }
        }
        items(media.episodes, key = { it.id }) { episode ->
            val url = episode.streams.firstOrNull()?.url
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (media.kind == MediaKind.MOVIE) {
                            "Movie"
                        } else {
                            "Season ${episode.season} · Episode ${episode.number}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.labelSecondary,
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.label,
                    )
                }
                Button(
                    onClick = { url?.let(onPlay) },
                    enabled = url != null,
                    modifier = Modifier.defaultMinSize(minWidth = HushSpacing.minTouch, minHeight = HushSpacing.minTouch),
                ) {
                    Text("Play")
                }
            }
        }
    }
}

/** Adds or removes the title from the device library. Reads state once per open, writes on tap. */
@Composable
private fun SaveToggle(media: MediaItem, library: LibraryRepository) {
    val scope = rememberCoroutineScope()
    var saved by remember(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) {
        saved = library.all().any { it.mediaId == media.id && it.saved }
    }
    val label = if (saved) "Saved" else "Save"
    val onClick = {
        scope.launch {
            if (saved) {
                library.remove(media.id)
                saved = false
            } else {
                library.upsert(
                    LibraryEntry(
                        mediaId = media.id,
                        kind = media.kind,
                        title = media.title,
                        posterUrl = null,
                        saved = true,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
                saved = true
            }
        }
        Unit
    }
    if (saved) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
        ) { Text(label) }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
        ) { Text(label) }
    }
}
