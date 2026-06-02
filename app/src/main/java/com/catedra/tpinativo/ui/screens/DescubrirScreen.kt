package com.catedra.tpinativo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescubrirScreen(viewModel: HabitosViewModel, userId: String) {
    val plantillas by viewModel.plantillasCatalogo.collectAsState()
    val desafios by viewModel.desafiosCatalogo.collectAsState()
    var categoriaSeleccionada by remember { mutableStateOf("Físico") }
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.mensajeInspirador) {
        state.mensajeInspirador?.let { mensaje ->
            snackbarHostState.showSnackbar(message = mensaje, duration = SnackbarDuration.Short)
            viewModel.resetearMensajeInspirador()
        }
    }

    LaunchedEffect(categoriaSeleccionada) {
        if (categoriaSeleccionada == "🏆 Desafíos") {
            viewModel.cargarDesafios()
            // ✅ Cargamos todas las categorías acumuladas para resolver nombres en las cards
            viewModel.cargarTodasLasPlantillas()
        } else {
            viewModel.cargarCatalogoPorCategoria(categoriaSeleccionada)
        }
    }

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
            // ✅ Row con scroll horizontal — evita conflictos con LazyColumn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Físico", "Estudio", "Salud", "Productividad", "🏆 Desafíos").forEach { cat ->
                    FilterChip(
                        selected = categoriaSeleccionada == cat,
                        onClick = { categoriaSeleccionada = cat },
                        label = {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (categoriaSeleccionada == "🏆 Desafíos") {
                    items(desafios) { desafio ->
                        val (textoBoton, botonDeshabilitado) = state.obtenerEstadoDesafio(desafio.id)
                        val yaSeCumplioHoy = textoBoton == "¡Desafío Cumplido hoy! 🎉"

                        // ✅ Card expandible con hábitos asociados
                        DesafioCard(
                            desafio = desafio,
                            plantillas = plantillas,
                            textoBoton = textoBoton,
                            botonDeshabilitado = botonDeshabilitado,
                            yaSeCumplioHoy = yaSeCumplioHoy,
                            onSuscribirse = { viewModel.suscribirseADesafio(userId, desafio) }
                        )
                    }
                } else {
                    items(plantillas) { plantilla ->
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
                                Button(
                                    onClick = { viewModel.suscribirseAHabito(userId, plantilla) },
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

// ✅ Card de desafío expandible — muestra los hábitos asociados al tocar
@Composable
fun DesafioCard(
    desafio: DesafioObjetivo,
    plantillas: List<HabitoPlantilla>,
    textoBoton: String,
    botonDeshabilitado: Boolean,
    yaSeCumplioHoy: Boolean,
    onSuscribirse: () -> Unit
) {
    var expandida by remember { mutableStateOf(false) }

    // Resolvemos los nombres de los hábitos asociados desde las plantillas cargadas
    // Si no están cargadas mostramos el ID como fallback
    val habitosAsociados = desafio.habitosRequeridos.map { plantillaId ->
        plantillas.find { it.id == plantillaId }?.nombre ?: plantillaId
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // ── Cabecera siempre visible ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = desafio.nombreDesafio,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${desafio.habitosRequeridos.size} hábitos en combo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Botón expandir/contraer
                IconButton(onClick = { expandida = !expandida }) {
                    Icon(
                        imageVector = if (expandida) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expandida) "Contraer" else "Ver hábitos"
                    )
                }
            }

            // ── Detalle expandible ────────────────────────────────
            AnimatedVisibility(visible = expandida) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (desafio.descripcion.isNotEmpty()) {
                        Text(
                            text = desafio.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Hábitos incluidos:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lista de hábitos del desafío
                    habitosAsociados.forEach { nombreHabito ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "•  $nombreHabito",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "🏅 Insignia al completar todos en el mismo día",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón de suscripción
                    Button(
                        onClick = onSuscribirse,
                        enabled = !botonDeshabilitado || yaSeCumplioHoy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (yaSeCumplioHoy) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text(textoBoton)
                    }
                }
            }

            // Botón visible cuando está contraída y no está suscripto
            if (!expandida && !botonDeshabilitado) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSuscribirse,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(textoBoton)
                }
            }
        }
    }
}