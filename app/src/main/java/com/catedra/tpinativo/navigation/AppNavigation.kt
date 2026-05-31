package com.catedra.tpinativo.navigation

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.catedra.tpinativo.ui.screens.DescubrirScreen
import com.catedra.tpinativo.ui.screens.DetalleHabitoScreen
import com.catedra.tpinativo.ui.screens.HabitosScreen
import com.catedra.tpinativo.ui.screens.LoginScreen
import com.catedra.tpinativo.ui.screens.LogrosScreen
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.google.firebase.auth.FirebaseAuth
import com.catedra.tpinativo.ui.screens.RegisterScreen

// 📌 Definición de rutas fijas
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"       // Tu pantalla actual de tildar hábitos
    const val DESCUBRIR = "descubrir" // El catálogo por categorías
    const val LOGROS = "logros"     // Tus medallas ganadas
    const val DETALLE = "detalle/{habitoId}"
    fun detalle(id: String) = "detalle/$id"
}

//  Estructura para los botones de la barra inferior
data class ItemBarraNavegacion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

@Composable
fun AppNavigation(viewModel: HabitosViewModel, userId: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    val usuarioLogueado = FirebaseAuth.getInstance().currentUser != null
    val destinoInicial = if (usuarioLogueado) Rutas.HOME else Rutas.LOGIN

    // Lista de las 3 pestañas principales de la barra de abajo
    val itemsNavegacion = listOf(
        ItemBarraNavegacion(Rutas.HOME, "Home", Icons.Default.Home),
        ItemBarraNavegacion(Rutas.DESCUBRIR, "Descubrir", Icons.Default.List),
        ItemBarraNavegacion(Rutas.LOGROS, "Logros", Icons.Default.Star)
    )

    Scaffold(
        bottomBar = {
            // 🚨 SÓLO MOSTRAMOS LA BARRA SI EL USUARIO NO ESTÁ EN EL LOGIN
            if (rutaActual != Rutas.LOGIN && rutaActual != Rutas.REGISTRO && rutaActual?.startsWith(
                    "detalle"
                ) == false
            ) {
                NavigationBar {
                    itemsNavegacion.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icono, contentDescription = item.titulo) },
                            label = { Text(item.titulo) },
                            selected = rutaActual == item.ruta,
                            onClick = {
                                if (rutaActual != item.ruta) {
                                    navController.navigate(item.ruta) {
                                        // Evita acumular pantallas repetidas en el historial
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = destinoInicial,
            modifier = Modifier.padding(innerPadding) // Aplica el espacio de la barra inferior automáticamente
        ) {
            // 1. PANTALLA: LOGIN
            composable(Rutas.LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginExitoso = {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    },
                    onIrARegistro = {
                        navController.navigate(Rutas.REGISTRO)
                    }
                )
            }

            composable(Rutas.REGISTRO) {
                RegisterScreen(
                    onRegistroExitoso = {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    },
                    onVolver = { navController.popBackStack() }
                )
            }

            // 2. PANTALLA: HOME (Tu lista actual con el buscador, filtrada por ti)
            composable(Rutas.HOME) {
                HabitosScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onVerDetalle = { habitoId ->
                        navController.navigate(Rutas.detalle(habitoId))
                    }
                )
            }

            // 3. PANTALLA: DESCUBRIR (Catálogo por categorías)
            composable(Rutas.DESCUBRIR) {
                DescubrirScreen(viewModel = viewModel, userId = userId)
            }

            // 4. PANTALLA: LOGROS (Lista de medallas ganadas)
            composable(Rutas.LOGROS) {
                // TODO: Crearemos LogrosScreen.kt
                Text("Pantalla de Logros 🏆 (Próximo paso)", modifier = Modifier.padding(16.dp))
            }

            // 5. PANTALLA: DETALLE
            composable(
                route = Rutas.DETALLE,
                arguments = listOf(navArgument("habitoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val habitoId = backStackEntry.arguments?.getString("habitoId") ?: ""
                DetalleHabitoScreen(
                    habitoId = habitoId,
                    viewModel = viewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
            // 4. PANTALLA: LOGROS (Lista de medallas ganadas) - ¡CONECTADO CON ÉXITO!
            composable(Rutas.LOGROS) {
                LogrosScreen(
                    viewModel = viewModel,
                    userId = userId
                )
            }
        }
    }
}