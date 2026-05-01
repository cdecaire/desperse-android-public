package app.desperse.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import app.desperse.ui.components.DesperseBackButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.BlockedUser
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Accounts") },
                navigationIcon = {
                    DesperseBackButton(onClick = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(DesperseSpacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(DesperseSpacing.md))
                        DesperseTextButton(
                            text = "Retry",
                            onClick = { viewModel.load() },
                            variant = ButtonVariant.Default
                        )
                    }
                }
                state.users.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(DesperseSpacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FaIcon(
                            icon = FaIcons.Ban,
                            size = 32.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = FaIconStyle.Solid
                        )
                        Spacer(Modifier.height(DesperseSpacing.md))
                        Text(
                            text = "No blocked accounts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(DesperseSpacing.xs))
                        Text(
                            text = "People you block will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.users, key = { it.id }) { user ->
                            BlockedUserRow(
                                user = user,
                                isUnblocking = user.id in state.pendingUnblockIds,
                                onUnblock = { viewModel.unblock(user) },
                                onClick = { onUserClick(user.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: BlockedUser,
    isUnblocking: Boolean,
    onUnblock: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val optimizedAvatar = remember(user.avatarUrl) {
        user.avatarUrl?.let { ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (optimizedAvatar != null) {
                AsyncImage(
                    model = optimizedAvatar,
                    contentDescription = user.displayName ?: user.slug,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                GeometricAvatar(input = user.slug, size = 40.dp)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.slug,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "@${user.slug}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DesperseTextButton(
            text = if (isUnblocking) "Unblocking..." else "Unblock",
            onClick = onUnblock,
            variant = ButtonVariant.Default,
            enabled = !isUnblocking
        )
    }
}
