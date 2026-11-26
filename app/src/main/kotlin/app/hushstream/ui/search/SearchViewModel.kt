package app.hushstream.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [query] is the text in the field. [submitted] is the last term sent to the repository. */
data class SearchUiState(
    val query: String = "",
    val submitted: String = "",
    val status: SearchStatus = SearchStatus.Idle,
)

sealed interface SearchStatus {
    data object Idle : SearchStatus
    data object Loading : SearchStatus
    data class Results(val items: List<MediaItem>) : SearchStatus
    data object Empty : SearchStatus
    data object Error : SearchStatus
}

class SearchViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /** Runs the search for the current query. A newer submit cancels the older one. */
    fun submit() {
        val term = _uiState.value.query.trim()
        searchJob?.cancel()
        if (term.isEmpty()) {
            _uiState.update { it.copy(submitted = "", status = SearchStatus.Idle) }
            return
        }
        _uiState.update { it.copy(submitted = term, status = SearchStatus.Loading) }
        searchJob = viewModelScope.launch {
            val status: SearchStatus = try {
                val results = repository.search(term)
                if (results.isEmpty()) SearchStatus.Empty else SearchStatus.Results(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchStatus.Error
            }
            _uiState.update { it.copy(status = status) }
        }
    }

    fun retry() {
        submit()
    }
}
