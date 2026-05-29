package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.data.Habito

// IMPORTS ESENCIALES PARA LA PREVIEW Y EL TEMA
import androidx.compose.ui.tooling.preview.Preview
import com.catedra.tpinativo.ui.theme.TPINativoTheme

// ==========================================
// 1. LA PREVIEW (Corregida con imports)
// ==========================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitosScreenPreview() {
    TPINativoTheme {
        // Usamos variables auxiliares con nombres explícitos para evitar errores de posición
        val habito1 = Habito(id = "1", nombre = "Ir al gimnasio (Preview)", categoria = "Físico", cumplido = false, userId = "user_123")
        val habito2 = Habito(id = "2", nombre = "Tomar agua (Preview)", categoria = "Salud", cumplido = true, userId = "user_123")
        val habito3 = Habito(id = "3", nombre = "Revisar tableros de Grafana", categoria = "Productividad", cumplido = false, userId = "user_123")

        HabitosContent(
            habitos = listOf(habito1, habito2, habito3),
            cargando = false,
            error = null,
            onVerDetalle = {},
            onAlternarEstado = { _, _ -> }
        )
    }
}

// ==========================================
// 2. CONTENEDOR REAL (Maneja la lógica y Firebase)
// ==========================================
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

    HabitosContent(
        habitos = uiState.habitos,
        cargando = uiState.cargando,
        error = uiState.error,
        onVerDetalle = onVerDetalle,
        onAlternarEstado = { id, cumplido -> viewModel.alternarEstadoHabito(id, cumplido) }
    )
}

// ==========================================
// 3. EL CONTENIDO VISUAL (Limpio y reutilizable)
// ==========================================
@Composable
fun HabitosContent(
    habitos: List<Habito>,
    cargando: Boolean,
    error: String?,
    onVerDetalle: (String) -> Unit,
    onAlternarEstado: (String, Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis Hábitos Diarios",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    ) { innerPadding ->
        when {
            cargando -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
            }
            habitos.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("No hay hábitos creados para hoy.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(habitos) { habito ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onVerDetalle(habito.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = habito.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(text = habito.categoria, style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = habito.cumplido,
                                    onCheckedChange = { onAlternarEstado(habito.id, habito.cumplido) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}