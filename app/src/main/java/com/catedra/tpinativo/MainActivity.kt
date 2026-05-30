package com.catedra.tpinativo

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
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.viewmodel.HabitosViewModelFactory
import com.catedra.tpinativo.ui.theme.TPINativoTheme

class MainActivity : ComponentActivity() {

    // Declaramos la variable del ViewModel central
    private lateinit var habitosViewModel: HabitosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Mantiene el diseño moderno de borde a borde

        // 🚀 1. INSTANCIAMOS LA INFRAESTRUCTURA DE NUESTRA ARQUITECTURA LIMPIA
        val habitosRepo = HabitosRepository()
        val logrosRepo = LogrosRepository()

        // 🚀 2. ARMAMOS EL CASO DE USO (EL CEREBRO DE LA LÓGICA DE NEGOCIO)
        val gestionarProgresoUseCase = GestionarProgresoHabitoUseCase(habitosRepo, logrosRepo)

        // 🚀 3. INICIALIZAMOS EL VIEWMODEL PASÁNDOLE NUESTRO FACTORY FABRICADOR
        // Esto le enseña a Android a crear el ViewModel pasándole los parámetros requeridos
        habitosViewModel = ViewModelProvider(
            this,
            HabitosViewModelFactory(habitosRepo, gestionarProgresoUseCase)
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
}