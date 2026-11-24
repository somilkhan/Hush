package app.hush.placeholder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens
import app.hush.placeholder.ui.common.HeroBanner
import app.hush.placeholder.ui.common.LoadingRows
import app.hush.placeholder.ui.common.PosterCard
import app.hush.placeholder.ui.common.StateMessage
import app.hush.placeholder.ui.common.viewModelFactoryOf

@Composable
fun HomeScreen(
    repository: MediaRepository,
    onOpen: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = viewModelFactoryOf { HomeViewModel(repository) })
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        HomeUiState.Loading -> LoadingRows(description = "Loading featured titles")
        HomeUiState.Empty -> StateMessage(
            title = "Nothing featured",
            body = "The demo catalogue has no featured titles.",
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
                verticalArrangement = Arrangement.spacedBy(HushSpacing.s16),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroBanner(item = hero, onPlay = onPlay, onOpen = onOpen)
                }
                if (rest.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "More titles",
                            style = MaterialTheme.typography.titleMedium,
                            color = HushTokens.colors.label,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(rest, key = { it.id }) { media ->
                        PosterCard(item = media, onOpen = onOpen)
                    }
                }
            }
        }
    }
}
