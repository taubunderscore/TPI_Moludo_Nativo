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
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.data.model.HabitoSuscrito

// IMPORTS PARA PREVIEW Y TEMA
import androidx.compose.ui.tooling.preview.Preview
import com.catedra.tpinativo.ui.theme.TPINativoTheme

// ==========================================
// 1. LA PREVIEW (Funciona 100% offline)
// ==========================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetalleHabitoScreenPreview() {
    TPINativoTheme {
        // Le pasamos un objeto Habito hardcodeado directamente al diseño
        DetalleHabitoContent(
            habito = HabitoSuscrito(
                id = "habito_prueba_123",
                nombre = "Rutina de piernas y saltos al cajón",
                categoria = "Físico",
                //cumplido = false,
                userId = "user_varela_123"
            ),
            onVolver = {}
        )
    }
}

// ==========================================
// 2. CONTENEDOR REAL (Habla con el ViewModel)
// ==========================================
@Composable
fun DetalleHabitoScreen(
    habitoId: String,
    viewModel: HabitosViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Buscamos el hábito real dentro del estado del ViewModel
    val habitoReal = uiState.habitos.find { it.id == habitoId }

    // Le delegamos todo el dibujo al componente visual pasándole el objeto encontrado
    DetalleHabitoContent(
        habito = habitoReal,
        onVolver = onVolver
    )
}

// ==========================================
// 3. EL CONTENIDO VISUAL (Puro, recibe datos limpios)
// ==========================================
@Composable
fun DetalleHabitoContent(
    habito: HabitoSuscrito?,
    onVolver: () -> Unit
) {
    // Calculamos el String de hoy para comprobar el estado
    val hoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.Close, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detalle del Hábito",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            if (habito != null) {
                // Chequeamos si la lista de fechas del hábito contiene el día de hoy
                val estaCumplidoHoy = habito.fechasCumplidas.contains(hoy)

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = habito.nombre, style = MaterialTheme.typography.headlineMedium)
                    Text(text = "Categoría: ${habito.categoria}", style = MaterialTheme.typography.bodyLarge)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Estado hoy: ", style = MaterialTheme.typography.bodyLarge)
                        AssistChip(
                            onClick = {},
                            // 🚀 USAMOS LA VARIABLE LOCAL CALCULADA:
                            label = { Text(if (estaCumplidoHoy) "CUMPLIDO" else "PENDIENTE") }
                        )
                    }
                }
            } else {
                Text(
                    text = "No se encontró la información del hábito.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}