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
import com.catedra.tpinativo.data.repository.DesafiosRepository // 🚀 IMPORTAMOS EL NUEVO REPO
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirHabitoUseCase
import com.catedra.tpinativo.domain.usecase.ObtenerDesafiosCatalogoUseCase // 🚀 IMPORTAMOS EL CASO DE USO 1
import com.catedra.tpinativo.domain.usecase.SuscribirseADesafioUseCase // 🚀 IMPORTAMOS EL CASO DE USO 2
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.viewmodel.HabitosViewModelFactory
import com.catedra.tpinativo.ui.theme.TPINativoTheme

class MainActivity : ComponentActivity() {

    // Declaramos la variable del ViewModel central
    private lateinit var habitosViewModel: HabitosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Mantiene el diseño moderno de borde a borde

        // 1. Instanciamos los Repositorios de la capa de datos
        val habitosRepo = HabitosRepository()
        val logrosRepo = LogrosRepository()
        val desafiosRepo = DesafiosRepository() // 🚀 ¡Faltaba esta línea clave!

        // 2. Instanciamos los Casos de Uso (Capa de Dominio) pasando sus dependencias
        val gestionarProgresoUseCase = GestionarProgresoHabitoUseCase(habitosRepo, logrosRepo)
        val suscribirUseCase = SuscribirHabitoUseCase(habitosRepo)
        val obtenerDesafiosUseCase = ObtenerDesafiosCatalogoUseCase(desafiosRepo)
        val suscribirseADesafioUseCase = SuscribirseADesafioUseCase(habitosRepo) // 🚀 Con un solo parámetro como tu constructor real

        // 3. Fábrica inyectando absolutamente todo de forma limpia
        habitosViewModel = ViewModelProvider(
            this,
            HabitosViewModelFactory(
                habitosRepository = habitosRepo,
                gestionarProgresoHabitoUseCase = gestionarProgresoUseCase,
                suscribirHabitoUseCase = suscribirUseCase,
                obtenerDesafiosCatalogoUseCase = obtenerDesafiosUseCase, // 🚀 Reemplazamos el TODO()
                suscribirseADesafioUseCase = suscribirseADesafioUseCase // 🚀 Reemplazamos el TODO()
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
}