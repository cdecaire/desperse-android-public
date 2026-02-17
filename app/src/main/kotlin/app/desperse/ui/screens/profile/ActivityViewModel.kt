package app.desperse.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.dto.response.ActivityItem
import app.desperse.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val activities: List<ActivityItem> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, activities = emptyList()) }

            userRepository.getUserActivity()
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        activities = data.activities,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor,
                        error = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load activity"
                    ) }
                }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return

        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            userRepository.getUserActivity(cursor)
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        isLoadingMore = false,
                        activities = it.activities + data.activities,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor
                    ) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }
}
