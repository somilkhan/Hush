package app.hushstream.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.repository.MediaRepository
import app.hushstream.core.domain.sync.LibraryEntry
import app.hushstream.core.domain.sync.LibraryRepository
import app.hushstream.design.HushShapes
import app.hushstream.design.HushSpacing
import app.hushstream.design.HushTokens
import app.hushstream.ui.common.DemoBadge
import app.hushstream.ui.common.GlassTopBar
import app.hushstream.ui.common.HushBackButton
import app.hushstream.ui.common.LoadingRows
import app.hushstream.ui.common.StateMessage
import app.hushstream.ui.common.kindLabel
import app.hushstream.ui.common.viewModelFactoryOf
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    repository: MediaRepository,
    library: LibraryRepository,
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
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
                body = "This title is no longer available from your sources.",
                actionLabel = "Back",
                onAction = onBack,
            )
            DetailsUiState.Error -> StateMessage(
                title = "Could not load",
                body = "Details failed to load.",
                actionLabel = "Retry",
                onAction = vm::load,
            )
            is DetailsUiState.Loaded -> DetailsContent(media = s.item, repository = repository, library = library, onPlay = onPlay)
        }
    }
}

@Composable
private fun DetailsContent(
    media: MediaItem,
    repository: MediaRepository,
    library: LibraryRepository,
    onPlay: (PlayRequest) -> Unit,
) {
    val colors = HushTokens.colors
    val scope = rememberCoroutineScope()
    val seasons = remember(media.id) { media.episodes.map { it.season }.distinct().sorted() }
    var season by remember(media.id) { mutableStateOf(seasons.firstOrNull() ?: 1) }
    val visible = media.episodes.filter { it.season == season }
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, colors.canvas.copy(alpha = 0.7f), colors.canvas),
                                ),
                            ),
                    )
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
        if (seasons.size > 1) {
            item {
                SeasonRow(seasons = seasons, selected = season, onSelect = { season = it })
            }
        }
        items(visible, key = { it.id }) { episode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HushShapes.m)
                    .background(colors.surfaceRaised)
                    .padding(HushSpacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HushSpacing.s4)) {
                    Text(
                        text = if (media.kind == MediaKind.MOVIE) {
                            "Movie"
                        } else {
                            "Season ${episode.season} · Episode ${episode.number}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.labelSecondary,
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            repository.streams(media, episode.id).firstOrNull()?.let {
                                onPlay(PlayRequest(it.url, media.id, episode.id, media.title))
                            }
                        }
                    },
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

/** Horizontal season picker. Shown only for series with more than one season. */
@Composable
private fun SeasonRow(seasons: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(HushSpacing.s8),
    ) {
        seasons.forEach { s ->
            val modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch)
            if (s == selected) {
                Button(onClick = { onSelect(s) }, modifier = modifier) { Text("Season $s") }
            } else {
                OutlinedButton(onClick = { onSelect(s) }, modifier = modifier) { Text("Season $s") }
            }
        }
    }
}
