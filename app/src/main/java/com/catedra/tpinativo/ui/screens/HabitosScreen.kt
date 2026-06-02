package com.catedra.tpinativo.ui.screens

import com.catedra.tpinativo.ui.theme.TPINativoTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HabitosScreen(
    viewModel: HabitosViewModel,
    userId: String,
    onVerDetalle: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val desafios by viewModel.desafiosCatalogo.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.cargarHabitos(userId)
        viewModel.cargarDesafios()
    }

    uiState.ultimoDesafioLogrado?.let { nombreDesafio ->
        AlertDialog(
            onDismissRequest = { viewModel.resetearAlertaDesafio() },
            confirmButton = {
                Button(onClick = { viewModel.resetearAlertaDesafio() }) { Text("¡Buenísimo!") }
            },
            title = { Text("🏆 ¡Desafío Completado!") },
            text = { Text("Felicitaciones, desbloqueaste el logro:\n\n\"$nombreDesafio\"") }
        )
    }

    HabitosContent(
        habitos = uiState.habitos,
        cargando = uiState.cargando,
        error = uiState.error,
        onVerDetalle = onVerDetalle,
        onAlternarEstado = { habito -> viewModel.alternarEstadoHabito(habito) },
        desafios = desafios
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitosContent(
    habitos: List<HabitoSuscrito>,
    cargando: Boolean,
    error: String?,
    onVerDetalle: (String) -> Unit,
    onAlternarEstado: (HabitoSuscrito) -> Unit,
    desafios: List<com.catedra.tpinativo.data.model.DesafioObjetivo> = emptyList()
) {
    var filtroSeleccionado by remember { mutableStateOf("TODOS") }
    var textoBuscado by remember { mutableStateOf("") }
    val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val habitosFiltrados = habitos.filter { habito ->
        val cumpleTexto = habito.nombre.contains(textoBuscado, ignoreCase = true)
        val estaCumplidoHoy = habito.fechasCumplidas.contains(hoy)
        val cumpleEstado = when (filtroSeleccionado) {
            "PENDIENTES" -> !estaCumplidoHoy
            "CUMPLIDOS"  -> estaCumplidoHoy
            else         -> true
        }
        cumpleTexto && cumpleEstado
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Hábitos Diarios") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = textoBuscado,
                onValueChange = { textoBuscado = it },
                label = { Text("🔍 Buscar hábito...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtroSeleccionado == "TODOS",
                    onClick = { filtroSeleccionado = "TODOS" },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filtroSeleccionado == "PENDIENTES",
                    onClick = { filtroSeleccionado = "PENDIENTES" },
                    label = { Text("Pendientes") }
                )
                FilterChip(
                    selected = filtroSeleccionado == "CUMPLIDOS",
                    onClick = { filtroSeleccionado = "CUMPLIDOS" },
                    label = { Text("Cumplidos") }
                )
            }

            when {
                cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                habitosFiltrados.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron hábitos.")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(habitosFiltrados) { habito ->
                            val cumplidoHoy = habito.fechasCumplidas.contains(hoy)

                            // ✅ Usamos desafioId para evitar falsos positivos
                            val nombreDesafioAsociado = habito.desafioId?.let { desafioId ->
                                desafios.find { it.id == desafioId }?.nombreDesafio
                            }

                            HabitoCard(
                                habito = habito,
                                cumplidoHoy = cumplidoHoy,
                                nombreDesafioAsociado = nombreDesafioAsociado,
                                onVerDetalle = { onVerDetalle(habito.id) },
                                onAlternarEstado = { onAlternarEstado(habito) }
                            )
                        }
                    }
                }
            }
        }
    }
}

//  Card extraída como composable propio — para poder como en el lab 1 web reutilizable
@Composable
fun HabitoCard(
    habito: HabitoSuscrito,
    cumplidoHoy: Boolean,
    nombreDesafioAsociado: String?,
    onVerDetalle: () -> Unit,
    onAlternarEstado: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerDetalle() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habito.nombre,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = habito.categoria,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // insignia del desafío — solo aparece si el hábito pertenece a uno
                if (nombreDesafioAsociado != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "🏆 $nombreDesafioAsociado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Checkbox(
                checked = cumplidoHoy,
                onCheckedChange = { onAlternarEstado() }
            )
        }
    }
}