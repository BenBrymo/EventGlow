package com.example.eventglow.admin_main_screen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = UserPreferences(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun updateUsernameInSharedPreferences(username: String) {
        sharedPreferences.updateUsername(username)
    }

    fun updateProfileImageUrlInSharedPreferences(newProfileImageUrl: String?) {
        sharedPreferences.updateProfileImageUrl(newProfileImageUrl)
    }

    fun updateHeaderInSharedPreferences(newHeaderImageUrl: String?) {
        sharedPreferences.updateHeaderImageUrl(newHeaderImageUrl)
    }

    // Function to update profilePictureUrl in Firestore
    fun updateProfilePictureUrlInFirestore(newProfileImageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val userDocRef = firestore.collection("users").document(userId)
            userDocRef.update("profilePictureUrl", newProfileImageUrl)
                .addOnSuccessListener {
                    // Update successful
                    Log.d("AdminViewModel", "Saved new profile image to firestore")
                }
                .addOnFailureListener { exception ->
                    // Handle the error
                    Log.d("AdminViewModel", "Failed to saved new profile image to firestore $exception.message")
                }
        }
    }

    // Function to fetch profilePictureUrl from Firestore
    suspend fun fetchProfilePictureUrlFromFirestore(): String? {
        val userId = auth.currentUser?.uid ?: return null

        return try {
            val userDocRef = firestore.collection("users").document(userId)
            val documentSnapshot = userDocRef.get().await()
            if (documentSnapshot.exists()) {
                documentSnapshot.getString("profilePictureUrl")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("AdminViewModel", "Failed to fetch profile image from Firestore: ${e.message}")
            null
        }
    }


}
