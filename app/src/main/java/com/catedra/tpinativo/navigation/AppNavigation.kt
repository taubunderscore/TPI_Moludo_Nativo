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

object Rutas {
    const val LISTA = "lista"
    const val DETALLE = "detalle/{habitoId}"
    fun detalle(id: String) = "detalle/$id"
}

@Composable
fun AppNavigation(viewModel: HabitosViewModel, userId: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Rutas.LISTA) {
        composable(Rutas.LISTA) {
            HabitosScreen(
                viewModel = viewModel,
                userId = userId,
                onVerDetalle = { habitoId ->
                    navController.navigate(Rutas.detalle(habitoId))
                }
            )
        }

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
    }
}