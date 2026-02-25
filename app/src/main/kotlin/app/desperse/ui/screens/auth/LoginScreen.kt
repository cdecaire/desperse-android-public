package app.desperse.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.R
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.InstalledWallet
import app.desperse.ui.components.WalletPickerSheet
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    walletCallbacks: SharedFlow<android.net.Uri>? = null,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var showWalletPicker by remember { mutableStateOf(false) }

    val isSeekerDevice = viewModel.isSeekerDevice
    val isMwaAvailable = viewModel.isMwaAvailable
    val installedWallets = remember { viewModel.installedWallets }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> onLoginSuccess()
                is LoginEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Handle wallet deeplink callbacks
    LaunchedEffect(walletCallbacks) {
        walletCallbacks?.collect { uri ->
            viewModel.handleWalletCallback(activity, uri)
        }
    }

    // Connect wallet: show picker if multiple wallets, otherwise connect directly
    val connectToWallet = remember(activity, viewModel) {
        { wallet: InstalledWallet ->
            if (viewModel.shouldTryMwaFirst(wallet.packageName)) {
                viewModel.loginWithMwa(
                    activity = activity,
                    targetPackage = wallet.packageName,
                    fallbackToDeeplinkOnMwaFailure = true
                )
            } else if (viewModel.shouldUseDeeplink(wallet.packageName)) {
                viewModel.loginWithDeeplink(activity, wallet.packageName)
            } else {
                viewModel.loginWithMwa(activity, wallet.packageName)
            }
        }
    }

    val onConnectWallet = remember(installedWallets) {
        {
            if (installedWallets.size > 1) {
                showWalletPicker = true
            } else if (installedWallets.size == 1) {
                val wallet = installedWallets.first()
                connectToWallet(InstalledWallet(wallet.packageName, wallet.displayName, wallet.walletClientType))
            } else {
                // No wallets detected: try MWA with system chooser
                viewModel.loginWithMwa(activity)
            }
        }
    }

    // Wallet picker bottom sheet
    if (showWalletPicker) {
        WalletPickerSheet(
            wallets = installedWallets.map { mwaWallet ->
                InstalledWallet(
                    packageName = mwaWallet.packageName,
                    displayName = mwaWallet.displayName,
                    walletClientType = mwaWallet.walletClientType
                )
            },
            onWalletSelected = { wallet ->
                connectToWallet(wallet)
            },
            onDismiss = { showWalletPicker = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Icon(
                    painter = painterResource(id = R.drawable.desperse_logo),
                    contentDescription = "Desperse",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Welcome to Desperse",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Create, collect, and own your media.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error display
            if (uiState is LoginUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val isInputDisabled = uiState is LoginUiState.Loading ||
                uiState is LoginUiState.MwaConnecting ||
                uiState is LoginUiState.MwaSigning

            // Seeker device: Show "Connect Wallet" first, then divider, then email + OAuth
            if (isSeekerDevice) {
                // Primary wallet button for Seeker devices
                Button(
                    onClick = onConnectWallet,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInputDisabled
                ) {
                    FaIcon(
                        icon = FaIcons.Wallet,
                        size = 18.dp,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Wallet")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider with "or"
                OrDivider()

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Login flow
            when (uiState) {
                is LoginUiState.Idle, is LoginUiState.Error -> {
                    EmailInputSection(
                        email = email,
                        onEmailChange = {
                            email = it
                            viewModel.clearError()
                        },
                        onSubmit = { viewModel.sendEmailCode(email) },
                        enabled = email.isNotBlank()
                    )
                }

                is LoginUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Please wait...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is LoginUiState.MwaConnecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to wallet...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is LoginUiState.MwaSigning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Approve the sign request in your wallet...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is LoginUiState.CodeSent -> {
                    CodeInputSection(
                        email = email,
                        code = code,
                        onCodeChange = { code = it },
                        onSubmit = { viewModel.verifyEmailCode(code) },
                        onBack = {
                            code = ""
                            viewModel.resetToEmailInput()
                        },
                        enabled = code.length >= 6
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider with "or"
            OrDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Social login buttons
            OutlinedButton(
                onClick = { viewModel.loginWithGoogle() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInputDisabled
            ) {
                FaIcon(
                    icon = FaIcons.Google,
                    size = 18.dp,
                    style = FaIconStyle.Brands,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.loginWithX() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInputDisabled
            ) {
                FaIcon(
                    icon = FaIcons.X,
                    size = 18.dp,
                    style = FaIconStyle.Brands,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with X")
            }

            // Non-Seeker device with MWA wallet available: show wallet button at bottom
            if (!isSeekerDevice && isMwaAvailable) {
                Spacer(modifier = Modifier.height(24.dp))

                // Divider with "or"
                OrDivider()

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onConnectWallet,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInputDisabled
                ) {
                    FaIcon(
                        icon = FaIcons.Wallet,
                        size = 18.dp,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Wallet")
                }
            }
        }

            // Terms and Privacy at bottom
            val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
            val linkColor = MaterialTheme.colorScheme.primary
            val linkStyles = TextLinkStyles(
                style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            )
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(color = baseColor)) {
                    append("By signing in, you agree to our ")
                }
                withLink(LinkAnnotation.Url("https://desperse.com/terms", linkStyles)) {
                    append("Terms of Service")
                }
                withStyle(SpanStyle(color = baseColor)) {
                    append(" and ")
                }
                withLink(LinkAnnotation.Url("https://desperse.com/privacy", linkStyles)) {
                    append("Privacy Policy")
                }
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun EmailInputSection(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email address") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (enabled) onSubmit() }
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Text("Continue with Email")
    }
}

@Composable
private fun CodeInputSection(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    enabled: Boolean
) {
    Text(
        text = "Enter the code sent to",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = email,
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = code,
        onValueChange = { if (it.length <= 6) onCodeChange(it) },
        label = { Text("Verification code") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (enabled) onSubmit() }
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Text("Verify")
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Use different email")
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "or",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
