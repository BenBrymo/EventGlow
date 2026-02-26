package com.example.eventglow.settings

import android.app.Application
import com.example.eventglow.common.BaseViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class ChangePasswordViewModel(application: Application) : BaseViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _showCurrent = MutableStateFlow(false)
    val showCurrent: StateFlow<Boolean> = _showCurrent.asStateFlow()

    private val _showNew = MutableStateFlow(false)
    val showNew: StateFlow<Boolean> = _showNew.asStateFlow()

    private val _showConfirm = MutableStateFlow(false)
    val showConfirm: StateFlow<Boolean> = _showConfirm.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun onCurrentPasswordChanged(value: String) {
        _currentPassword.value = value
    }

    fun onNewPasswordChanged(value: String) {
        _newPassword.value = value
    }

    fun onConfirmPasswordChanged(value: String) {
        _confirmPassword.value = value
    }

    fun toggleShowCurrent() {
        _showCurrent.value = !_showCurrent.value
    }

    fun toggleShowNew() {
        _showNew.value = !_showNew.value
    }

    fun toggleShowConfirm() {
        _showConfirm.value = !_showConfirm.value
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun changePassword() {
        clearError()
        _successMessage.value = null

        val current = _currentPassword.value.trim()
        val newPass = _newPassword.value.trim()
        val confirm = _confirmPassword.value.trim()

        when {
            current.isEmpty() || newPass.isEmpty() || confirm.isEmpty() -> {
                setFailure("Please fill all password fields.")
                return
            }

            newPass.length < 6 -> {
                setFailure("New password must be at least 6 characters.")
                return
            }

            newPass != confirm -> {
                setFailure("New password and confirmation do not match.")
                return
            }

            current == newPass -> {
                setFailure("New password must be different from current password.")
                return
            }
        }

        launchSafely(tag = "AdminChangePasswordViewModel") {
            setLoading()
            val user = auth.currentUser
                ?: throw IllegalStateException("No signed-in admin account found.")
            val email = user.email?.trim().orEmpty()
            if (email.isBlank()) throw IllegalStateException("No account email found.")

            val credential = EmailAuthProvider.getCredential(email, current)
            user.reauthenticate(credential).await()
            user.updatePassword(newPass).await()

            _currentPassword.value = ""
            _newPassword.value = ""
            _confirmPassword.value = ""
            _successMessage.value = "Password changed successfully."
            setSuccess()
        }
    }
}
