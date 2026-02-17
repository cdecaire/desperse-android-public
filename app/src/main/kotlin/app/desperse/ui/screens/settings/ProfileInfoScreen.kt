package app.desperse.ui.screens.settings

import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.repository.UserRepository
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.GeometricBanner
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AVATAR_MAX_SIZE = 2 * 1024 * 1024 // 2MB
private const val HEADER_MAX_SIZE = 5 * 1024 * 1024 // 5MB

@HiltViewModel
class ProfileInfoViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val currentUser = userRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _events = MutableSharedFlow<ProfileInfoEvent>()
    val events = _events.asSharedFlow()

    var displayName by mutableStateOf("")
        private set
    var bio by mutableStateOf("")
        private set
    var username by mutableStateOf("")
        private set
    var website by mutableStateOf("")
        private set

    // Track initial values for dirty checking
    private var initialDisplayName = ""
    private var initialBio = ""
    private var initialUsername = ""
    private var initialWebsite = ""

    var isSaving by mutableStateOf(false)
        private set
    var isUploadingAvatar by mutableStateOf(false)
        private set
    var isUploadingHeader by mutableStateOf(false)
        private set

    val isDirty: Boolean
        get() = displayName != initialDisplayName ||
                bio != initialBio ||
                username != initialUsername ||
                website != initialWebsite

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                user?.let {
                    displayName = it.displayName ?: ""
                    bio = it.bio ?: ""
                    username = it.slug ?: ""
                    website = it.website ?: ""
                    // Store initial values
                    initialDisplayName = displayName
                    initialBio = bio
                    initialUsername = username
                    initialWebsite = website
                }
            }
        }
    }

    fun updateDisplayName(value: String) {
        if (value.length <= 50) displayName = value
    }

    fun updateBio(value: String) {
        if (value.length <= 280) bio = value
    }

    fun updateUsername(value: String) {
        val filtered = value.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        if (filtered.length <= 24) username = filtered
    }

    fun updateWebsite(value: String) {
        website = value
    }

    fun save() {
        viewModelScope.launch {
            isSaving = true
            try {
                val result = userRepository.updateProfile(
                    displayName = displayName.takeIf { it.isNotBlank() },
                    bio = bio.takeIf { it.isNotBlank() },
                    usernameSlug = username.takeIf { it.isNotBlank() && username != initialUsername },
                    website = website.takeIf { it.isNotBlank() }
                )
                result.fold(
                    onSuccess = {
                        // Update initial values after successful save
                        initialDisplayName = displayName
                        initialBio = bio
                        initialUsername = username
                        initialWebsite = website
                        _events.emit(ProfileInfoEvent.Saved)
                    },
                    onFailure = { error ->
                        _events.emit(ProfileInfoEvent.Error(error.message ?: "Failed to save"))
                    }
                )
            } finally {
                isSaving = false
            }
        }
    }

    fun uploadAvatar(base64Data: String, fileName: String, mimeType: String, fileSize: Int) {
        viewModelScope.launch {
            isUploadingAvatar = true
            try {
                val result = userRepository.uploadAvatar(base64Data, fileName, mimeType, fileSize)
                result.fold(
                    onSuccess = {
                        _events.emit(ProfileInfoEvent.AvatarUploaded)
                    },
                    onFailure = { error ->
                        _events.emit(ProfileInfoEvent.Error(error.message ?: "Failed to upload avatar"))
                    }
                )
            } finally {
                isUploadingAvatar = false
            }
        }
    }

    fun uploadHeader(base64Data: String, fileName: String, mimeType: String, fileSize: Int) {
        viewModelScope.launch {
            isUploadingHeader = true
            try {
                val result = userRepository.uploadHeader(base64Data, fileName, mimeType, fileSize)
                result.fold(
                    onSuccess = {
                        _events.emit(ProfileInfoEvent.HeaderUploaded)
                    },
                    onFailure = { error ->
                        _events.emit(ProfileInfoEvent.Error(error.message ?: "Failed to upload header"))
                    }
                )
            } finally {
                isUploadingHeader = false
            }
        }
    }
}

