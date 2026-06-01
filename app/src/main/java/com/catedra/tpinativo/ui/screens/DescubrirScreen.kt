package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescubrirScreen(viewModel: HabitosViewModel, userId: String) {
    // 1. Escuchamos tanto las plantillas normales como los desafíos combinados
    val plantillas by viewModel.plantillasCatalogo.collectAsState()
    val desafios by viewModel.desafiosCatalogo.collectAsState() // 🏆 Escucha el nuevo flujo

    var categoriaSeleccionada by remember { mutableStateOf("Físico") }

    // 2. Cargador inteligente: Si toca el nuevo chip, va a buscar desafíos, sino hábitos
    LaunchedEffect(categoriaSeleccionada) {
        if (categoriaSeleccionada == "🏆 Desafíos") {
            viewModel.cargarDesafios()
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
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // 🔘 FILTROS DE CATEGORÍAS (Chips modernos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Lista de categorías incluyendo tu filtro maestro de Desafíos
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
                        modifier = Modifier.weight(1f) // Esto hace que se distribuyan parejitos en la pantalla
                    )
                }
            }

            // 📜 LISTA DINÁMICA MIXTA
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 🔄 CONDICIONAL MAESTRO: Si es el chip de desafíos, dibuja la interfaz de retos
                if (categoriaSeleccionada == "🏆 Desafíos") {
                    items(desafios) { desafio ->
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
                                    // Invoca la suscripción combinada que hicimos hace un rato
                                    Button(onClick = { viewModel.suscribirseADesafio(userId, desafio) }) {
                                        Text("Unirse")
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
                    // Si seleccionó Físico, Estudio o Salud, dibuja los hábitos simples de antes
                    items(plantillas) { plantilla ->
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
                                Button(onClick = { viewModel.suscribirseAHabito(userId, plantilla) }) {
                                    Text("Suscribir")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}