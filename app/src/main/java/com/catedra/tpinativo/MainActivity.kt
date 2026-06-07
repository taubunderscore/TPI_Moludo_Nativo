package com.catedra.tpinativo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.catedra.tpinativo.data.repository.NotificacionRepository
import com.catedra.tpinativo.domain.usecase.ProgramarRecordatorioUseCase
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
import com.catedra.tpinativo.data.repository.DesafiosRepository // 🚀 NUEVO IMPORT
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.data.repository.LogrosRepository
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import com.catedra.tpinativo.domain.usecase.ObtenerDesafiosCatalogoUseCase // 🚀 NUEVO IMPORT
import com.catedra.tpinativo.domain.usecase.SuscribirHabitoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirseADesafioUseCase // 🚀 NUEVO IMPORT
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.viewmodel.HabitosViewModelFactory
import com.catedra.tpinativo.ui.theme.TPINativoTheme

class MainActivity : ComponentActivity() {

    // Declaramos la variable del ViewModel central
    private lateinit var habitosViewModel: HabitosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crearCanalNotificaciones()  // notificaicones
        enableEdgeToEdge() // Mantiene el diseño moderno de borde a borde

        // 1. Repositorios
        val habitosRepo = HabitosRepository()
        val logrosRepo = LogrosRepository()
        val desafiosRepo = DesafiosRepository() // 🚀 Instanciamos el repo de desafíos
        val notificacionRepo = NotificacionRepository(this) // ← nuevo, necesita Context

        // 2. Casos de Uso (Capa de Dominio)
        val programarRecordatorioUseCase = ProgramarRecordatorioUseCase(notificacionRepo) // ← notificacion
        val gestionarProgresoUseCase = GestionarProgresoHabitoUseCase(habitosRepo, logrosRepo)
        val suscribirUseCase = SuscribirHabitoUseCase(habitosRepo, programarRecordatorioUseCase) //
        val obtenerDesafiosUseCase = ObtenerDesafiosCatalogoUseCase(desafiosRepo) // Nuevo caso de uso
        val suscribirseADesafioUseCase = SuscribirseADesafioUseCase(habitosRepo) //Nuevo caso de uso combo
        // 3. Crear canal de notificaciones — AGREGAR ESTE MÉTODO en MainActivity

        // 3. Fábrica inyectando TODO el ecosistema completo
        habitosViewModel = ViewModelProvider(
            this,
            HabitosViewModelFactory(
                habitosRepo,
                gestionarProgresoUseCase,
                suscribirUseCase,
                obtenerDesafiosUseCase,      // 🚀 Pasamos el 4to parámetro
                suscribirseADesafioUseCase,   // 🚀 Pasamos el 5to parámetro
                notificacionRepo
            )
        )[HabitosViewModel::class.java]

        setContent {
            TPINativoTheme {
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
    // 3. Crear canal de notificaciones — AGREGAR ESTE MÉTODO en MainActivity
    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "notificacion_fcm",
                "Recordatorios de Hábitos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios diarios de hábitos pendientes"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }
}