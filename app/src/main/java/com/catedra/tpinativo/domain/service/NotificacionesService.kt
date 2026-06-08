package com.catedra.tpinativo.domain.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.catedra.tpinativo.R
import com.catedra.tpinativo.data.repository.HabitosPersonalizadosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
//  BroadcastReceiver: muestra la notificación y reprograma el día siguiente
//  (setExact no es repetitivo — hay que reencolar manualmente cada disparo)
// ─────────────────────────────────────────────────────────────────────────────

class RecordatorioHabitoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nombre   = intent.getStringExtra(EXTRA_NOMBRE)           ?: "Hábito"
        val detalle  = intent.getStringExtra(EXTRA_DETALLE)          ?: "Es hora de cumplir tu hábito 💪"
        val hora     = intent.getStringExtra(EXTRA_HORA)             ?: return
        val habitoId = intent.getStringExtra(EXTRA_HABITO_ID)        ?: return
        val notifId  = intent.getIntExtra(EXTRA_ID, habitoId.hashCode())

        crearCanalSiNoExiste(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_logo)
            .setContentTitle("⏰ $nombre")
            .setContentText(detalle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)

        // Reprogramar para mañana a la misma hora (setExact no es periódico)
        NotificacionesService.programar(
            context          = context,
            habitoId         = habitoId,
            nombre           = nombre,
            detalle          = detalle,
            horaRecordatorio = hora
        )
    }

    private fun crearCanalSiNoExiste(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val canal = NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de hábitos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones diarias para tus hábitos personalizados"
                }
                manager.createNotificationChannel(canal)
            }
        }
    }

    companion object {
        const val CHANNEL_ID      = "recordatorio_habitos_personalizados"
        const val EXTRA_NOMBRE    = "extra_nombre"
        const val EXTRA_DETALLE   = "extra_detalle"
        const val EXTRA_HORA      = "extra_hora"
        const val EXTRA_HABITO_ID = "extra_habito_id"
        const val EXTRA_ID        = "extra_id"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BroadcastReceiver: reprograma TODAS las alarmas tras reinicio del dispositivo
//  (AlarmManager pierde todas las alarmas al apagar el teléfono)
// ─────────────────────────────────────────────────────────────────────────────

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "Boot detectado — reprogramando alarmas de hábitos personalizados")

        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Log.w("BootReceiver", "Usuario no logueado, no se reprograman alarmas")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo    = HabitosPersonalizadosRepository()
                val habitos = repo.obtenerHabitosDeUsuario(userId)
                habitos.forEach { habito ->
                    NotificacionesService.programar(
                        context          = context,
                        habitoId         = habito.id,
                        nombre           = habito.nombre,
                        detalle          = habito.detalle.ifBlank { "Es hora de cumplir tu hábito 💪" },
                        horaRecordatorio = habito.horaRecordatorio
                    )
                }
                Log.d("BootReceiver", "${habitos.size} alarmas reprogramadas")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error reprogramando alarmas: ${e.localizedMessage}")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Servicio de scheduling
// ─────────────────────────────────────────────────────────────────────────────

object NotificacionesService {

    private const val TAG = "NotificacionesService"

    /**
     * Programa una alarma exacta para el próximo disparo del hábito.
     * Usa setExactAndAllowWhileIdle para garantizar el disparo incluso en Doze mode.
     * El receiver se encarga de reencolar el día siguiente.
     */
    fun programar(
        context: Context,
        habitoId: String,
        nombre: String,
        detalle: String,
        horaRecordatorio: String
    ) {
        val partes = horaRecordatorio.split(":")
        if (partes.size != 2) {
            Log.w(TAG, "Hora inválida: $horaRecordatorio")
            return
        }
        val hora    = partes[0].toIntOrNull() ?: return
        val minutos = partes[1].toIntOrNull() ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // En Android 12+ verificar permiso de alarmas exactas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Sin permiso SCHEDULE_EXACT_ALARM para habitoId=$habitoId")
                // Redirigir al usuario a la configuración si se desea:
                // Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                return
            }
        }

        val pendingIntent = buildPendingIntent(context, habitoId, nombre, detalle, horaRecordatorio)

        val calendario = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minutos)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Si la hora de hoy ya pasó, programar para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            // setExactAndAllowWhileIdle: funciona incluso en Doze mode (Android 6+)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Alarma exacta programada: $nombre a las $horaRecordatorio — disparo: ${calendario.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException al programar alarma: ${e.localizedMessage}")
        }
    }

    /**
     * Cancela la alarma asociada a [habitoId].
     */
    fun cancelar(context: Context, habitoId: String) {
        val alarmManager  = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, habitoId, "", "", "")
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarma cancelada: habitoId=$habitoId")
    }

    private fun buildPendingIntent(
        context: Context,
        habitoId: String,
        nombre: String,
        detalle: String,
        hora: String
    ): PendingIntent {
        val intent = Intent(context, RecordatorioHabitoReceiver::class.java).apply {
            putExtra(RecordatorioHabitoReceiver.EXTRA_NOMBRE,    nombre)
            putExtra(RecordatorioHabitoReceiver.EXTRA_DETALLE,   detalle)
            putExtra(RecordatorioHabitoReceiver.EXTRA_HORA,      hora)
            putExtra(RecordatorioHabitoReceiver.EXTRA_HABITO_ID, habitoId)
            putExtra(RecordatorioHabitoReceiver.EXTRA_ID,        habitoId.hashCode())
        }
        return PendingIntent.getBroadcast(
            context,
            habitoId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}