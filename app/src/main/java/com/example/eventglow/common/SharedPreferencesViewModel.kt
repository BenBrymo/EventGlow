package com.example.eventglow.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.eventglow.dataClass.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedPreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = UserPreferences(application)

    //MutableStateFlow to keep sharedPreference Map
    private val _userInfo = MutableStateFlow(sharedPreferences.getUserInfo())
    val userInfo: StateFlow<Map<String, String?>> = _userInfo

}