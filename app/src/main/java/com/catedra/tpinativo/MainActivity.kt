package com.catedra.tpinativo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.catedra.tpinativo.navigation.AppNavigation
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.data.repository.LogrosRepository
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirHabitoUseCase
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.viewmodel.HabitosViewModelFactory
import com.catedra.tpinativo.ui.theme.TPINativoTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

class MainActivity : ComponentActivity() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "notificacion_fcm"
    }

    // Declaramos la variable del ViewModel central
    private lateinit var habitosViewModel: HabitosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Mantiene el diseño moderno de borde a borde

        Firebase.messaging.token.addOnCompleteListener {
            if(!it.isSuccessful){
                return@addOnCompleteListener
            }
            val token = it.result
        }

        createNotificacionChannel()

        // 1. Repositorios
        val habitosRepo = HabitosRepository()
        val logrosRepo = LogrosRepository()

// 2. Casos de Uso (Capa de Dominio)
        val gestionarProgresoUseCase = GestionarProgresoHabitoUseCase(habitosRepo, logrosRepo)
        val suscribirUseCase = SuscribirHabitoUseCase(habitosRepo) // 🚀 El nuevo

// 3. Fábrica inyectando todo
        habitosViewModel = ViewModelProvider(
            this,
            HabitosViewModelFactory(
                habitosRepo,
                gestionarProgresoUseCase,
                suscribirUseCase // 🚀 Pasamos el nuevo caso de uso
            )
        )[HabitosViewModel::class.java]
        setContent {
            TPINativoTheme {
                // Surface es tu contenedor visual principal
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Encendemos nuestro enrutador central pasándole el ViewModel ya construido con éxito
                    AppNavigation(
                        viewModel = habitosViewModel,
                        userId = "user_varela_123" // ID simulado para filtrar en Firestore
                    )
                }
            }
        }
    }

    private fun createNotificacionChannel(){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Notificaciones Habit Flow",
            NotificationManager.IMPORTANCE_HIGH
            )
        channel.description = "Estas notificaciones son provenientes de FCM"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}