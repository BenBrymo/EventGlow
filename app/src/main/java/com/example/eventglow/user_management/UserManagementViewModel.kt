package com.example.eventglow.user_management

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.BuildConfig
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

class UserManagementViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val auth = FirebaseAuth.getInstance()
    private val adminApi: SupabaseAdminApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://placeholder.functions.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseAdminApi::class.java)
    }

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _searchQueryUser = MutableStateFlow("")
    val searchQueryUser: StateFlow<String> = _searchQueryUser.asStateFlow()

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAddingUser = MutableStateFlow(false)
    val isAddingUser: StateFlow<Boolean> = _isAddingUser.asStateFlow()

    private val _addUserError = MutableStateFlow<String?>(null)
    val addUserError: StateFlow<String?> = _addUserError.asStateFlow()

    private val _addUserSuccess = MutableStateFlow<String?>(null)
    val addUserSuccess: StateFlow<String?> = _addUserSuccess.asStateFlow()

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = usersCollection.get().await()
                val userList = result.documents.map { document ->
                    mapUser(document.id, document.data.orEmpty())
                }
                _users.value = userList
                applyFilter(_searchQueryUser.value)
            } catch (e: Exception) {
                _addUserError.value = e.message ?: "Failed to fetch users."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAddUserFeedback() {
        _addUserError.value = null
        _addUserSuccess.value = null
    }

    fun onSearchQueryChangeUser(query: String) {
        _searchQueryUser.value = query
        applyFilter(query)
    }

    fun addUser(email: String, password: String, username: String, role: String) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            val trimmedEmail = email.trim()
            val trimmedRole = role.trim().ifBlank { "user" }

            if (trimmedUsername.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
                _addUserError.value = "Please fill all fields."
                return@launch
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                _addUserError.value = "Invalid email address."
                return@launch
            }
            if (password.length < 6) {
                _addUserError.value = "Password must be at least 6 characters."
                return@launch
            }

            _isAddingUser.value = true
            _addUserError.value = null
            _addUserSuccess.value = null
            try {
                val isTaken = checkIfUsernameIsTaken(trimmedUsername)
                if (isTaken) {
                    _addUserError.value = "Username is already taken."
                    return@launch
                }

                createUserViaAdminFunction(
                    username = trimmedUsername,
                    email = trimmedEmail,
                    password = password,
                    role = trimmedRole
                )
                _addUserSuccess.value = "Account created successfully."
                fetchUsers()
            } catch (e: Exception) {
                _addUserError.value = e.message ?: "Failed to create account."
            } finally {
                _isAddingUser.value = false
            }
        }
    }

    suspend fun checkIfUsernameIsTaken(username: String): Boolean {
        val trimmed = username.trim()
        if (trimmed.isBlank()) return false
        val exactMatch = usersCollection.whereEqualTo("username", trimmed).get().await()
        if (!exactMatch.isEmpty) return true
        val lowerMatch = usersCollection.whereEqualTo("username", trimmed.lowercase()).get().await()
        return !lowerMatch.isEmpty
    }

    private suspend fun createUserViaAdminFunction(
        username: String,
        email: String,
        password: String,
        role: String
    ) {
        val functionsBaseUrl = BuildConfig.SUPABASE_FUNCTIONS_BASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_FUNCTIONS_ANON_KEY.trim()
        val createPath = BuildConfig.SUPABASE_FUNCTIONS_CREATE_USER_PATH.trim().trimStart('/')
        if (functionsBaseUrl.isBlank() || anonKey.isBlank() || createPath.isBlank()) {
            throw IllegalStateException(
                "Create-user config missing. Set SUPABASE_FUNCTIONS_BASE_URL, SUPABASE_FUNCTIONS_ANON_KEY and SUPABASE_FUNCTIONS_CREATE_USER_PATH."
            )
        }

        val currentUser = auth.currentUser ?: throw IllegalStateException("Admin must be signed in.")
        val idToken = currentUser.getIdToken(true).await().token?.trim().orEmpty()
        if (idToken.isBlank()) throw IllegalStateException("Failed to obtain admin token.")

        val response = adminApi.createUser(
            url = "$functionsBaseUrl/$createPath",
            apiKey = anonKey,
            authorization = "Bearer $anonKey",
            body = CreateUserAdminRequest(
                firebaseIdToken = idToken,
                username = username,
                email = email,
                password = password,
                role = role
            )
        )

        if (!response.isSuccessful) {
            val errorText = response.errorBody()?.string().orEmpty()
            val backendMessage = parseBackendError(errorText)
            throw IllegalStateException("Create user failed [HTTP ${response.code()}]: $backendMessage")
        }
    }

    private fun applyFilter(query: String) {
        val normalized = query.trim()
        val filteredList = if (normalized.isBlank()) {
            _users.value
        } else {
            _users.value.filter { user ->
                user.userName.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
            }
        }
        _filteredUsers.value = filteredList
    }

    private fun mapUser(id: String, data: Map<String, Any>): User {
        val boughtTickets = (data["boughtTickets"] as? List<Map<String, Any>>).orEmpty().map { ticket ->
            BoughtTicket(
                transactionReference = ticket["transactionReference"] as? String ?: "",
                eventOrganizer = ticket["eventOrganizer"] as? String ?: "",
                eventId = ticket["eventId"] as? String ?: "",
                eventName = ticket["eventName"] as? String ?: "",
                startDate = ticket["startDate"] as? String ?: "",
                eventStatus = ticket["eventStatus"] as? String ?: "",
                endDate = ticket["endDate"] as? String ?: "",
                imageUrl = ticket["imageUrl"] as? String,
                ticketName = ticket["ticketName"] as? String ?: "",
                ticketPrice = ticket["ticketPrice"] as? String ?: ""
            )
        }

        val bookmarkEvents =
            ((data["bookmarkEvents"] ?: data["bookmarks"]) as? List<Map<String, Any>>).orEmpty().map { event ->
                Event(
                    id = event["id"] as? String ?: "",
                    eventName = event["eventName"] as? String ?: "",
                    startDate = event["startDate"] as? String ?: "",
                    endDate = event["endDate"] as? String ?: "",
                    isMultiDayEvent = event["isMultiDayEvent"] as? Boolean ?: false,
                    eventTime = event["eventTime"] as? String ?: "",
                    durationLabel = event["durationLabel"] as? String ?: "",
                    durationMinutes = (event["durationMinutes"] as? Number)?.toInt() ?: 0,
                    eventVenue = event["eventVenue"] as? String ?: "",
                    eventStatus = event["eventStatus"] as? String ?: "",
                    isImportant = event["isImportant"] as? Boolean ?: false,
                    eventCategory = event["eventCategory"] as? String ?: "",
                    imageUri = event["imageUri"] as? String ?: "",
                    isDraft = event["isDraft"] as? Boolean ?: false,
                    eventOrganizer = event["eventOrganizer"] as? String ?: "",
                    eventDescription = event["eventDescription"] as? String ?: "",
                    createdAtMs = (event["createdAtMs"] as? Number)?.toLong() ?: 0L
                )
            }

        val favoriteEvents =
            ((data["favoriteEvents"] ?: data["favouriteEvents"]) as? List<Map<String, Any>>).orEmpty().map { event ->
                Event(
                    id = event["id"] as? String ?: "",
                    eventName = event["eventName"] as? String ?: "",
                    startDate = event["startDate"] as? String ?: "",
                    endDate = event["endDate"] as? String ?: "",
                    isMultiDayEvent = event["isMultiDayEvent"] as? Boolean ?: false,
                    eventTime = event["eventTime"] as? String ?: "",
                    durationLabel = event["durationLabel"] as? String ?: "",
                    durationMinutes = (event["durationMinutes"] as? Number)?.toInt() ?: 0,
                    eventVenue = event["eventVenue"] as? String ?: "",
                    eventStatus = event["eventStatus"] as? String ?: "",
                    isImportant = event["isImportant"] as? Boolean ?: false,
                    eventCategory = event["eventCategory"] as? String ?: "",
                    imageUri = event["imageUri"] as? String ?: "",
                    isDraft = event["isDraft"] as? Boolean ?: false,
                    eventOrganizer = event["eventOrganizer"] as? String ?: "",
                    eventDescription = event["eventDescription"] as? String ?: "",
                    createdAtMs = (event["createdAtMs"] as? Number)?.toLong() ?: 0L
                )
            }

        return User(
            id = id,
            userName = data["username"] as? String ?: "",
            email = data["email"] as? String ?: "",
            role = data["role"] as? String ?: "user",
            notificationsEnabled = data["notificationsEnabled"] as? Boolean ?: true,
            fcmToken = data["fcmToken"] as? String,
            profilePictureUrl = data["profilePictureUrl"] as? String,
            headerPictureUrl = data["headerPictureUrl"] as? String,
            boughtTickets = boughtTickets,
            bookmarkEvents = bookmarkEvents,
            favoriteEvents = favoriteEvents
        )
    }

    fun updateNotificationsEnabled(userId: String, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                usersCollection.document(userId).update("notificationsEnabled", enabled).await()
            }.onSuccess {
                fetchUsers()
            }.onFailure {
                _addUserError.value = it.message ?: "Failed to update user."
            }
        }
    }
}

private fun parseBackendError(raw: String): String {
    if (raw.isBlank()) return "Unknown backend error."
    return try {
        val json = JsonParser.parseString(raw)
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            val direct = obj.get("error")?.asString?.trim().orEmpty()
            if (direct.isNotBlank()) return direct
            val message = obj.get("message")?.asString?.trim().orEmpty()
            if (message.isNotBlank()) return message
        }
        raw.trim()
    } catch (_: Exception) {
        raw.trim()
    }
}

private interface SupabaseAdminApi {
    @Headers("Content-Type: application/json")
    @POST
    suspend fun createUser(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: CreateUserAdminRequest
    ): retrofit2.Response<ResponseBody>
}

private data class CreateUserAdminRequest(
    val firebaseIdToken: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String
)
