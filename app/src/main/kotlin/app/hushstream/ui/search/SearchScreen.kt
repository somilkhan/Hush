package app.hushstream.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hushstream.core.domain.repository.MediaRepository
import app.hushstream.design.HushSpacing
import app.hushstream.ui.common.LoadingRows
import app.hushstream.ui.common.PosterCard
import app.hushstream.ui.common.StateMessage
import app.hushstream.ui.common.viewModelFactoryOf

@Composable
fun SearchScreen(repository: MediaRepository, onOpen: (String) -> Unit) {
    val vm: SearchViewModel = viewModel(factory = viewModelFactoryOf { SearchViewModel(repository) })
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s8),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HushSpacing.minTouch),
            label = { Text("Search titles") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit() }),
        )
        Spacer(Modifier.height(HushSpacing.s12))

        when (val status = state.status) {
            SearchStatus.Idle -> StateMessage(
                title = "Search your sources",
                body = "Searches every addon you have added. Add one in Settings, under Sources.",
            )
            SearchStatus.Loading -> LoadingRows(description = "Searching")
            SearchStatus.Empty -> StateMessage(
                title = "No matches",
                body = "Nothing matches \"${state.submitted}\" in your sources.",
            )
            SearchStatus.Error -> StateMessage(
                title = "Search failed",
                body = "The search did not finish. Try again.",
                actionLabel = "Retry",
                onAction = vm::retry,
            )
            is SearchStatus.Results -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = HushSpacing.s8),
                horizontalArrangement = Arrangement.spacedBy(HushSpacing.s12),
                verticalArrangement = Arrangement.spacedBy(HushSpacing.s16),
            ) {
                items(status.items, key = { it.id }) { media ->
                    PosterCard(item = media, onOpen = onOpen)
                }
            }
        }
    }
}
