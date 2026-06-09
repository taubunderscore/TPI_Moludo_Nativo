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
import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.Habito
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescubrirScreen(viewModel: HabitosViewModel, userId: String) {
    val catalogoHabitos  by viewModel.catalogoHabitos.collectAsState()
    val desafios         by viewModel.desafiosCatalogo.collectAsState()
    val uiState          by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var categoriaSeleccionada by remember { mutableStateOf("🏆 Desafíos") }

    LaunchedEffect(uiState.mensajeInspirador) {
        uiState.mensajeInspirador?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.resetearMensajeInspirador()
        }
    }
    LaunchedEffect(userId) {
        viewModel.cargarHabitos(userId)
    }
    // ✅ Este reacciona cuando desafiosSuscritos cambia
    LaunchedEffect(uiState.desafiosSuscritos) {
        android.util.Log.d("DESCUBRIR", "desafiosSuscritos actualizado: ${uiState.desafiosSuscritos.size}")
    }
    LaunchedEffect(categoriaSeleccionada) {
        if (categoriaSeleccionada == "🏆 Desafíos") {
            viewModel.cargarDesafios()
            viewModel.cargarTodosLosHabitosCatalogo()
        } else {
            viewModel.cargarCatalogoPorCategoria(categoriaSeleccionada)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (categoriaSeleccionada == "🏆 Desafíos")
                            "Desafíos Especiales" else "Catálogo de Hábitos"
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("🏆 Desafíos", "Estudio", "Salud", "Productividad", "Físico").forEach { cat ->
                    FilterChip(
                        selected = categoriaSeleccionada == cat,
                        onClick  = { categoriaSeleccionada = cat },
                        label    = { Text(cat, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            LazyColumn(
                modifier              = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp)
            ) {
                if (categoriaSeleccionada == "🏆 Desafíos") {
                    items(desafios) { desafio ->
                        val (textoBoton, botonDeshabilitado) = uiState.obtenerEstadoDesafio(desafio.id)
                        val yaSeCumplioHoy = textoBoton == "¡Cumplido hoy! 🎉"

                        DesafioCard(
                            desafio             = desafio,
                            catalogoHabitos     = catalogoHabitos,
                            textoBoton          = textoBoton,
                            botonDeshabilitado  = botonDeshabilitado,
                            yaSeCumplioHoy      = yaSeCumplioHoy,
                            onSuscribirse       = { viewModel.suscribirseADesafio(userId, desafio) },
                            onDarDeBaja         = {
                                val hijosIds = uiState.habitos
                                    .filter { it.desafioId == desafio.id }
                                    .map { it.id }
                                viewModel.darDeBajaDesafioCompleto(userId, desafio.id, hijosIds)
                            }
                        )
                    }
                } else {
                    items(catalogoHabitos) { habito ->
                        val (textoBoton, botonDeshabilitado) = uiState.obtenerEstadoHabitoIndividual(habito.id)
                        val yaSeCompletoHoy = textoBoton == "¡Completado! 💪"

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier              = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(habito.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text  = "${habito.categoria} · ${habito.frecuencia}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick  = { viewModel.suscribirseAHabito(userId, habito) },
                                    enabled  = !botonDeshabilitado,
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor        = if (yaSeCompletoHoy) Color(0xFF4CAF50)
                                        else MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = Color.LightGray,
                                        disabledContentColor   = Color.DarkGray
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

@Composable
fun DesafioCard(
    desafio: Desafio,
    catalogoHabitos: List<Habito>,
    textoBoton: String,
    botonDeshabilitado: Boolean,
    yaSeCumplioHoy: Boolean,
    onSuscribirse: () -> Unit,
    onDarDeBaja: () -> Unit
) {
    var expandida by remember { mutableStateOf(false) }
    var mostrarDialogoBaja by remember { mutableStateOf(false) }

    val yaSuscripto = botonDeshabilitado || yaSeCumplioHoy

    if (mostrarDialogoBaja) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBaja = false },
            title            = { Text("⚠️ Dar de baja el desafío") },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se eliminarán de tu lista:")
                    desafio.habitosIds.forEach { habitoId ->
                        val nombre = catalogoHabitos.find { it.id == habitoId }?.nombre ?: habitoId
                        Text("  •  $nombre")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoBaja = false; onDarDeBaja() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Dar de baja") }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogoBaja = false }) { Text("Cancelar") }
            }
        )
    }

    val habitosNombres = desafio.habitosIds.map { id ->
        catalogoHabitos.find { it.id == id }?.nombre ?: id
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(desafio.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text  = "${desafio.tipo.name.lowercase().replaceFirstChar { it.uppercase() }} · ${desafio.habitosIds.size} hábitos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { expandida = !expandida }) {
                    Icon(
                        imageVector     = if (expandida) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expandida) "Contraer" else "Ver hábitos"
                    )
                }
            }

            AnimatedVisibility(visible = expandida) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    if (desafio.descripcion.isNotEmpty()) {
                        Text(
                            text  = desafio.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text("Hábitos incluidos:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    habitosNombres.forEach { nombre ->
                        Text("•  $nombre", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                    if (desafio.meta > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text  = "🎯 Meta: ${desafio.meta} días",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (yaSuscripto) {
                        OutlinedButton(
                            onClick  = { mostrarDialogoBaja = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Dar de baja el desafío") }
                    } else {
                        Button(
                            onClick  = onSuscribirse,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(textoBoton) }
                    }
                }
            }

            if (!expandida) {
                Spacer(modifier = Modifier.height(8.dp))
                if (yaSuscripto) {
                    OutlinedButton(
                        onClick  = { mostrarDialogoBaja = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Dar de baja el desafío") }
                } else {
                    Button(onClick = onSuscribirse, modifier = Modifier.fillMaxWidth()) {
                        Text(textoBoton)
                    }
                }
            }
        }
    }
}