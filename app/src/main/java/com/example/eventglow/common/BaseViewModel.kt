package com.example.eventglow.common

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

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

    private fun resolveErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Permission denied."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "Service unavailable. Please try again."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "Request timed out. Please try again."
                    FirebaseFirestoreException.Code.NOT_FOUND -> "Requested data was not found."
                    FirebaseFirestoreException.Code.CANCELLED -> "Request was cancelled."
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "Quota exceeded. Try again later."
                    else -> throwable.message ?: "Firestore request failed."
                }
            }

            is FirebaseNetworkException -> "No internet connection. Check your network and try again."
            is IllegalArgumentException -> throwable.message ?: "Invalid request."
            else -> throwable.message ?: "Something went wrong"
        }
    }

}
