package com.example.pagerproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "my_channel_id"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        // Create the notification channel when the service is created
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d("Notification Channel", "Creating notification channel")
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Notifications",
                NotificationManager.IMPORTANCE_HIGH // Set to high importance for sound
            ).apply {
                description = "Channel description"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(alarmSound, null) // Set the alarm sound
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM Message", "From: ${remoteMessage.from}")

        // Show notification
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }

        // Handle the message payload (data)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM Data", "Data Payload: ${remoteMessage.data}")
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.user) // Ensure you have this icon in your drawable resources
            .setContentTitle(title ?: "Default Title") // Default title if null
            .setContentText(message ?: "Default message") // Default message if null
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // For larger messages
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure high priority
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Make it visible on the lock screen

        // Build and display the notification
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

}

