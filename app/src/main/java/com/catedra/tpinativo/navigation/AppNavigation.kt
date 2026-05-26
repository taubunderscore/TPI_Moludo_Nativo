package com.catedra.tpinativo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.catedra.tpinativo.ui.screens.DetalleHabitoScreen
import com.catedra.tpinativo.ui.screens.HabitosScreen
import com.catedra.tpinativo.ui.screens.HabitosViewModel
import com.catedra.tpinativo.ui.screens.LoginScreen

// Creo un singleton para que no exista otra instancia igual
object Rutas {
    const val LOGIN = "login"
    const val LISTA = "lista"
    const val DETALLE = "detalle/{habitoId}"
    fun detalle(id: String) = "detalle/$id"
}

@Composable
fun AppNavigation(viewModel: HabitosViewModel, userId: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN
    ) {
        // Pantalla de Login
        composable(Rutas.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoginExitoso = {
                    // Cuando toca ingresar, viaja a la lista y BORRA el Login del historial
                    navController.navigate(Rutas.LISTA) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Lista de Hábitos
        composable(Rutas.LISTA) {
            HabitosScreen(
                viewModel = viewModel,
                userId = userId,
                onVerDetalle = { habitoId ->
                    navController.navigate(Rutas.detalle(habitoId))
                }
            )
        }

        // Pantalla de Detalle
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument("habitoId") { type = NavType.StringType }) // Le digo a navhost qué parámetros espera recibir esa ruta y de qué tipo son
        ) { backStackEntry ->
            val habitoId = backStackEntry.arguments?.getString("habitoId") ?: ""
            // Acá está la papa, es donde termino llamando al detalle, y le meto o configuro el botón volver
            DetalleHabitoScreen(
                habitoId = habitoId,
                viewModel = viewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}