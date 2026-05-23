package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DetalleHabitoScreen(
    habitoId: String,
    viewModel: HabitosViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val habito = uiState.habitos.find { it.id == habitoId }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    // Si cambiaste a Icons.Default.Close o tenés la flecha, usás el que te compile
                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detalle del Hábito",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
            if (habito != null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = habito.nombre, style = MaterialTheme.typography.headlineMedium)
                    Text(text = "Categoría: ${habito.categoria}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Fecha: ${habito.fecha}", style = MaterialTheme.typography.bodyMedium)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Estado: ", style = MaterialTheme.typography.bodyLarge)
                        AssistChip(
                            onClick = {},
                            label = { Text(if (habito.cumplido) "CUMPLIDO" else "PENDIENTE") }
                        )
                    }
                }
            } else {
                Text("No se encontró la información del hábito.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}