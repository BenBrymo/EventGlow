package com.example.eventglow.dataClass

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserPreferences(application: Application) : AndroidViewModel(application) {

    // Create a private SharedPreferences instance
    private val sharedPreferences =
        getApplication<Application>().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    val gson = Gson()

    // Save user info, bookmarks, and favorite events
    fun saveUserInfo(
        email: String,
        userName: String,
        profilePictureUrl: String?,
        headerPictureUrl: String?,
        role: String,
        boughtTickets: List<BoughtTicket>,
        filteredEvents: List<Event>,
        bookmarks: List<Event>,
        favoriteEvents: List<Event>
    ) {
        with(sharedPreferences.edit()) {
            putString("USER_EMAIL", email)
            putString("USERNAME", userName)
            putString("PROFILE_PICTURE_URL", profilePictureUrl)
            putString("HEADER_PICTURE_URL", headerPictureUrl)
            putString("ROLE", role)
            putString("BOUGHT_TICKETS", gson.toJson(boughtTickets)) // Convert list of BoughtTicket to JSON
            putString("FILTERED_EVENTS", gson.toJson(filteredEvents))
            putString("BOOKMARKS", gson.toJson(bookmarks)) // Convert list of bookmarks (IDs) to JSON
            putString("FAVORITE_EVENTS", gson.toJson(favoriteEvents)) // Convert list of favorite event IDs to JSON
            apply() // Start saving process
        }
    }

    // Retrieve user info
    fun getUserInfo(): Map<String, String?> {
        return mapOf(
            "USER_EMAIL" to sharedPreferences.getString("USER_EMAIL", null),
            "USERNAME" to sharedPreferences.getString("USERNAME", null),
            "PROFILE_PICTURE_URL" to sharedPreferences.getString("PROFILE_PICTURE_URL", null),
            "HEADER_PICTURE_URL" to sharedPreferences.getString("HEADER_PICTURE_URL", null),
            "ROLE" to sharedPreferences.getString("ROLE", null),
            "BOUGHT_TICKETS" to sharedPreferences.getString("BOUGHT_TICKETS", null),
            "FILTERED_EVENTS" to sharedPreferences.getString("FILTERED_EVENTS", null),
            "BOOKMARKS" to sharedPreferences.getString("BOOKMARKS", null),
            "FAVORITE_EVENTS" to sharedPreferences.getString("FAVORITE_EVENTS", null)
        )
    }

    // Method to clear all shared preferences
    fun clearAllData() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }

    // Retrieve list of BoughtTicket
    fun getBoughtTickets(): List<BoughtTicket>? {
        val json = sharedPreferences.getString("BOUGHT_TICKETS", null)
        return if (json != null) {
            val type = object : TypeToken<List<BoughtTicket>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    fun getFilteredEvents(): List<Event>? {
        val json = sharedPreferences.getString("FILTERED_EVENTS", null)
        return if (json != null) {
            val type = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    fun getBookmarks(): List<Event>? {
        val json = sharedPreferences.getString("BOOKMARKS", null)
        return if (json != null) {
            val type = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    fun getFavoriteEvents(): List<Event>? {
        val json = sharedPreferences.getString("FAVORITE_EVENTS", null)
        return if (json != null) {
            val type = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    // Update methods
    fun resetFilteredEventsToEmpty() {
        val filteredEvents = emptyList<Event>()
        with(sharedPreferences.edit()) {
            putString("FILTERED_EVENTS", gson.toJson(filteredEvents))
            apply() // Apply edits
        }
    }

    fun updateUsername(newUsername: String) {
        with(sharedPreferences.edit()) {
            putString("USERNAME", newUsername)
            apply() // Apply edits
        }
    }

    fun updateProfileImageUrl(newProfileImageUrl: String?) {
        try {
            with(sharedPreferences.edit()) {
                putString("PROFILE_PICTURE_URL", newProfileImageUrl)
                apply() // Save changes asynchronously
            }
        } catch (e: Exception) {
            Log.e("UserPreferences", "Failed to update profile image URL", e)
        }
    }

    fun updateHeaderImageUrl(newHeaderImageUrl: String?) {
        try {
            with(sharedPreferences.edit()) {
                putString("HEADER_PICTURE_URL", newHeaderImageUrl)
                apply() // Save changes asynchronously
            }
        } catch (e: Exception) {
            Log.e("UserPreferences", "Failed to update header image URL", e)
        }
    }

    fun addTicketToBoughtTickets(newTicket: BoughtTicket) {
        // Retrieve existing list of bought tickets
        val currentTickets = getBoughtTickets()?.toMutableList() ?: mutableListOf()

        // Add the new ticket to the list
        currentTickets.add(newTicket)

        // Save the updated list back to SharedPreferences
        with(sharedPreferences.edit()) {
            putString("BOUGHT_TICKETS", gson.toJson(currentTickets)) // Convert list of BoughtTicket to JSON
            apply() // Start saving process
        }
    }

    fun keepFilteredEventsInSharedPref(filteredEvents: List<Event>) {
        // Save the updated list back to SharedPreferences
        with(sharedPreferences.edit()) {
            putString("FILTERED_EVENTS", gson.toJson(filteredEvents)) // Convert list of Event to JSON
            apply() // Start saving process
        }
    }

    fun addBookmark(bookmark: Event) {
        val currentBookmarks = getBookmarks()?.toMutableList() ?: mutableListOf()
        if (!currentBookmarks.contains(bookmark)) {
            currentBookmarks.add(bookmark)
            with(sharedPreferences.edit()) {
                putString("BOOKMARKS", gson.toJson(currentBookmarks)) // Convert list of bookmarks to JSON
                apply() // Start saving process
            }
        }
    }

    fun removeBookmark(bookmark: Event) {
        val currentBookmarks = getBookmarks()?.toMutableList() ?: mutableListOf()
        if (currentBookmarks.contains(bookmark)) {
            currentBookmarks.remove(bookmark)
            with(sharedPreferences.edit()) {
                putString("BOOKMARKS", gson.toJson(currentBookmarks)) // Convert list of bookmarks to JSON
                apply() // Start saving process
            }
        }
    }

    fun addFavoriteEvent(favouriteEvent: Event) {
        val currentFavorites = getFavoriteEvents()?.toMutableList() ?: mutableListOf()
        if (!currentFavorites.contains(favouriteEvent)) {
            currentFavorites.add(favouriteEvent)
            with(sharedPreferences.edit()) {
                putString(
                    "FAVORITE_EVENTS",
                    gson.toJson(currentFavorites)
                ) // Convert list of favorite event IDs to JSON
                apply() // Start saving process
            }
        }
    }

    fun removeFavoriteEvent(favouriteEvent: Event) {
        val currentFavorites = getFavoriteEvents()?.toMutableList() ?: mutableListOf()
        if (currentFavorites.contains(favouriteEvent)) {
            currentFavorites.remove(favouriteEvent)
            with(sharedPreferences.edit()) {
                putString(
                    "FAVORITE_EVENTS",
                    gson.toJson(currentFavorites)
                ) // Convert list of favorite event IDs to JSON
                apply() // Start saving process
            }
        }
    }
}
