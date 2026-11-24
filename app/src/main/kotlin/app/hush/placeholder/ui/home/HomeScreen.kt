package app.hush.placeholder.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.ui.common.LoadingRows
import app.hush.placeholder.ui.common.MediaListItem
import app.hush.placeholder.ui.common.StateMessage
import app.hush.placeholder.ui.common.viewModelFactoryOf

@Composable
fun HomeScreen(repository: MediaRepository, onOpen: (String) -> Unit) {
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
        is HomeUiState.Loaded -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = HushSpacing.gutter, vertical = HushSpacing.s8),
        ) {
            items(s.items, key = { it.id }) { media ->
                MediaListItem(item = media, onOpen = onOpen)
            }
        }
    }
}
