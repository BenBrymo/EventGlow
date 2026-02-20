package com.example.eventglow.notifications

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object FirestoreNotificationListener {

    fun start(
        role: String,
        onNewNotification: (FirestoreNotification) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        val firestore = FirebaseFirestore.getInstance()
        var isFirstSnapshot = true

        return firestore.collection("notifications")
            .whereIn("targetRole", listOf("all", role))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val nonNullSnapshot = snapshot ?: return@addSnapshotListener
                if (isFirstSnapshot) {
                    isFirstSnapshot = false
                    return@addSnapshotListener
                }

                nonNullSnapshot.documentChanges
                    .filter { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val doc = change.document
                        val notification = FirestoreNotification(
                            title = doc.getString("title").orEmpty(),
                            body = doc.getString("body").orEmpty(),
                            route = doc.getString("route").orEmpty().ifBlank { "detailed_event_screen" },
                            eventId = doc.getString("eventId"),
                            targetRole = doc.getString("targetRole").orEmpty().ifBlank { "all" }
                        )
                        if (notification.title.isBlank() || notification.body.isBlank()) {
                            Log.d("FirestoreNotification", "Skipping invalid notification document ${doc.id}")
                            return@forEach
                        }
                        onNewNotification(notification)
                    }
            }
    }
}

