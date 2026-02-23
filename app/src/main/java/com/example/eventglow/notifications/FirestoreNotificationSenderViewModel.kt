package com.example.eventglow.notifications

import android.app.Application
import com.example.eventglow.BuildConfig
import com.example.eventglow.common.BaseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

class FirestoreNotificationSenderViewModel(application: Application) : BaseViewModel(application) {

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val pushApi: SupabaseFunctionsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://placeholder.functions.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseFunctionsApi::class.java)
    }

    fun sendNotificationToRole(
        title: String,
        body: String,
        targetRole: String,
        route: String = "detailed_event_screen",
        eventId: String? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (title.isBlank() || body.isBlank()) {
            val message = "Title and body are required."
            setFailure(message)
            onResult(false, message)
            return
        }
        if (targetRole.isBlank()) {
            val message = "Target role is required."
            setFailure(message)
            onResult(false, message)
            return
        }

        val normalizedTitle = title.trim()
        val normalizedBody = body.trim()
        val normalizedRoute = route.trim()
        val normalizedRole = targetRole.trim().lowercase()
        val normalizedEventId = eventId?.trim().orEmpty()

        if (normalizedTitle.length > 120) {
            val message = "Notification title is too long (max 120 chars)."
            setFailure(message)
            onResult(false, message)
            return
        }
        if (normalizedBody.length > 400) {
            val message = "Notification body is too long (max 400 chars)."
            setFailure(message)
            onResult(false, message)
            return
        }
        if (normalizedRoute !in setOf("detailed_event_screen", "detailed_event_screen_admin")) {
            val message = "Unsupported route."
            setFailure(message)
            onResult(false, message)
            return
        }

        val functionsBaseUrl = BuildConfig.SUPABASE_FUNCTIONS_BASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_FUNCTIONS_ANON_KEY.trim()
        val pushPath = BuildConfig.SUPABASE_FUNCTIONS_PUSH_PATH.trim().trimStart('/')
        if (functionsBaseUrl.isBlank() || anonKey.isBlank() || pushPath.isBlank()) {
            val message =
                "Push sender config missing. Set SUPABASE_FUNCTIONS_BASE_URL, SUPABASE_FUNCTIONS_ANON_KEY and SUPABASE_FUNCTIONS_PUSH_PATH in BuildConfig."
            setFailure(message)
            onResult(false, message)
            return
        }

        _isSending.value = true
        clearError()
        launchWhenConnected(tag = "FirestoreNotificationSenderViewModel", onError = { throwable ->
            _isSending.value = false
            val message = throwable.message ?: "Failed to send push notification."
            onResult(false, message)
        }) {
            enforceAdminSender()
            enforcePushCooldown()

            if (normalizedRole !in setOf("all", "admin", "user")) {
                _isSending.value = false
                val message = "Unsupported target role. Use all, user, or admin."
                setFailure(message)
                onResult(false, message)
                return@launchWhenConnected
            }

            val payload = SupabasePushRequest(
                title = normalizedTitle,
                body = normalizedBody,
                targetRole = normalizedRole,
                route = normalizedRoute,
                eventId = normalizedEventId
            )

            val response = withContext(Dispatchers.IO) {
                pushApi.send(
                    url = "$functionsBaseUrl/$pushPath",
                    apiKey = anonKey,
                    authorization = "Bearer $anonKey",
                    body = payload
                )
            }
            if (!response.isSuccessful) {
                val errorText = response.errorBody()?.string().orEmpty()
                val parsedError = parseBackendError(errorText)
                throw IllegalStateException("Push send failed [HTTP ${response.code()}]: $parsedError")
            }

            _isSending.value = false
            setSuccess()
            onResult(true, null)
        }
    }

    private suspend fun enforceAdminSender() {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in.")
        val snapshot = firestore.collection("users").document(userId).get().await()
        val role = snapshot.getString("role")?.trim()?.lowercase().orEmpty()
        if (role != "admin") throw IllegalStateException("Only admins can send notifications.")
    }

    private suspend fun enforcePushCooldown() {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in.")
        val limiterRef = firestore.collection("rate_limits").document(userId)
        val cooldownMs = 20_000L

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(limiterRef)
            val lastPushAtMs = snapshot.getLong("lastPushSendAtMs") ?: 0L
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastPushAtMs < cooldownMs) {
                val secondsLeft = ((cooldownMs - (nowMs - lastPushAtMs)) / 1000L).coerceAtLeast(1L)
                throw IllegalStateException("Please wait ${secondsLeft}s before sending another push.")
            }
            transaction.set(
                limiterRef,
                mapOf(
                    "lastPushSendAtMs" to nowMs,
                    "updatedAtMs" to nowMs
                )
            )
            null
        }.await()
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

private interface SupabaseFunctionsApi {
    @Headers("Content-Type: application/json")
    @POST
    suspend fun send(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: SupabasePushRequest
    ): retrofit2.Response<ResponseBody>
}

private data class SupabasePushRequest(
    val title: String,
    val body: String,
    val targetRole: String,
    val route: String,
    val eventId: String
)
