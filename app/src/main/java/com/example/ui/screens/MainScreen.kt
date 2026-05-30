package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ApiKeyEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.KeyViewModel
import java.text.SimpleDateFormat
import java.util.*

// Simulated Active Screen Enum
enum class ActiveScreen {
    Splash,
    Login,
    SignUp,
    Dashboard,
    Tutorial,
    Admin
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: KeyViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // UI state
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authSuccessMessage by viewModel.authSuccessMessage.collectAsStateWithLifecycle()
    
    val currentKeys by viewModel.currentKeys.collectAsStateWithLifecycle()
    val allKeys by viewModel.allKeys.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var activeScreen by remember { mutableStateOf(ActiveScreen.Splash) }
    var selectedKeyForTutorial by remember { mutableStateOf<ApiKeyEntity?>(null) }
    
    // Dialog triggers
    var showCreateKeyDialog by remember { mutableStateOf(false) }
    var showEditKeyDialog by remember { mutableStateOf<ApiKeyEntity?>(null) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }

    // On-start effects
    LaunchedEffect(Unit) {
        delay(2000)
        activeScreen = if (currentUser != null) {
            if (currentUser?.isAdmin == true) ActiveScreen.Admin else ActiveScreen.Dashboard
        } else {
            ActiveScreen.Login
        }
    }

    // Monitor login switches
    LaunchedEffect(currentUser) {
        if (currentUser != null && (activeScreen == ActiveScreen.Login || activeScreen == ActiveScreen.SignUp)) {
            activeScreen = if (currentUser?.isAdmin == true) ActiveScreen.Admin else ActiveScreen.Dashboard
        } else if (currentUser == null && activeScreen != ActiveScreen.Splash) {
            activeScreen = ActiveScreen.Login
        }
    }

    // Monitor success/error messages
    LaunchedEffect(authError, authSuccessMessage) {
        authError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        authSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    // Sophisticated Dark matte gradient brush
    val darkBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            SophBackground,
            Color(0xFF141517)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Render screen
        AnimatedContent(
            targetState = activeScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                ActiveScreen.Splash -> SplashScreenView()
                ActiveScreen.Login -> LoginView(
                    onGoogleLoginClick = { showGoogleAccountDialog = true },
                    onNavigateToSignUp = { activeScreen = ActiveScreen.SignUp },
                    onEmailLoginSubmit = { email, password -> viewModel.loginWithEmail(email, password) },
                    onAdminLoginSubmit = { username, password -> viewModel.loginAsAdminWithCredentials(username, password) }
                )
                ActiveScreen.SignUp -> SignUpView(
                    onRegisterSubmit = { email, name, password -> viewModel.registerWithEmail(email, name, password) },
                    onNavigateToLogin = { activeScreen = ActiveScreen.Login }
                )
                ActiveScreen.Dashboard -> {
                    DashboardView(
                        currentUser = currentUser,
                        currentKeys = currentKeys,
                        isSyncing = isSyncing,
                        syncProgress = syncProgress,
                        onLogoutClick = { viewModel.logout() },
                        onGetNewKeyClick = { showCreateKeyDialog = true },
                        onEditKeyClick = { showEditKeyDialog = it },
                        onDeleteKeyClick = { viewModel.deleteKey(it.id) },
                        onUseInTutorialClick = {
                            selectedKeyForTutorial = it
                            activeScreen = ActiveScreen.Tutorial
                        },
                        onNavigateToTutorial = { activeScreen = ActiveScreen.Tutorial },
                        onNavigateToAdmin = { activeScreen = ActiveScreen.Admin },
                        onForceSyncClick = { viewModel.syncAccounts() }
                    )
                }
                ActiveScreen.Tutorial -> {
                    TutorialView(
                        selectedKey = selectedKeyForTutorial,
                        onBackClick = { activeScreen = ActiveScreen.Dashboard }
                    )
                }
                ActiveScreen.Admin -> {
                    AdminPanelView(
                        allUsers = allUsers,
                        allKeys = allKeys,
                        onBackClick = { activeScreen = ActiveScreen.Dashboard },
                        onToggleKeyStatus = { viewModel.toggleKeyStatusByAdmin(it) }
                    )
                }
            }
        }

