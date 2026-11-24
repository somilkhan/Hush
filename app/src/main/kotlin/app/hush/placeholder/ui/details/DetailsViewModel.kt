package app.hush.placeholder.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hush.placeholder.core.domain.model.MediaItem
import app.hush.placeholder.core.domain.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data object NotFound : DetailsUiState
    data object Error : DetailsUiState
    data class Loaded(val item: MediaItem) : DetailsUiState
}

class DetailsViewModel(
    private val repository: MediaRepository,
    private val mediaId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = DetailsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val item = repository.details(mediaId)
                if (item == null) DetailsUiState.NotFound else DetailsUiState.Loaded(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailsUiState.Error
            }
        }
    }
}
