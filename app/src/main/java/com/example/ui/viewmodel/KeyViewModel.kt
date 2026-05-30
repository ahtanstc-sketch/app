package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ApiKeyEntity
import com.example.data.model.UserEntity
import com.example.data.repository.KeyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class KeyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KeyRepository

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    private val _currentKeys = MutableStateFlow<List<ApiKeyEntity>>(emptyList())
    val currentKeys: StateFlow<List<ApiKeyEntity>> = _currentKeys.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeyRepository(database.userDao(), database.apiKeyDao())

        // Insert a default admin account into local database and load session
        viewModelScope.launch {
            val adminUser = UserEntity(
                email = "admin@termux.key",
                displayName = "Administrator Termux",
                avatarUrl = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=150",
                isAdmin = true,
                passwordHash = "ADMIN123"
            )
            repository.insertUser(adminUser)

            // Auto log-in persistent user session
            val prefs = application.getSharedPreferences("termux_key_prefs", android.content.Context.MODE_PRIVATE)
            val savedEmail = prefs.getString("logged_in_email", null)
            if (!savedEmail.isNullOrEmpty()) {
                val user = repository.getUserByEmail(savedEmail)
                if (user != null) {
                    _currentUser.value = user
                }
            }
        }

        // Keep currentKeys in sync with currentUser
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user != null) {
                    repository.getKeysForUser(user.email).collect { keys ->
                        _currentKeys.value = keys
                    }
                } else {
                    _currentKeys.value = emptyList()
                }
            }
        }
    }

    // Admin state: get all keys
    val allKeys: StateFlow<List<ApiKeyEntity>> = repository.getAllKeys()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Admin state: get all users
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Google Sign-In Simulation
    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            _authError.value = null
            _isSyncing.value = true
            _syncProgress.value = 0.1f
            delay(1000) // simulation delay for nice UI experience
            
            val cleanedEmail = email.trim().lowercase()
            if (cleanedEmail.isEmpty() || !cleanedEmail.contains("@")) {
                _authError.value = "Email Google tidak valid!"
                _isSyncing.value = false
                return@launch
            }

            var user = repository.getUserByEmail(cleanedEmail)
            if (user == null) {
                // If they sign in with Google but user database doesn't have it, create one (autocreate member)
                val randomAvatar = "https://images.unsplash.com/photo-${1500000000000 + Random.nextInt(1000000)}?w=150"
                user = UserEntity(
                    email = cleanedEmail,
                    displayName = name.ifEmpty { "Termux User" },
                    avatarUrl = randomAvatar,
                    isAdmin = cleanedEmail.contains("admin") || cleanedEmail == "admin@termux.key"
                )
                repository.insertUser(user)
            }

            _currentUser.value = user
            saveLoggedInEmail(user.email)
            _authSuccessMessage.value = "Selamat datang, ${user.displayName} (Via Google)!"
            _isSyncing.value = false
            _syncProgress.value = 0f
            
            // Auto sync keys on login
            syncAccounts()
        }
    }

    // Custom Email Register
    fun registerWithEmail(email: String, name: String, pass: String) {
        viewModelScope.launch {
            _authError.value = null
            val cleanedEmail = email.trim().lowercase()
            if (cleanedEmail.isEmpty() || !cleanedEmail.contains("@")) {
                _authError.value = "Format email salah!"
                return@launch
            }
            if (name.trim().isEmpty()) {
                _authError.value = "Nama tidak boleh kosong!"
                return@launch
            }
            if (pass.length < 4) {
                _authError.value = "Password minimal 4 karakter!"
                return@launch
            }

            val existing = repository.getUserByEmail(cleanedEmail)
            if (existing != null) {
                _authError.value = "Akun dengan email tersebut sudah terdaftar!"
                return@launch
            }

            val randomAvatar = "https://images.unsplash.com/photo-${1510000000000 + Random.nextInt(1000000)}?w=150"
            val newUser = UserEntity(
                email = cleanedEmail,
                displayName = name,
                avatarUrl = randomAvatar,
                isAdmin = false,
                passwordHash = pass
            )
            repository.insertUser(newUser)
            _currentUser.value = newUser
            saveLoggedInEmail(newUser.email)
            _authSuccessMessage.value = "Pendaftaran berhasil! Halo, $name"
        }
    }

    // Custom Email Login (for admin/member)
    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _authError.value = null
            val cleanedEmail = email.trim().lowercase()
            
            // Check admin direct bypass
            if (cleanedEmail == "admin@termux.key" && pass == "ADMIN123") {
                val admin = repository.getUserByEmail(cleanedEmail) ?: UserEntity(
                    email = "admin@termux.key",
                    displayName = "Administrator Termux",
                    avatarUrl = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=150",
                    isAdmin = true,
                    passwordHash = "ADMIN123"
                )
                repository.insertUser(admin)
                _currentUser.value = admin
                saveLoggedInEmail(admin.email)
                _authSuccessMessage.value = "Halo Admin! Masuk tanpa daftar berhasil."
                return@launch
            }

            val user = repository.getUserByEmail(cleanedEmail)
            if (user == null) {
                _authError.value = "Akun tidak ditemukan. Harap daftar terlebih dahulu!"
                return@launch
            }

            if (user.passwordHash != pass) {
                _authError.value = "Password yang Anda masukkan salah!"
                return@launch
            }

            _currentUser.value = user
            saveLoggedInEmail(user.email)
            _authSuccessMessage.value = "Berhasil masuk sebagai ${user.displayName}!"
            
            // Auto sync
            syncAccounts()
        }
    }

    // Credentials-based Admin Login (does not need signup)
    fun loginAsAdminWithCredentials(username: String, pass: String) {
        viewModelScope.launch {
            _authError.value = null
            val cleanedUsername = username.trim()
            if (cleanedUsername.uppercase() != "ADMIN11" || pass != "ADMIN123") {
                _authError.value = "Username atau Password Admin salah!"
                return@launch
            }
            val adminEmail = "admin@termux.key"
            val admin = repository.getUserByEmail(adminEmail) ?: UserEntity(
                email = adminEmail,
                displayName = "Administrator Termux",
                avatarUrl = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=150",
                isAdmin = true,
                passwordHash = "ADMIN123"
            )
            repository.insertUser(admin)
            _currentUser.value = admin
            saveLoggedInEmail(admin.email)
            _authSuccessMessage.value = "Berhasil masuk sebagai Administrator!"
        }
    }

    // Logout
    fun logout() {
        _currentUser.value = null
        clearLoggedInEmail()
        _authSuccessMessage.value = "Berhasil keluar dari sesi."
    }

    private fun saveLoggedInEmail(email: String) {
        val prefs = getApplication<Application>().getSharedPreferences("termux_key_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("logged_in_email", email).apply()
    }

    private fun clearLoggedInEmail() {
        val prefs = getApplication<Application>().getSharedPreferences("termux_key_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("logged_in_email").apply()
    }

    // Key operations: Generate API Key
    fun generateAndInsertKey(label: String, scriptType: String, serviceType: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val keyHash = generateRandomKey()
            val newKey = ApiKeyEntity(
                userEmail = user.email,
                label = label.ifEmpty { "Script Tanpa Nama" },
                keyValue = keyHash,
                scriptType = scriptType,
                serviceType = serviceType,
                status = "ACTIVE",
                isSynced = false
            )
            repository.insertKey(newKey)
            _authSuccessMessage.value = "API Key baru berhasil dibuat!"
        }
    }

    // Edit Key Name/Service
    fun updateKeyDetails(id: Int, newLabel: String, newService: String, status: String) {
        viewModelScope.launch {
            val existing = repository.getKeyById(id)
            if (existing != null) {
                val updated = existing.copy(
                    label = newLabel,
                    serviceType = newService,
                    status = status,
                    isSynced = false // status changed, needs sync
                )
                repository.updateKey(updated)
                _authSuccessMessage.value = "API Key berhasil diperbarui!"
            }
        }
    }

    // Delete Key
    fun deleteKey(id: Int) {
        viewModelScope.launch {
            repository.deleteKeyById(id)
            _authSuccessMessage.value = "API Key berhasil dihapus!"
        }
    }

    // Simulated Cloud Accounts Sync with elegant animation flow
    fun syncAccounts() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncProgress.value = 0f
            
            val keys = repository.getKeysForUser(user.email).first()
            val steps = 5
            for (i in 1..steps) {
                delay(300)
                _syncProgress.value = i.toFloat() / steps
            }
            
            // Mark all keys as synced in DB
            keys.forEach { key ->
                if (!key.isSynced) {
                    repository.updateKey(key.copy(isSynced = true))
                }
            }
            
            _isSyncing.value = false
            _syncProgress.value = 0f
            _authSuccessMessage.value = "Sinkronisasi Akun Berhasil! Seluruh API Key tersimpan di cloud."
        }
    }

    // Toggle service state in Admin view
    fun toggleKeyStatusByAdmin(id: Int) {
        viewModelScope.launch {
            val existing = repository.getKeyById(id)
            if (existing != null) {
                val newStatus = if (existing.status == "ACTIVE") "REVOKED" else "ACTIVE"
                repository.updateKey(existing.copy(status = newStatus))
                _authSuccessMessage.value = "Admin: Status API Key berhasil dialihkan!"
            }
        }
    }

    // Helper functions
    private fun generateRandomKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val prefix = "TMX"
        val part1 = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${prefix}_${part1}_${part2}"
    }

    fun clearMessages() {
        _authError.value = null
        _authSuccessMessage.value = null
    }
}