sealed class ProfileInfoEvent {
    object Saved : ProfileInfoEvent()
    object AvatarUploaded : ProfileInfoEvent()
    object HeaderUploaded : ProfileInfoEvent()
    data class Error(val message: String) : ProfileInfoEvent()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileInfoScreen(
    onBack: () -> Unit,
    viewModel: ProfileInfoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    // Avatar picker launcher
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(selectedUri) ?: "image/jpeg"

                if (!mimeType.startsWith("image/")) {
                    Toast.makeText(context, "Please select an image file", Toast.LENGTH_SHORT).show()
                    return@let
                }

                contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()

                    if (bytes.size > AVATAR_MAX_SIZE) {
                        Toast.makeText(context, "Avatar must be 2MB or smaller", Toast.LENGTH_SHORT).show()
                        return@let
                    }

                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val fileName = selectedUri.lastPathSegment ?: "avatar.jpg"

                    viewModel.uploadAvatar(base64, fileName, mimeType, bytes.size)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Header picker launcher
    val headerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(selectedUri) ?: "image/jpeg"

                if (!mimeType.startsWith("image/")) {
                    Toast.makeText(context, "Please select an image file", Toast.LENGTH_SHORT).show()
                    return@let
                }

                contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()

                    if (bytes.size > HEADER_MAX_SIZE) {
                        Toast.makeText(context, "Header must be 5MB or smaller", Toast.LENGTH_SHORT).show()
                        return@let
                    }

                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val fileName = selectedUri.lastPathSegment ?: "header.jpg"

                    viewModel.uploadHeader(base64, fileName, mimeType, bytes.size)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileInfoEvent.Saved -> {
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is ProfileInfoEvent.AvatarUploaded -> {
                    Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
                }
                is ProfileInfoEvent.HeaderUploaded -> {
                    Toast.makeText(context, "Header updated", Toast.LENGTH_SHORT).show()
                }
                is ProfileInfoEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                }
            )
        }
    ) { padding ->
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Update your public profile and username.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Header Background Image - full width
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Header Background Image",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val headerUrl = remember(currentUser?.headerUrl) {
                        currentUser?.headerUrl?.let {
                            ImageOptimization.getOptimizedUrlForContext(it, ImageContext.PROFILE_HEADER)
                        }
                    }

                    if (headerUrl != null) {
                        AsyncImage(
                            model = headerUrl,
                            contentDescription = "Header background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            GeometricBanner(
                                input = currentUser?.walletAddress ?: currentUser?.slug ?: "",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Upload button overlay - top right
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!viewModel.isUploadingHeader) {
                                    headerPickerLauncher.launch("image/*")
                                }
                            },
                            enabled = !viewModel.isUploadingHeader,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (viewModel.isUploadingHeader) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (currentUser?.headerUrl != null) "Change" else "Upload")
                        }
                    }
                }

                Text(
                    text = "Recommended: 1200×400px. Max 5MB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Avatar Row - profile preview style
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    val avatarUrl = remember(currentUser?.avatarUrl) {
                        currentUser?.avatarUrl?.let {
                            ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR)
                        }
                    }

                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            GeometricAvatar(
                                input = currentUser?.walletAddress ?: currentUser?.slug ?: "",
                                size = 64.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // User info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: currentUser?.slug ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${currentUser?.slug ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Upload button
                    Button(
                        onClick = {
                            if (!viewModel.isUploadingAvatar) {
                                avatarPickerLauncher.launch("image/*")
                            }
                        },
                        enabled = !viewModel.isUploadingAvatar,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (viewModel.isUploadingAvatar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (currentUser?.avatarUrl != null) "Change" else "Upload")
                    }
                }
            }

            // Form fields
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Display Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Display name",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        value = viewModel.displayName,
                        onValueChange = { viewModel.updateDisplayName(it) },
                        placeholder = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            Text(
                                text = "${viewModel.displayName.length}/50",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // Bio
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bio",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Box {
                        OutlinedTextField(
                            value = viewModel.bio,
                            onValueChange = { viewModel.updateBio(it) },
                            placeholder = { Text("Tell the world about you") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            delay(300)
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            minLines = 4,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            )
                        )
                        Text(
                            text = "${viewModel.bio.length}/280",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 8.dp)
                        )
                    }
                }

                // Website
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Website",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        value = viewModel.website,
                        onValueChange = { viewModel.updateWebsite(it) },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                    Text(
                        text = "Your portfolio, website, or social media link",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Username
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        placeholder = { Text("username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            Text(
                                text = "${viewModel.username.length}/24",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Text(
                        text = "Lowercase a-z, 0-9, _ and . only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Save button - only show when dirty
            if (viewModel.isDirty) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !viewModel.isSaving
                    ) {
                        if (viewModel.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Save changes")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
