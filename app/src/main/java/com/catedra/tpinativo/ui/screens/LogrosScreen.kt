package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ✅ FIX 5: Función pura que calcula la racha actual desde fechasCumplidas
// No guarda nada en Firestore — solo lee y cuenta días consecutivos hacia atrás
fun calcularRachaActual(fechasCumplidas: List<String>): Int {
    if (fechasCumplidas.isEmpty()) return 0

    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val fechas = fechasCumplidas
        .mapNotNull { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }
        .sortedDescending()

    var racha = 0
    var diaEsperado = LocalDate.now()

    for (fecha in fechas) {
        if (fecha == diaEsperado || fecha == diaEsperado.minusDays(1)) {
            racha++
            diaEsperado = fecha.minusDays(1)
        } else {
            break
        }
    }

    return racha
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogrosScreen(viewModel: HabitosViewModel, userId: String) {
    val logros by viewModel.logrosUsuario.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Filtramos hábitos individuales con al menos 1 día cumplido para mostrar racha
    val habitosConRacha = uiState.habitos
        .filter { it.fechasCumplidas.isNotEmpty() }
        .map { habito -> habito to calcularRachaActual(habito.fechasCumplidas) }
        .filter { (_, racha) -> racha > 0 }
        .sortedByDescending { (_, racha) -> racha }

    LaunchedEffect(Unit) {
        viewModel.cargarLogros(userId)
        viewModel.cargarHabitos(userId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Logros y Medallas 🏆") }) }
    ) { innerPadding ->

        if (logros.isEmpty() && habitosConRacha.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¡Todavía no ganaste ninguna medalla!\nCompletá tus desafíos en el Home.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Sección Trofeos (desafíos completados) ─────────────
                if (logros.isNotEmpty()) {
                    item {
                        Text(
                            text = "Vitrina de Trofeos 🏅",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(logros) { logro ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Medalla",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = logro.nombreDesafio,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Ganado el: ${logro.fechaObtencion}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Sección Rachas (hábitos individuales) ───────────────
                if (habitosConRacha.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rachas activas 🔥",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(habitosConRacha) { (habito, racha) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔥",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Column {
                                    Text(
                                        text = habito.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "$racha día${if (racha != 1) "s" else ""} consecutivo${if (racha != 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
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