package app.desperse.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.PostUpdateManager
import app.desperse.data.dto.response.BlockedUser
import app.desperse.data.repository.UserRepository
import app.desperse.ui.components.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val isLoading: Boolean = true,
    val users: List<BlockedUser> = emptyList(),
    val error: String? = null,
    val pendingUnblockIds: Set<String> = emptySet()
)

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postUpdateManager: PostUpdateManager,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            userRepository.getBlockedUsers()
                .onSuccess { users ->
                    _uiState.update { it.copy(isLoading = false, users = users) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load") }
                }
        }
    }

    fun unblock(user: BlockedUser) {
        if (user.id in _uiState.value.pendingUnblockIds) return
        _uiState.update { it.copy(pendingUnblockIds = it.pendingUnblockIds + user.id) }

        viewModelScope.launch {
            userRepository.unblockUser(user.id)
                .onSuccess {
                    postUpdateManager.emitBlockUpdate(user.id, isBlocked = false)
                    toastManager.showInfo("Unblocked @${user.slug}")
                    _uiState.update {
                        it.copy(
                            users = it.users.filterNot { u -> u.id == user.id },
                            pendingUnblockIds = it.pendingUnblockIds - user.id
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(pendingUnblockIds = it.pendingUnblockIds - user.id) }
                    toastManager.showError(error.message ?: "Failed to unblock")
                }
        }
    }
}
