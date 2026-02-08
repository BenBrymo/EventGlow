package com.example.eventglow.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class NotificationViewModel : ViewModel() {

    private val oneSignalApi: OneSignalApi = Retrofit.Builder()
        .baseUrl("https://onesignal.com/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OneSignalApi::class.java)

    suspend fun sendNewEventNotification() {
        val notificationPayload = NotificationPayload(
            app_id = "4c98b5cc-62e9-4d6f-bf5a-28b59c259f60",
            filters = listOf(
                Filter(field = "tag", key = "role", relation = "=", value = "user")
            ),
            headings = mapOf("en" to "New Event Just Dropped"),
            contents = mapOf("en" to "Check out the latest event!")
        )

        try {
            val response = oneSignalApi.sendNotification(notificationPayload)
            if (response.isSuccessful) {
                Log.d("NotificationViewModel", "Notification sent successfully")
            } else {
                Log.e("NotificationViewModel", "Failed to send notification: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Error sending notification: ${e.message}")
        }
    }

}

data class NotificationPayload(
    val app_id: String,
    val filters: List<Filter>,
    val headings: Map<String, String>,
    val contents: Map<String, String>
)

data class Filter(
    val field: String,
    val key: String,
    val relation: String,
    val value: String
)

interface OneSignalApi {
    @POST("notifications")
    suspend fun sendNotification(@Body notification: NotificationPayload): Response<ResponseBody>
}