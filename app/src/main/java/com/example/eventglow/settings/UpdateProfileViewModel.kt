package com.example.eventglow.settings

import android.app.Application
import android.util.Patterns
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UpdateProfileViewModel(application: Application) : BaseViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sharedPreferences = UserPreferences(application)

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _initialFullName = MutableStateFlow("")
    private val _initialEmail = MutableStateFlow("")

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadProfile()
    }

    fun onFullNameChange(value: String) {
        _fullName.value = value
    }

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun loadProfile() {
        clearError()
        _successMessage.value = null
        val cached = sharedPreferences.getUserInfo()
        val cachedName = cached["USERNAME"]?.trim().orEmpty()
        val cachedEmail = cached["USER_EMAIL"]?.trim().orEmpty()

        if (cachedName.isNotBlank() || cachedEmail.isNotBlank()) {
            _fullName.value = cachedName
            _email.value = cachedEmail
            _initialFullName.value = cachedName
            _initialEmail.value = cachedEmail
        }

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            if (cachedName.isBlank() && cachedEmail.isBlank()) {
                setFailure("No signed-in admin account found.")
            }
            return
        }

        launchWhenConnected(tag = "AdminUpdateProfileViewModel.loadProfile") {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val serverName = snapshot.getString("username")?.trim().orEmpty()
            val serverEmail = snapshot.getString("email")?.trim().orEmpty()
            val resolvedName = if (serverName.isNotBlank()) serverName else cachedName
            val resolvedEmail = if (serverEmail.isNotBlank()) serverEmail else cachedEmail

            _fullName.value = resolvedName
            _email.value = resolvedEmail
            _initialFullName.value = resolvedName
            _initialEmail.value = resolvedEmail

            if (serverName.isNotBlank()) {
                sharedPreferences.updateUsername(serverName)
            }
            if (serverEmail.isNotBlank()) {
                sharedPreferences.updateUserEmail(serverEmail)
            }
            setSuccess()
        }
    }

    fun saveProfile() {
        clearError()
        _successMessage.value = null

        val trimmedName = _fullName.value.trim()
        val trimmedEmail = _email.value.trim()

        when {
            trimmedName.isBlank() || trimmedEmail.isBlank() -> {
                setFailure("Name and email are required.")
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                setFailure("Invalid email address.")
                return
            }

            trimmedName == _initialFullName.value && trimmedEmail == _initialEmail.value -> {
                setFailure("No changes to save.")
                return
            }
        }

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            setFailure("No signed-in admin account found.")
            return
        }

        launchWhenConnected(tag = "AdminUpdateProfileViewModel.saveProfile") {
            setLoading()
            val normalizedName = trimmedName.lowercase()
            val usernameQuery = firestore.collection("users")
                .whereEqualTo("username", trimmedName)
                .get()
                .await()
            val normalizedQuery = if (normalizedName != trimmedName) {
                firestore.collection("users")
                    .whereEqualTo("username", normalizedName)
                    .get()
                    .await()
            } else {
                null
            }

            val hasConflict = usernameQuery.documents.any { it.id != uid } ||
                    (normalizedQuery?.documents?.any { it.id != uid } == true)
            if (hasConflict) {
                setFailure("Username is already taken.")
                return@launchWhenConnected
            }

            firestore.collection("users")
                .document(uid)
                .update(
                    mapOf(
                        "username" to trimmedName,
                        "email" to trimmedEmail
                    )
                )
                .await()

            val authUser = auth.currentUser
            if (authUser != null && authUser.email?.trim() != trimmedEmail) {
                try {
                    authUser.updateEmail(trimmedEmail).await()
                } catch (e: Exception) {
                    setFailure(
                        "Profile saved, but auth email update failed. Re-login may be required: ${resolveErrorMessage(e)}"
                    )
                    sharedPreferences.updateUsername(trimmedName)
                    sharedPreferences.updateUserEmail(trimmedEmail)
                    _initialFullName.value = trimmedName
                    _initialEmail.value = trimmedEmail
                    return@launchWhenConnected
                }
            }

            sharedPreferences.updateUsername(trimmedName)
            sharedPreferences.updateUserEmail(trimmedEmail)
            _initialFullName.value = trimmedName
            _initialEmail.value = trimmedEmail
            _successMessage.value = "Profile updated successfully."
            setSuccess()
        }
    }
}
