package com.catedra.tpinativo.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.catedra.tpinativo.R

class HabitoReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // ✅ Leemos los datos que le pasamos al programar
        val nombreHabito = inputData.getString(KEY_NOMBRE_HABITO) ?: "tu hábito"
        val habitoId = inputData.getString(KEY_HABITO_ID) ?: return Result.failure()

        mostrarNotificacion(nombreHabito, habitoId)
        return Result.success()
    }

    private fun mostrarNotificacion(nombreHabito: String, habitoId: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("¡Recordatorio de hábito! 💪")
            .setContentText("No olvides: $nombreHabito")
            .setSmallIcon(R.drawable.notification_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Usamos hashCode del habitoId para ID único por hábito
        notificationManager.notify(habitoId.hashCode(), notification)
    }

    companion object {
        const val KEY_NOMBRE_HABITO = "nombre_habito"
        const val KEY_HABITO_ID = "habito_id"
        const val CHANNEL_ID = "notificacion_fcm" // mismo canal
    }
}