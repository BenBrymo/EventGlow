package com.example.eventglow.common

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    private data class ThrottleState(
        var failedAttempts: Int = 0,
        var lockoutUntilMillis: Long = 0L
    )

    private val throttleStates = mutableMapOf<String, ThrottleState>()

    private val _loadState = MutableStateFlow(LoadState.SUCCESS)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    protected fun setLoading() {
        _loadState.value = LoadState.LOADING
        _errorMessage.value = null
    }

    protected fun setSuccess() {
        _loadState.value = LoadState.SUCCESS
    }

    protected fun setFailure(message: String?) {
        _loadState.value = LoadState.FAILURE
        _errorMessage.value = message ?: "Something went wrong"
    }

    fun clearError() {
        _errorMessage.value = null
    }

    protected fun launchSafely(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        tag: String = "BaseViewModel",
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                block()
            } catch (cancellation: CancellationException) {
                Log.d(tag, "Coroutine cancelled")
                throw cancellation
            } catch (t: Throwable) {
                Log.e(tag, "Unhandled error", t)
                setFailure(resolveErrorMessage(t))
                onError?.invoke(t)
            }
        }
    }

    protected fun launchWhenConnected(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        tag: String = "BaseViewModel",
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        if (!isNetworkAvailable()) {
            setFailure("No internet connection. Check your network and try again.")
            return
        }
        launchSafely(
            dispatcher = dispatcher,
            tag = tag,
            onError = onError,
            block = block
        )
    }

    protected fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(getApplication())
    }

    protected fun resolveErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthException -> {
                when (throwable.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "The email address is invalid."
                    "ERROR_WRONG_PASSWORD" -> "Wrong username or password."
                    "ERROR_INVALID_CREDENTIAL" -> "Wrong username or password."
                    "INVALID_LOGIN_CREDENTIALS" -> "Wrong username or password."
                    "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                    "ERROR_USER_DISABLED" -> "User account has been disabled."
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Please try again later."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "Operation not allowed. Please enable sign-in method."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use."
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak."
                    else -> {
                        val raw = throwable.localizedMessage.orEmpty()
                        if (raw.contains("The supplied auth", ignoreCase = true) ||
                            raw.contains("invalid login credentials", ignoreCase = true)
                        ) {
                            "Wrong username or password."
                        } else {
                            raw.ifBlank { "Authentication error occurred." }
                        }
                    }
                }
            }

            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Permission denied."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "Service unavailable. Please try again."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "Request timed out. Please try again."
                    FirebaseFirestoreException.Code.NOT_FOUND -> "Requested data was not found."
                    FirebaseFirestoreException.Code.CANCELLED -> "Request was cancelled."
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "Quota exceeded. Try again later."
                    else -> {
                        val raw = throwable.message.orEmpty()
                        when {
                            raw.contains("blocked all requests", ignoreCase = true) ->
                                "Firestore blocked requests for this app. Check App Check, Firestore rules, and project configuration."

                            raw.contains("missing or insufficient permissions", ignoreCase = true) ->
                                "Permission denied. Check Firestore security rules for this operation."

                            raw.contains("quota", ignoreCase = true) || raw.contains("exceeded", ignoreCase = true) ->
                                "Firestore quota/rate limit reached. Try again later."

                            raw.contains("app check", ignoreCase = true) ->
                                "App Check rejected this request. Verify App Check setup for your debug/development build."

                            else -> raw.ifBlank { "Firestore request failed." }
                        }
                    }
                }
            }

            is FirebaseNetworkException -> "No internet connection. Check your network and try again."
            is IllegalArgumentException -> throwable.message ?: "Invalid request."
            else -> throwable.message ?: "Something went wrong"
        }
    }

    protected fun getThrottleLockMessage(throttleKey: String): String? {
        val state = throttleStates[throttleKey] ?: return null
        val now = System.currentTimeMillis()
        if (now >= state.lockoutUntilMillis) return null
        val secondsLeft = ((state.lockoutUntilMillis - now) / 1000L).coerceAtLeast(1L)
        return "Too many failed attempts. Try again in ${secondsLeft}s."
    }

    protected fun resolveThrottledFailureMessage(
        throttleKey: String,
        isThrottleCandidate: Boolean,
        baseMessage: String,
        maxFailedAttempts: Int = 2,
        lockoutDurationMillis: Long = 60_000L
    ): String {
        if (!isThrottleCandidate) return baseMessage

        val state = throttleStates.getOrPut(throttleKey) { ThrottleState() }
        val now = System.currentTimeMillis()

        if (now < state.lockoutUntilMillis) {
            val secondsLeft = ((state.lockoutUntilMillis - now) / 1000L).coerceAtLeast(1L)
            return "Too many failed attempts. Try again in ${secondsLeft}s."
        }

        state.failedAttempts += 1

        if (state.failedAttempts >= maxFailedAttempts) {
            state.failedAttempts = 0
            state.lockoutUntilMillis = now + lockoutDurationMillis
            return "Too many failed attempts. Try again in ${lockoutDurationMillis / 1000L}s."
        }

        val attemptsLeft = (maxFailedAttempts - state.failedAttempts).coerceAtLeast(0)
        return "$baseMessage $attemptsLeft attempt left."
    }

    protected fun clearThrottle(throttleKey: String) {
        throttleStates.remove(throttleKey)
    }

}
