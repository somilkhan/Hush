package app.hushstream.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data object Error : HomeUiState
    data class Loaded(val items: List<MediaItem>) : HomeUiState
}

class HomeViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val items = repository.featured()
                if (items.isEmpty()) HomeUiState.Empty else HomeUiState.Loaded(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                HomeUiState.Error
            }
        }
    }
}
