package com.catedra.tpinativo.domain.usecase

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.catedra.tpinativo.R

class FcmService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showNotificacion(message)
    }

    private fun showNotificacion(message: RemoteMessage) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // 🚀 CORREGIDO: Ahora lee la constante local del companion object de esta misma clase
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.notification_logo)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    // 🎯 CONSTANTE LOCAL: Así el servicio no depende de que la MainActivity esté modificada o no
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "notificacion_fcm"
    }
}