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

    LaunchedEffect(userId) {
        viewModel.cargarHabitos(userId)
    }

    // 🏆 DIÁLOGO DE FESTEJO REACTIVO (Opción 1)
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
        onAlternarEstado = { habito -> viewModel.alternarEstadoHabito(habito) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitosContent(
    habitos: List<HabitoSuscrito>,
    cargando: Boolean,
    error: String?,
    onVerDetalle: (String) -> Unit,
    onAlternarEstado: (HabitoSuscrito) -> Unit
) {
    var filtroSeleccionado by remember { mutableStateOf("TODOS") }
    var textoBuscado by remember { mutableStateOf("") }
    val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // 🔍 LÓGICA DE FILTRADO EN MEMORIA (Requerimiento de Cátedra)
    val habitosFiltrados = habitos.filter { habito ->
        val cumpleTexto = habito.nombre.contains(textoBuscado, ignoreCase = true)
        val estaCumplidoHoy = habito.fechasCumplidas.contains(hoy)
        val cumpleEstado = when (filtroSeleccionado) {
            "PENDIENTES" -> !estaCumplidoHoy
            "CUMPLIDOS" -> estaCumplidoHoy
            else -> true
        }
        cumpleTexto && cumpleEstado
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Hábitos Diarios") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Buscador
            OutlinedTextField(
                value = textoBuscado,
                onValueChange = { textoBuscado = it },
                label = { Text("🔍 Buscar hábito...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Fila de Filtros (Chips)
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

            // Listado
            when {
                cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: $error", color = MaterialTheme.colorScheme.error) }
                habitosFiltrados.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se encontraron hábitos.") }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(habitosFiltrados) { habito ->
                            val cumplidoHoy = habito.fechasCumplidas.contains(hoy)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVerDetalle(habito.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = habito.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(text = habito.categoria, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Checkbox(
                                        checked = cumplidoHoy,
                                        onCheckedChange = { onAlternarEstado(habito) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}