        // --- GOOGLE SIGN IN POPUP DIALOG (SIMULATOR) ---
        if (showGoogleAccountDialog) {
            GoogleSignInSimulatorDialog(
                onAccountSelected = { email, name ->
                    viewModel.loginWithGoogle(email, name)
                    showGoogleAccountDialog = false
                },
                onDismiss = { showGoogleAccountDialog = false }
            )
        }

        // --- CREATE API KEY DIALOG ---
        if (showCreateKeyDialog) {
            CreateKeyDialog(
                onDismiss = { showCreateKeyDialog = false },
                onGenerateKey = { label, scriptType, serviceType ->
                    viewModel.generateAndInsertKey(label, scriptType, serviceType)
                    showCreateKeyDialog = false
                }
            )
        }

        // --- EDIT API KEY DIALOG ---
        showEditKeyDialog?.let { key ->
            EditKeyDialog(
                keyEntity = key,
                onDismiss = { showEditKeyDialog = null },
                onUpdateKey = { id, label, service, status ->
                    viewModel.updateKeyDetails(id, label, service, status)
                    showEditKeyDialog = null
                }
            )
        }
    }
}

// ----------------------------------------------------------------------
// SPLASH SCREEN VIEW
// ----------------------------------------------------------------------
@Composable
fun SplashScreenView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(SophPrimary.copy(alpha = 0.12f), CircleShape)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Hacker Lock Icon",
                tint = SophPrimary,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "TMX KEYMAKER",
            color = SophTextMain,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Text(
            text = "Secure API Key Server for Termux Shell",
            color = SophTextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        CircularProgressIndicator(
            color = SophPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ----------------------------------------------------------------------
// LOGIN VIEW
// ----------------------------------------------------------------------
@Composable
fun LoginView(
    onGoogleLoginClick: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onEmailLoginSubmit: (String, String) -> Unit,
    onAdminLoginSubmit: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var tabSelected by remember { mutableStateOf(0) } // 0: Member, 1: Admin Quick Access

    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))
            // Console Brand
            Box(
                modifier = Modifier
                    .background(SophSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, SophPrimary, RoundedCornerShape(12.dp))
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Text(
                    text = "$ termux-keygen --init",
                    color = SophPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Portal Autentikasi",
                color = SophTextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kelola API Key skrip Anda dengan sinkronisasi cloud",
                color = SophTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Tabs Admin / Member Switch
            TabRow(
                selectedTabIndex = tabSelected,
                containerColor = SophSurface,
                contentColor = SophPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = tabSelected == 0,
                    onClick = { tabSelected = 0 },
                    text = { Text("Member", fontFamily = FontFamily.Monospace) }
                )
                Tab(
                    selected = tabSelected == 1,
                    onClick = { tabSelected = 1 },
                    text = { Text("Admin", fontFamily = FontFamily.Monospace) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (tabSelected == 0) {
            // Member tab: Google Sign In + Email Form
            item {
                // GOOGLE SIGN IN BUTTON
                Card(
                    onClick = { onGoogleLoginClick() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SophTextMain),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Drawing G letters
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFFEA4335), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Masuk menggunakan Google",
                            color = SophBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // OR Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SophActiveSelection)
                    Text(
                        text = "ATAU LOG IN EMAIL",
                        color = SophTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SophActiveSelection)
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Email Inpur Row
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Alamat Email", color = SophTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophTextMain,
                        unfocusedTextColor = SophTextSecondary,
                        focusedBorderColor = SophPrimary,
                        unfocusedBorderColor = SophActiveSelection
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = SophTextSecondary) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = SophTextSecondary) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophTextMain,
                        unfocusedTextColor = SophTextSecondary,
                        focusedBorderColor = SophPrimary,
                        unfocusedBorderColor = SophActiveSelection
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = SophTextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = "Sembunyikan/Tampilkan password",
                                tint = SophTextSecondary
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Submit Login
                Button(
                    onClick = { onEmailLoginSubmit(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "MASUK SESI",
                        color = SophOnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Toggle signup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Belum memiliki akun? ", color = SophTextSecondary, fontSize = 14.sp)
                    Text(
                        text = "Sign Up",
                        color = SophPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToSignUp() }
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            // Admin Credentials tab
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = SophSurface),
                    border = BorderStroke(1.dp, SophPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield Icon",
                            tint = SophPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Login Akses Admin",
                            color = SophTextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Masukkan kredensial administrator resmi",
                            color = SophTextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Username Input
                        OutlinedTextField(
                            value = adminUsername,
                            onValueChange = { adminUsername = it },
                            label = { Text("Username", color = SophTextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SophTextMain,
                                unfocusedTextColor = SophTextSecondary,
                                focusedBorderColor = SophPrimary,
                                unfocusedBorderColor = SophActiveSelection
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username", tint = SophTextSecondary) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Input
                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            label = { Text("Password", color = SophTextSecondary) },
                            visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SophTextMain,
                                unfocusedTextColor = SophTextSecondary,
                                focusedBorderColor = SophPrimary,
                                unfocusedBorderColor = SophActiveSelection
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = SophTextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                                    Icon(
                                        imageVector = if (adminPasswordVisible) Icons.Default.Check else Icons.Default.Lock,
                                        contentDescription = "Sembunyikan/Tampilkan password",
                                        tint = SophTextSecondary
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Admin Login
                        Button(
                            onClick = { onAdminLoginSubmit(adminUsername, adminPassword) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "LOGIN SEBAGAI ADMIN",
                                color = SophOnPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// SIGN UP VIEW
// ----------------------------------------------------------------------
@Composable
fun SignUpView(
    onRegisterSubmit: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(60.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Sign up logo",
                tint = SophPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Daftar Akun Member",
                color = SophTextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sinkronkan API Key Anda di seluruh perangkat Anda",
                color = SophTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap", color = SophTextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SophTextMain,
                    unfocusedTextColor = SophTextSecondary,
                    focusedBorderColor = SophPrimary,
                    unfocusedBorderColor = SophActiveSelection
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User icon", tint = SophTextSecondary) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Alamat Email", color = SophTextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SophTextMain,
                    unfocusedTextColor = SophTextSecondary,
                    focusedBorderColor = SophPrimary,
                    unfocusedBorderColor = SophActiveSelection
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = SophTextSecondary) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = SophTextSecondary) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SophTextMain,
                    unfocusedTextColor = SophTextSecondary,
                    focusedBorderColor = SophPrimary,
                    unfocusedBorderColor = SophActiveSelection
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icons", tint = SophTextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Lock,
                            contentDescription = "Show Password",
                            tint = SophTextSecondary
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = { onRegisterSubmit(email, name, password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "DAFTARKAN AKUN",
                    color = SophOnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation to Log In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Sudah memiliki akun? ", color = SophTextSecondary, fontSize = 14.sp)
                Text(
                    text = "Log In",
                    color = SophPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ----------------------------------------------------------------------
// DASHBOARD VIEW
// ----------------------------------------------------------------------
@Composable
fun DashboardView(
    currentUser: UserEntity?,
    currentKeys: List<ApiKeyEntity>,
    isSyncing: Boolean,
    syncProgress: Float,
    onLogoutClick: () -> Unit,
    onGetNewKeyClick: () -> Unit,
    onEditKeyClick: (ApiKeyEntity) -> Unit,
    onDeleteKeyClick: (ApiKeyEntity) -> Unit,
    onUseInTutorialClick: (ApiKeyEntity) -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onForceSyncClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper cyber header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SophSurface)
                .padding(bottom = 20.dp, top = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Profile Image / Abbreviation Card
                Card(
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp),
                    colors = CardDefaults.cardColors(containerColor = SophPrimary)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (currentUser?.avatarUrl?.startsWith("http") == true) {
                            AsyncImage(
                                model = currentUser.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (currentUser?.displayName?.take(2) ?: "TX").uppercase(),
                                color = SophOnPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser?.displayName ?: "User Termux",
                            color = SophTextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentUser?.isAdmin == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(SophError, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = SophOnPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Text(
                        text = currentUser?.email ?: "",
                        color = SophTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // Header controllers
                IconButton(
                    onClick = { onLogoutClick() },
                    modifier = Modifier
                        .background(SophActiveSelection, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Keluar akun",
                        tint = SophTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Action Quick Access Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tutorial Panel Trigger
            Button(
                onClick = { onNavigateToTutorial() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SophSurface),
                border = BorderStroke(1.dp, SophPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tutorial",
                    tint = SophPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tutorial", color = SophTextMain, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            // Sync account force database
            Button(
                onClick = { onForceSyncClick() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SophSurface),
                border = BorderStroke(1.dp, SophPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Backup",
                    tint = SophPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Account", color = SophTextMain, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            // Admin panel access
            if (currentUser?.isAdmin == true) {
                Button(
                    onClick = { onNavigateToAdmin() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C2A31)),
                    border = BorderStroke(1.dp, SophError.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ADMIN PANEL", color = SophTextMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Synchronization status indicator
        if (isSyncing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SophPrimary.copy(alpha = 0.08f))
                    .padding(vertical = 12.dp, horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sinkronisasi Cloud Aktif...",
                        color = SophPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(syncProgress * 100).toInt()}%",
                        color = SophPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { syncProgress },
                    color = SophPrimary,
                    trackColor = SophActiveSelection,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Mid Content: Title and List of Created Keys
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (currentKeys.isEmpty()) {
                // Empty state view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(SophSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "{ }",
                            color = SophPrimary.copy(alpha = 0.5f),
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada API Key",
                        color = SophTextMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Klik tombol \"GET NEW KEY\" di bawah untuk membuat credential skrip Termux pertama Anda.",
                        color = SophTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "KREDENSIAL API KEY ANDA (${currentKeys.size})",
                            color = SophTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    items(currentKeys) { key ->
                        ApiKeyCardItem(
                            keyEntity = key,
                            onCopyClick = {
                                clipboardManager.setText(AnnotatedString(key.keyValue))
                                Toast.makeText(context, "API Key berhasil disalin!", Toast.LENGTH_SHORT).show()
                            },
                            onEditClick = { onEditKeyClick(key) },
                            onDeleteClick = { onDeleteKeyClick(key) },
                            onUseClick = { onUseInTutorialClick(key) }
                        )
                    }
                }
            }

            // GET NEW KEY Button positioned at bottom center
            Button(
                onClick = { onGetNewKeyClick() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Get API Key icon",
                    tint = SophOnPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GET NEW KEY",
                    color = SophOnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// SINGLE API KEY CARD ITEM
// ----------------------------------------------------------------------
@Composable
fun ApiKeyCardItem(
    keyEntity: ApiKeyEntity,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUseClick: () -> Unit
) {
    val dateString = remember(keyEntity.createdAt) {
        val df = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        df.format(Date(keyEntity.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SophSurface),
        border = BorderStroke(1.dp, SophActiveSelection)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = keyEntity.label,
                        color = SophTextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = keyEntity.scriptType.uppercase(),
                            color = SophPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  " + keyEntity.serviceType,
                            color = SophTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Action controls
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Key name", tint = SophTextSecondary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete key", tint = SophError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Display Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SophBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = keyEntity.keyValue,
                        color = if (keyEntity.status == "ACTIVE") SophPrimary else SophError,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Button Copy
                    Text(
                        text = "COPY",
                        color = SophPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onCopyClick() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync, status & timestamps footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync Status icon
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (keyEntity.status == "ACTIVE") SophPrimary else SophError,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (keyEntity.status == "ACTIVE") "ACTIVE" else "REVOKED",
                        color = if (keyEntity.status == "ACTIVE") SophPrimary else SophError,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(if (keyEntity.isSynced) SophPrimary else SophError, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (keyEntity.isSynced) "Cloud Synced" else "Local Only",
                        color = if (keyEntity.isSynced) SophPrimary else SophTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Compile-Use script trigger button
                Card(
                    modifier = Modifier.clickable { onUseClick() },
                    colors = CardDefaults.cardColors(containerColor = SophActiveSelection),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "> PASANG SKRIP",
                        color = SophTextMain,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// TUTORIAL VIEW
// ----------------------------------------------------------------------
@Composable
fun TutorialView(
    selectedKey: ApiKeyEntity?,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Bash, 1: Python, 2: NodeJS
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Set fallback key if no key is supplied
    val keyValueToInsert = selectedKey?.keyValue ?: "TMX_YOUR_ACTIVE_KEY_HERE"

    val bashTemplate = """
#!/data/data/com.termux/files/usr/bin/bash
# Script Auto-Generated oleh Termux Key Manager
# SINKRONISASI API KEY: ${selectedKey?.label ?: "Universal"}

API_KEY="$keyValueToInsert"

echo -e "\e[1;32m=== MEMULAI SCRIPT TERMUX ===\e[0m"
echo "[-] Memverifikasi API Key skrip Anda..."
sleep 1

# Simulasi validasi API key lokal
if [[ "${'$'}API_KEY" == *"TMX_"* ]]; then
    echo -e "\e[1;36m[+] Verifikasi Sukses! Selamat datang!\e[0m"
    echo "[-] Membuka module terminal..."
    # Masukkan perintah skrip Anda di sini
    echo "[!] Log: Termux script running successfully."
else
    echo -e "\e[1;31m[x] ERROR: API Key Tidak Valid!\e[0m"
    exit 1
fi
""".trimIndent()

    val pythonTemplate = """
# Python Termux Script Integration
# Generated by Termux Key Manager

import time
import sys

API_KEY = "$keyValueToInsert"

def check_credential():
    print("[-] Memeriksa API Key di database local...")
    time.sleep(1)
    if API_KEY.startswith("TMX_"):
        print(f"[+] API Key ACTIVE: {API_KEY[:10]}...")
        return True
    return False

if __name__ == "__main__":
    print("=== SCRIPT UTILITY PYTHON ===")
    if not check_credential():
        print("[x] Error: Hubungan kredensial ditolak!")
        sys.exit(1)
    
    print("[+] Kredensial sinkron! Menjalankan logika skrip Python...")
    # Tulis program Python Anda di bawah ini
""".trimIndent()

    val nodeTemplate = """
// Node.js Termux Module Script
// Generated by Termux Key Manager

const API_KEY = "$keyValueToInsert";

console.log("=== TERMUX NODE.JS CONTROLLER ===");
console.log("[-] Menghubungkan API Key: " + API_KEY.substring(0, 10) + "...");

setTimeout(() => {
  if (!API_KEY.startsWith("TMX_")) {
    console.error("[x] Gagal: API KEY TIDAK VALID ATAU DI-REVOKE!");
    process.exit(1);
  }
  console.log("[+] Sukses terkoneksi! Menjalankan daemon skrip...");
  // Masukkan implementasi javascript skrip Anda disini
}, 1000);
""".trimIndent()

    val currentFilename = when (selectedTab) {
        0 -> "script_test.sh"
        1 -> "script_test.py"
        else -> "script_test.js"
    }

    val currentCodeBlock = when (selectedTab) {
        0 -> bashTemplate
        1 -> pythonTemplate
        else -> nodeTemplate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Simple cyber header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SophSurface)
                .padding(bottom = 14.dp, top = 14.dp, start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back icon", tint = SophTextMain)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Tutorial Pemasanan Kode",
                    color = SophTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (selectedKey != null) "Menggunakan key: ${selectedKey.label}" else "Silakan pilih Key di Dashboard",
                    color = SophPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // SECTION 1: LANGKAH EXECUTABLE DI TERMUX
            item {
                Text(
                    text = "TAHAP 1: PERSIAPAN INTERNAL TERMUX",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Standard Preparation step card
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(containerColor = SophSurface),
                     border = BorderStroke(1.dp, SophActiveSelection)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Buka aplikasi Termux Anda dan jalankan perintah install library yang diperlukan:",
                            color = SophTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Terminal block
                        TerminalCommandBlock(
                            command = "pkg update && pkg install curl python nodejs -y",
                            onCopyClick = {
                                clipboardManager.setText(AnnotatedString("pkg update && pkg install curl python nodejs -y"))
                                Toast.makeText(context, "Perintah install disalin!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // SECTION 2: CODE IMPLEMENTATION INTEGRATION
            item {
                Text(
                    text = "TAHAP 2: MENYISIPKAN KODE",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Language Switch Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SophSurface,
                    contentColor = SophPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bash (.sh)", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Python (.py)", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Node.js (.js)", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophSurface),
                    border = BorderStroke(1.dp, SophActiveSelection)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "File: $currentFilename",
                                color = SophTextMain,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentCodeBlock))
                                    Toast.makeText(context, "Kode template disalin!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("COPY SCRIPT", color = SophOnPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Large code scrollbox
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SophBackground, RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentCodeBlock,
                                color = SophTextMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // SECTION 3: LANGKAH RUN CODE DI TERMUX
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "TAHAP 3: MENJALANKAN DI TERMUX",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophSurface),
                    border = BorderStroke(1.dp, SophActiveSelection)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Jalankan perintah ini di Termux untuk menulis dan mengoperasikan skrip:",
                            color = SophTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val runCommand = when (selectedTab) {
                            0 -> "chmod +x $currentFilename && ./$currentFilename"
                            1 -> "python $currentFilename"
                            else -> "node $currentFilename"
                        }

                        TerminalCommandBlock(
                            command = "nano $currentFilename  # Paste kode lalu CTRL+O, ENTER, CTRL+X\n$runCommand",
                            onCopyClick = {
                                val fullCommands = "nano $currentFilename\n$runCommand"
                                clipboardManager.setText(AnnotatedString(fullCommands))
                                Toast.makeText(context, "Perintah eksekusi disalin!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalCommandBlock(
    command: String,
    onCopyClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SophBackground, RoundedCornerShape(6.dp))
            .border(1.dp, SophActiveSelection, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = command,
                color = SophPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onCopyClick,
                modifier = Modifier
                    .background(SophSurface, RoundedCornerShape(4.dp))
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share, 
                    contentDescription = "Copy command",
                    tint = SophPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// ADMIN PANEL VIEW
// ----------------------------------------------------------------------
@Composable
fun AdminPanelView(
    allUsers: List<UserEntity>,
    allKeys: List<ApiKeyEntity>,
    onBackClick: () -> Unit,
    onToggleKeyStatus: (Int) -> Unit
) {
    var selectedViewTab by remember { mutableStateOf(0) } // 0: API Keys, 1: Users registered

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SophSurface)
                .padding(bottom = 14.dp, top = 14.dp, start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali ke Dashboard", tint = SophTextMain)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Admin Server Panel",
                    color = SophTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Akses Penuh Pengelolaan Kunci & Database",
                    color = SophError,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Quick Admin Stats Board
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SophSurface),
            border = BorderStroke(1.dp, SophError.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL KUNCI", color = SophTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${allKeys.size}", color = SophTextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL USER", color = SophTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${allUsers.size}", color = SophTextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val activeCount = allKeys.count { it.status == "ACTIVE" }
                    Text("ACTIVE KEY", color = SophTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("$activeCount", color = SophPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }

        // Switching Tabs (All Keys / All Users)
        TabRow(
            selectedTabIndex = selectedViewTab,
            containerColor = SophSurface,
            contentColor = SophPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedViewTab == 0,
                onClick = { selectedViewTab = 0 },
                text = { Text("Kelola API Keys", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            )
            Tab(
                selected = selectedViewTab == 1,
                onClick = { selectedViewTab = 1 },
                text = { Text("Daftar Pengguna", fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (selectedViewTab == 0) {
                // ALL API KEYS LIST WITH MASTER BLOCK/ENABLE ACTIONS
                if (allKeys.isEmpty()) {
                    Text(
                        text = "Tidak ada API key yang terdaftar di sistem database offline.",
                        color = SophTextSecondary,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(allKeys) { key ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SophSurface),
                                border = BorderStroke(1.dp, SophActiveSelection)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = key.label,
                                                color = SophTextMain,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Pembuat: ${key.userEmail}",
                                                color = SophTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Enable/Disable Trigger
                                        Button(
                                            onClick = { onToggleKeyStatus(key.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (key.status == "ACTIVE") SophError else SophPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (key.status == "ACTIVE") "REVOKE" else "RE-ALLOW",
                                                color = SophOnPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display Key
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SophBackground, RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = key.keyValue,
                                            color = if (key.status == "ACTIVE") SophPrimary else SophError,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Details footer
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Tipe: ${key.scriptType} / ${key.serviceType}",
                                            color = SophTextSecondary,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "Status: ${key.status}",
                                            color = if (key.status == "ACTIVE") SophPrimary else SophError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ALL REGISTERED USERS LIST
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(allUsers) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SophSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=150" },
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SophActiveSelection)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        color = SophTextMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = user.email,
                                        color = SophTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (user.isAdmin) {
                                    Box(
                                        modifier = Modifier
                                            .background(SophError, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ADMIN",
                                            color = SophOnPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(SophActiveSelection, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MEMBER",
                                            color = SophPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// GOOGLE ACCOUNT SELECTOR SIMULATOR DIALOG
// ----------------------------------------------------------------------
@Composable
fun GoogleSignInSimulatorDialog(
    onAccountSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val simulatedAccounts = listOf(
        Pair("hilmanmaulana2100@gmail.com", "Hilman Maulana"),
        Pair("termuxer.cyber@gmail.com", "Termux Android Cyber"),
        Pair("budi.shellmaster@gmail.com", "Budi Hackmeister")
    )
    var customEmailInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SophSurface),
            border = BorderStroke(1.dp, SophPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Sign in with Google",
                    color = SophTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Pilih akun Google Anda yang terhubung pada perangkat ini:",
                    color = SophTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // List simulated device Google accounts
                simulatedAccounts.forEach { (email, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAccountSelected(email, name) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small colored letter avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SophPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1),
                                color = SophOnPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = name, color = SophTextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = email, color = SophTextSecondary, fontSize = 11.sp)
                        }
                    }
                    HorizontalDivider(color = SophActiveSelection.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Atau input Email Google Kustom:",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Custom accounts
                OutlinedTextField(
                    value = customEmailInput,
                    onValueChange = { customEmailInput = it },
                    placeholder = { Text("Contoh: hilman@gmail.com", color = SophTextSecondary.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophTextMain,
                        unfocusedTextColor = SophTextSecondary,
                        focusedBorderColor = SophPrimary,
                        unfocusedBorderColor = SophActiveSelection
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customNameInput,
                    onValueChange = { customNameInput = it },
                    placeholder = { Text("Nama Tampilan (Opsional)", color = SophTextSecondary.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophTextMain,
                        unfocusedTextColor = SophTextSecondary,
                        focusedBorderColor = SophPrimary,
                        unfocusedBorderColor = SophActiveSelection
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BATAL", color = SophTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customEmailInput.isNotEmpty()) {
                                onAccountSelected(customEmailInput, customNameInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                        shape = RoundedCornerShape(6.dp),
                        enabled = customEmailInput.isNotEmpty()
                    ) {
                        Text("MASUK", color = SophOnPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// DIALOG: CREATE KEY METHOD
// ----------------------------------------------------------------------
@Composable
fun CreateKeyDialog(
    onDismiss: () -> Unit,
    onGenerateKey: (String, String, String) -> Unit
) {
    var keyLabel by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("python") } // python, bash, nodejs
    var selectedService by remember { mutableStateOf("AI Assistant") } // AI Assistant, Web Scraper, Database Link, Universal Script

    val languages = listOf("bash", "python", "nodejs")
    val services = listOf("AI Assistant", "Web Scraper", "Database Link", "Universal Script")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SophSurface),
            border = BorderStroke(1.dp, SophActiveSelection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Generate API Key Baru",
                    color = SophTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Label Input
                OutlinedTextField(
                    value = keyLabel,
                    onValueChange = { keyLabel = it },
                    label = { Text("Nama/Label Kunci Skrip", color = SophTextSecondary) },
                    placeholder = { Text("Contoh: bot-spam-whatsapp", color = SophTextSecondary.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophTextMain,
                        unfocusedTextColor = SophTextSecondary,
                        focusedBorderColor = SophPrimary,
                        unfocusedBorderColor = SophActiveSelection
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Select Script Language
                Text(
                    text = "Bahasa Pemrograman Skrip:",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Card(
                            onClick = { selectedLanguage = lang },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SophPrimary.copy(alpha = 0.15f) else SophBackground
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SophPrimary else SophActiveSelection
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.uppercase(),
                                    color = if (isSelected) SophPrimary else SophTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Utility Service
                Text(
                    text = "Tipe Layanan API Key:",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Lazy wrap or simple Column flow for service types
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    services.forEach { service ->
                        val isSelected = selectedService == service
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SophActiveSelection else SophBackground)
                                .border(1.dp, if (isSelected) SophPrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { selectedService = service }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isSelected) SophPrimary else SophTextSecondary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = service,
                                color = if (isSelected) SophTextMain else SophTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BATAL", color = SophTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onGenerateKey(keyLabel, selectedLanguage, selectedService) },
                        colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("BUAT SEKARANG", color = SophOnPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// DIALOG: EDIT KEY DETAILS
// ----------------------------------------------------------------------
@Composable
fun EditKeyDialog(
    keyEntity: ApiKeyEntity,
    onDismiss: () -> Unit,
    onUpdateKey: (Int, String, String, String) -> Unit
) {
    var keyLabel by remember { mutableStateOf(keyEntity.label) }
    var selectedService by remember { mutableStateOf(keyEntity.serviceType) }
    var selectedStatus by remember { mutableStateOf(keyEntity.status) }

    val services = listOf("AI Assistant", "Web Scraper", "Database Link", "Universal Script")
    val statuses = listOf("ACTIVE", "EXPIRED", "REVOKED")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SophSurface),
            border = BorderStroke(1.dp, SophActiveSelection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Edit Detail API Key",
                    color = SophTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Label input
                OutlinedTextField(
                    value = keyLabel,
                    onValueChange = { keyLabel = it },
                    label = { Text("Nama/Label Kunci Skrip", color = SophTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SophTextMain,
                        unfocusedTextColor = SophTextSecondary,
                        focusedBorderColor = SophPrimary,
                        unfocusedBorderColor = SophActiveSelection
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Utility Service
                Text(
                    text = "Tipe Layanan API Key:",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    services.forEach { service ->
                        val isSelected = selectedService == service
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SophActiveSelection else SophBackground)
                                .border(1.dp, if (isSelected) SophPrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { selectedService = service }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isSelected) SophPrimary else SophTextSecondary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = service,
                                color = if (isSelected) SophTextMain else SophTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Status
                Text(
                    text = "Status API Key:",
                    color = SophTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statuses.forEach { status ->
                        val isSelected = selectedStatus == status
                        Card(
                            onClick = { selectedStatus = status },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    if (status == "ACTIVE") SophPrimary.copy(alpha = 0.15f) else SophError.copy(alpha = 0.15f)
                                } else SophBackground
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) {
                                    if (status == "ACTIVE") SophPrimary else SophError
                                } else Color.Transparent
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status,
                                    color = if (isSelected) {
                                        if (status == "ACTIVE") SophPrimary else SophError
                                    } else SophTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BATAL", color = SophTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onUpdateKey(keyEntity.id, keyLabel, selectedService, selectedStatus) },
                        colors = ButtonDefaults.buttonColors(containerColor = SophPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SIMPAN DETAIL", color = SophOnPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
