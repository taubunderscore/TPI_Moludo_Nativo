package com.catedra.tpinativo.data.repository

import android.content.Context
import androidx.work.*
import com.catedra.tpinativo.workers.HabitoReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificacionRepository(private val context: Context) {

    // ✅ Programa una notificación diaria a la hora indicada
    fun programarRecordatorio(habitoId: String, nombreHabito: String, horaRecordatorio: String) {
        val partes = horaRecordatorio.split(":")
        if (partes.size != 2) return

        val hora = partes[0].toIntOrNull() ?: return
        val minuto = partes[1].toIntOrNull() ?: return

        // Calculamos cuántos milisegundos faltan para la próxima vez que sea esa hora
        val ahora = Calendar.getInstance()
        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            // Si ya pasó la hora hoy, programamos para mañana
            if (before(ahora)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val demora = objetivo.timeInMillis - ahora.timeInMillis

        // Datos que le pasamos al Worker
        val inputData = workDataOf(
            HabitoReminderWorker.KEY_HABITO_ID    to habitoId,
            HabitoReminderWorker.KEY_NOMBRE_HABITO to nombreHabito
        )

        // ✅ PeriodicWorkRequest — se repite cada 24 horas
        val workRequest = PeriodicWorkRequestBuilder<HabitoReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(demora, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        // Tag único por hábito — permite cancelarlo después
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "recordatorio_$habitoId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        android.util.Log.d("NotificacionRepo",
            "Recordatorio programado para $nombreHabito a las $horaRecordatorio")
    }

    // ✅ Cancela el recordatorio cuando el usuario se da de baja
    fun cancelarRecordatorio(habitoId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("recordatorio_$habitoId")

        android.util.Log.d("NotificacionRepo", "Recordatorio cancelado para habitoId=$habitoId")
    }
}