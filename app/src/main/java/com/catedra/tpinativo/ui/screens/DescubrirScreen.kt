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
    val plantillas by viewModel.plantillasCatalogo.collectAsState()
    var categoriaSeleccionada by remember { mutableStateOf("Físico") }

    // Carga inicial y cada vez que cambia el botón
    LaunchedEffect(categoriaSeleccionada) {
        viewModel.cargarCatalogoPorCategoria(categoriaSeleccionada)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Catálogo de Hábitos") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // 🔘 BOTONES DE CATEGORÍAS
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Físico", "Estudio", "Productividad").forEach { cat ->
                    Button(
                        onClick = { categoriaSeleccionada = cat },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (categoriaSeleccionada == cat)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(cat, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 📜 LISTA DE PLANTILLAS
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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