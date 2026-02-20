package com.example.eventglow.notifications

import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EventGlowMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        UserPreferences(application).updateFcmToken(token)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "EventGlow"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new update."
        val route = message.data["route"]
        val eventId = message.data["eventId"]
        LocalNotificationHelper.show(
            context = this,
            title = title,
            body = body,
            route = route,
            eventId = eventId
        )
    }
}
