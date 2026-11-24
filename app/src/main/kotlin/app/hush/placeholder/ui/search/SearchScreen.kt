package app.hush.placeholder.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.ui.common.LoadingRows
import app.hush.placeholder.ui.common.MediaListItem
import app.hush.placeholder.ui.common.StateMessage
import app.hush.placeholder.ui.common.viewModelFactoryOf

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
            label = { Text("Search the demo catalogue") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit() }),
        )
        Spacer(Modifier.height(HushSpacing.s12))

        when (val status = state.status) {
            SearchStatus.Idle -> StateMessage(
                title = "Search the demo list",
                body = "Results come only from the built-in demo titles.",
            )
            SearchStatus.Loading -> LoadingRows(description = "Searching")
            SearchStatus.Empty -> StateMessage(
                title = "No matches",
                body = "Nothing in the demo list matches \"${state.submitted}\".",
            )
            SearchStatus.Error -> StateMessage(
                title = "Search failed",
                body = "The search did not finish. Try again.",
                actionLabel = "Retry",
                onAction = vm::retry,
            )
            is SearchStatus.Results -> LazyColumn(
                contentPadding = PaddingValues(vertical = HushSpacing.s8),
            ) {
                items(status.items, key = { it.id }) { media ->
                    MediaListItem(item = media, onOpen = onOpen)
                }
            }
        }
    }
}
