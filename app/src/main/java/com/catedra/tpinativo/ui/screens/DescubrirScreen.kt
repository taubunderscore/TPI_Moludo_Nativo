package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescubrirScreen(viewModel: HabitosViewModel, userId: String) {
    // 1. Escuchamos tanto las plantillas normales como los desafíos combinados
    val plantillas by viewModel.plantillasCatalogo.collectAsState()
    val desafios by viewModel.desafiosCatalogo.collectAsState()

    var categoriaSeleccionada by remember { mutableStateOf("Físico") }
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 🚀 EFECTO TEMPORAL: Escucha cuando cambia el mensaje inspirador
    LaunchedEffect(state.mensajeInspirador) {
        state.mensajeInspirador?.let { mensaje ->
            snackbarHostState.showSnackbar(
                message = mensaje,
                duration = SnackbarDuration.Short
            )
            viewModel.resetearMensajeInspirador()
        }
    }

    // 2. Cargador inteligente de categorías
    LaunchedEffect(categoriaSeleccionada) {
        if (categoriaSeleccionada == "🏆 Desafíos") {
            viewModel.cargarDesafios()
        } else {
            viewModel.cargarCatalogoPorCategoria(categoriaSeleccionada)
        }
    }

    // 🎯 SCAFFOLD ÚNICO E INTEGRADO: Une barra superior, snackbar y contenedor
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (categoriaSeleccionada == "🏆 Desafíos") "Desafíos Especiales" else "Catálogo de Hábitos")
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // 🔘 FILTROS DE CATEGORÍAS (Chips modernos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Físico", "Estudio", "Salud", "🏆 Desafíos").forEach { cat ->
                    val seleccionada = categoriaSeleccionada == cat

                    FilterChip(
                        selected = seleccionada,
                        onClick = { categoriaSeleccionada = cat },
                        label = {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 📜 LISTA DINÁMICA MIXTA
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (categoriaSeleccionada == "🏆 Desafíos") {
                    items(desafios) { desafio ->
                        // 🔍 Buscamos el estado dinámico del botón según el UiState
                        val (textoBoton, botonDeshabilitado) = state.obtenerEstadoDesafio(desafio.id)
                        val yaSeCumplioHoy = textoBoton == "¡Desafío Cumplido hoy! 🎉"

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(desafio.nombreDesafio, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${desafio.habitosRequeridos.size} hábitos en combo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // 🚀 El botón inteligente de desafíos
                                    Button(
                                        onClick = { viewModel.suscribirseADesafio(userId, desafio) },
                                        // Habilitado si no está grisado O si ya se cumplió hoy para poder reactivarlo
                                        enabled = !botonDeshabilitado || yaSeCumplioHoy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (yaSeCumplioHoy) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = Color.LightGray,
                                            disabledContentColor = Color.DarkGray
                                        )
                                    ) {
                                        Text(textoBoton)
                                    }
                                }

                                if (desafio.descripcion.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = desafio.descripcion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(plantillas) { plantilla ->
                        // 🔍 Buscamos el estado para hábitos individuales
                        val (textoBoton, botonDeshabilitado) = state.obtenerEstadoHabitoIndividual(plantilla.id)
                        val yaSeCompletoHoy = textoBoton == "¡Completado! 💪"

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(plantilla.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("Frecuencia: ${plantilla.frecuencia}", style = MaterialTheme.typography.bodySmall)
                                }

                                // 🚀 El botón inteligente de hábitos sueltos
                                Button(
                                    onClick = { viewModel.suscribirseAHabito(userId, plantilla) },
                                    // Habilitado si no está grisado O si ya está en estado completado
                                    enabled = !botonDeshabilitado || yaSeCompletoHoy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (yaSeCompletoHoy) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = Color.LightGray,
                                        disabledContentColor = Color.DarkGray
                                    )
                                ) {
                                    Text(textoBoton)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}