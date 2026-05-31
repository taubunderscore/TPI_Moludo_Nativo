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
import com.catedra.tpinativo.ui.screens.RegisterScreen
import com.catedra.tpinativo.ui.screens.SplashScreen
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.google.firebase.auth.FirebaseAuth

// ── Rutas ─────────────────────────────────────────────────────────
object Rutas {
    const val SPLASH    = "splash"
    const val LOGIN     = "login"
    const val REGISTRO  = "registro"
    const val HOME      = "home"
    const val DESCUBRIR = "descubrir"
    const val LOGROS    = "logros"
    const val DETALLE   = "detalle/{habitoId}"
    fun detalle(id: String) = "detalle/$id"
}

data class ItemBarraNavegacion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

// Rutas que NO muestran la barra inferior
private val RUTAS_SIN_BARRA = setOf(Rutas.SPLASH, Rutas.LOGIN, Rutas.REGISTRO)

@Composable
fun AppNavigation(viewModel: HabitosViewModel, userId: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    val itemsNavegacion = listOf(
        ItemBarraNavegacion(Rutas.HOME,      "Home",      Icons.Default.Home),
        ItemBarraNavegacion(Rutas.DESCUBRIR, "Descubrir", Icons.Default.List),
        ItemBarraNavegacion(Rutas.LOGROS,    "Logros",    Icons.Default.Star)
    )

    Scaffold(
        bottomBar = {
            val mostrarBarra = rutaActual != null
                && rutaActual !in RUTAS_SIN_BARRA
                && !rutaActual.startsWith("detalle")

            if (mostrarBarra) {
                NavigationBar {
                    itemsNavegacion.forEach { item ->
                        NavigationBarItem(
                            icon    = { Icon(item.icono, contentDescription = item.titulo) },
                            label   = { Text(item.titulo) },
                            selected = rutaActual == item.ruta,
                            onClick = {
                                if (rutaActual != item.ruta) {
                                    navController.navigate(item.ruta) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
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
            navController    = navController,
            startDestination = Rutas.SPLASH,          // siempre arranca en el splash
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ── SPLASH ────────────────────────────────────────────
            composable(Rutas.SPLASH) {
                SplashScreen(
                    onSplashTerminado = {
                        val destino = if (FirebaseAuth.getInstance().currentUser != null)
                            Rutas.HOME else Rutas.LOGIN
                        navController.navigate(destino) {
                            popUpTo(Rutas.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // ── LOGIN ─────────────────────────────────────────────
            composable(Rutas.LOGIN) {
                LoginScreen(
                    viewModel      = viewModel,
                    onLoginExitoso = {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    },
                    onIrARegistro  = { navController.navigate(Rutas.REGISTRO) }
                )
            }

            // ── REGISTRO ──────────────────────────────────────────
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

            // ── HOME ──────────────────────────────────────────────
            composable(Rutas.HOME) {
                HabitosScreen(
                    viewModel  = viewModel,
                    userId     = userId,
                    onVerDetalle = { habitoId ->
                        navController.navigate(Rutas.detalle(habitoId))
                    }
                )
            }

            // ── DESCUBRIR ─────────────────────────────────────────
            composable(Rutas.DESCUBRIR) {
                DescubrirScreen(viewModel = viewModel, userId = userId)
            }

            // ── LOGROS ────────────────────────────────────────────
            composable(Rutas.LOGROS) {
                LogrosScreen(viewModel = viewModel, userId = userId)
            }

            // ── DETALLE ───────────────────────────────────────────
            composable(
                route     = Rutas.DETALLE,
                arguments = listOf(navArgument("habitoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val habitoId = backStackEntry.arguments?.getString("habitoId") ?: ""
                DetalleHabitoScreen(
                    habitoId  = habitoId,
                    viewModel = viewModel,
                    onVolver  = { navController.popBackStack() }
                )
            }
        }
    }
}
