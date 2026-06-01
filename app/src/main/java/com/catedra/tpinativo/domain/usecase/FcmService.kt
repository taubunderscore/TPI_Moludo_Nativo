package com.catedra.tpinativo.domain.usecase

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.catedra.tpinativo.MainActivity
import com.catedra.tpinativo.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showNotificacion(message)
    }

    private fun showNotificacion(message: RemoteMessage) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.notification_logo)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1, notification)

    }
}