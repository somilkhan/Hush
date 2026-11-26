package app.hushstream.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.repository.MediaRepository
import app.hushstream.core.domain.sync.WatchEntry
import app.hushstream.design.HushShapes
import app.hushstream.design.HushSpacing
import app.hushstream.design.HushTokens
import app.hushstream.ui.common.HeroBanner
import app.hushstream.ui.common.LoadingRows
import app.hushstream.ui.common.PosterCard
import app.hushstream.ui.common.StateMessage
import app.hushstream.ui.common.viewModelFactoryOf

/**
 * Home: hero, Continue watching (local history), then horizontal poster rails.
 * The hero opens Details, where episode streams are resolved on demand.
 */
@Composable
fun HomeScreen(
    repository: MediaRepository,
    continueWatching: List<WatchEntry>,
    onOpen: (String) -> Unit,
    onResume: (WatchEntry) -> Unit,
    refreshKey: Int = 0,
) {
    val vm: HomeViewModel = viewModel(key = "home-$refreshKey", factory = viewModelFactoryOf { HomeViewModel(repository) })
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        HomeUiState.Loading -> LoadingRows(description = "Loading featured titles")
        HomeUiState.Empty -> StateMessage(
            title = "No titles yet",
            body = "Add an addon in Settings, under Sources.",
        )
        HomeUiState.Error -> StateMessage(
            title = "Could not load",
            body = "Featured titles failed to load.",
            actionLabel = "Retry",
            onAction = vm::load,
        )
        is HomeUiState.Loaded -> {
            val hero = s.items.first()
            val rest = s.items.drop(1)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = HushSpacing.gutter,
                    vertical = HushSpacing.s16,
                ),
                horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
                verticalArrangement = Arrangement.spacedBy(HushSpacing.s24),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroBanner(item = hero, onPlay = { onOpen(hero.id) }, onOpen = onOpen)
                }
                if (continueWatching.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ContinueWatchingRow(items = continueWatching, onResume = onResume)
                    }
                }
                if (rest.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PosterRail(title = "More titles", items = rest, onOpen = onOpen)
                    }
                }
            }
        }
    }
}

/** Horizontal rail of poster tiles. Swipe to scroll; each tile is a 48dp+ target. */
@Composable
private fun PosterRail(title: String, items: List<MediaItem>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HushSpacing.s8)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = HushTokens.colors.label,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
        ) {
            items.forEach { media ->
                PosterCard(item = media, onOpen = onOpen, modifier = Modifier.width(140.dp))
            }
        }
    }
}

@Composable
private fun ContinueWatchingRow(items: List<WatchEntry>, onResume: (WatchEntry) -> Unit) {
    val colors = HushTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(HushSpacing.s8)) {
        Text(
            text = "Continue watching",
            style = MaterialTheme.typography.titleMedium,
            color = colors.label,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
        ) {
            items.forEach { entry ->
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .clickable(role = Role.Button, onClickLabel = "Resume ${entry.title}") { onResume(entry) },
                    verticalArrangement = Arrangement.spacedBy(HushSpacing.s8),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(HushShapes.m)
                            .background(colors.surfaceRaised),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        val fraction = if (entry.durationMs > 0) {
                            (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(3.dp)
                                .background(colors.label),
                        )
                    }
                    Text(
                        text = entry.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
