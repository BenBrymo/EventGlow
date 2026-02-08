package com.example.eventglow

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.eventts.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    //created an object of UserPreferences class
    private val sharedPreferences = UserPreferences(application)

    //get instance of FirebaseAuth Class
    private val firebaseAuth = FirebaseAuth.getInstance()

    // MutableStateFlow to hold and update the login state
    private val _persistentLoginState = MutableStateFlow<PersistentLoginState>(PersistentLoginState.SignedOut)
    val persistentLoginState: StateFlow<PersistentLoginState> = _persistentLoginState

    init {
        checkIfUserIsLoggedIn()
    }

    private fun checkIfUserIsLoggedIn() {
        //retrives current user from firebase
        val currentUser = firebaseAuth.currentUser
        //if there is a user
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                // State is set to signedIn with user as argument
                _persistentLoginState.value = PersistentLoginState.SignedIn(currentUser)
            } else {
                // Optionally prompt the user to verify their email or take appropriate action
                _persistentLoginState.value = PersistentLoginState.EmailNotVerified
            }
        } else {
            // State is set to signedOut
            _persistentLoginState.value = PersistentLoginState.SignedOut
        }
    }

    // Handle sign out
    fun signOut(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        try {
            firebaseAuth.signOut()
            sharedPreferences.clearAllData()
            Log.d("MainActivityModel", "User's Shared Preferences has been cleared")
            _persistentLoginState.value = PersistentLoginState.SignedOut
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }
}


sealed class PersistentLoginState() {
    data class SignedIn(val user: FirebaseUser) : PersistentLoginState()
    data object SignedOut : PersistentLoginState()
    data object EmailNotVerified : PersistentLoginState()
}